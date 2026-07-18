package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgScheduleItem;
import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleItemMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleScenarioMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgManualAiBatchServiceImplTest {

    private MfgScheduleScenarioServiceImpl service;
    private MfgScheduleScenarioMapper scenarioMapper;
    private MfgScheduleItemMapper itemMapper;
    private MfgProductionPlanMapper planMapper;
    private SysStatusChangeLogMapper statusLogMapper;
    private SysOperationLogMapper operationLogMapper;

    @BeforeEach
    void setUp() {
        service = new MfgScheduleScenarioServiceImpl();
        scenarioMapper = mock(MfgScheduleScenarioMapper.class);
        itemMapper = mock(MfgScheduleItemMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        statusLogMapper = mock(SysStatusChangeLogMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", scenarioMapper);
        ReflectionTestUtils.setField(service, "mfgScheduleItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void rejectsGeneratingWhenActiveAiBatchExists() {
        MfgScheduleScenario scenario = aiScenario();
        MfgScheduleItem item = scheduledItem();
        when(scenarioMapper.selectList(any())).thenReturn(List.of(scenario));
        when(itemMapper.selectList(any())).thenReturn(List.of(item));

        assertThrows(BusinessException.class, () -> service.generateManualAiBatch(7L));

        verify(scenarioMapper, never()).insert(any(MfgScheduleScenario.class));
    }

    @Test
    void withdrawsUnconfirmedAiBatchBySoftDeletingItemsAndCancellingScenario() {
        MfgScheduleScenario scenario = aiScenario();
        MfgScheduleItem item = scheduledItem();
        when(scenarioMapper.selectOne(any())).thenReturn(scenario);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(itemMapper.updateById(any(MfgScheduleItem.class))).thenReturn(1);
        when(scenarioMapper.updateById(any(MfgScheduleScenario.class))).thenReturn(1);

        service.withdrawManualAiBatch(31L, 0, 7L);

        ArgumentCaptor<MfgScheduleItem> itemCaptor = ArgumentCaptor.forClass(MfgScheduleItem.class);
        ArgumentCaptor<MfgScheduleScenario> scenarioCaptor = ArgumentCaptor.forClass(MfgScheduleScenario.class);
        verify(itemMapper).updateById(itemCaptor.capture());
        verify(scenarioMapper).updateById(scenarioCaptor.capture());
        assertEquals(1, itemCaptor.getValue().getDeletedFlag());
        assertEquals("CANCELLED", scenarioCaptor.getValue().getStatus());
    }

    @Test
    void refusesConfirmingAiBatchWithConflictItems() {
        MfgScheduleScenario scenario = aiScenario();
        MfgScheduleItem conflict = scheduledItem();
        conflict.setStatus("CONFLICT");
        conflict.setConflictReason("排程窗口有效产能不足");
        when(scenarioMapper.selectOne(any())).thenReturn(scenario);
        when(itemMapper.selectList(any())).thenReturn(List.of(conflict));

        assertThrows(BusinessException.class,
                () -> service.confirmManualAiBatch(31L, 0, "confirm batch", 7L));

        verify(planMapper, never()).insert(any(MfgProductionPlan.class));
    }

    @Test
    void refusesSingleItemConfirmationForAiBatchScenario() {
        when(scenarioMapper.selectOne(any())).thenReturn(aiScenario());

        assertThrows(BusinessException.class,
                () -> service.confirmManualItem(31L, 41L, 0, 0, "confirm single item", 7L));

        verify(planMapper, never()).insert(any(MfgProductionPlan.class));
        verify(itemMapper, never()).selectOne(any());
    }

    private MfgScheduleScenario aiScenario() {
        MfgScheduleScenario scenario = new MfgScheduleScenario();
        scenario.setScenarioId(31L);
        scenario.setScenarioNo("AI-BATCH-7-20260717190000000");
        scenario.setScenarioName("AI一键排产草案");
        scenario.setAlgorithmCode("MANUAL_BOARD");
        scenario.setStatus("DRAFT");
        scenario.setVersion(0);
        scenario.setCreatedBy(7L);
        return scenario;
    }

    private MfgScheduleItem scheduledItem() {
        MfgScheduleItem item = new MfgScheduleItem();
        item.setScheduleItemId(41L);
        item.setScenarioId(31L);
        item.setProductionOrderId(51L);
        item.setLineId(61L);
        item.setSequenceNo(1);
        item.setStatus("SCHEDULED");
        item.setDecisionSource("MANUAL");
        item.setPlannedStartTime(LocalDateTime.now().plusHours(2));
        item.setPlannedEndTime(LocalDateTime.now().plusHours(4));
        item.setVersion(0);
        item.setDeletedFlag(0);
        return item;
    }
}
