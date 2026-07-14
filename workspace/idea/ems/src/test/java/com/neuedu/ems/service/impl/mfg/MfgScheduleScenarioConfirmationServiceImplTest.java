package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgScheduleItem;
import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleItemMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleScenarioMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgScheduleScenarioConfirmationServiceImplTest {

    private MfgScheduleScenarioServiceImpl service;
    private MfgScheduleScenarioMapper scenarioMapper;
    private MfgScheduleItemMapper itemMapper;
    private MfgProductionOrderMapper orderMapper;
    private MfgProductionPlanMapper planMapper;
    private SysStatusChangeLogMapper statusChangeLogMapper;
    private SysOperationLogMapper operationLogMapper;

    @BeforeEach
    void setUp() {
        service = new MfgScheduleScenarioServiceImpl();
        scenarioMapper = org.mockito.Mockito.mock(MfgScheduleScenarioMapper.class);
        itemMapper = org.mockito.Mockito.mock(MfgScheduleItemMapper.class);
        orderMapper = org.mockito.Mockito.mock(MfgProductionOrderMapper.class);
        planMapper = org.mockito.Mockito.mock(MfgProductionPlanMapper.class);
        statusChangeLogMapper = org.mockito.Mockito.mock(SysStatusChangeLogMapper.class);
        operationLogMapper = org.mockito.Mockito.mock(SysOperationLogMapper.class);

        ReflectionTestUtils.setField(service, "baseMapper", scenarioMapper);
        ReflectionTestUtils.setField(service, "mfgScheduleItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusChangeLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);

        when(scenarioMapper.updateById(any(MfgScheduleScenario.class))).thenReturn(1);
        when(itemMapper.updateById(any(MfgScheduleItem.class))).thenReturn(1);
        when(orderMapper.updateById(any(MfgProductionOrder.class))).thenReturn(1);
        when(planMapper.selectCount(any())).thenReturn(0L);
        when(statusChangeLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
        when(planMapper.insert(any(MfgProductionPlan.class))).thenAnswer(invocation -> {
            invocation.<MfgProductionPlan>getArgument(0).setPlanId(501L);
            return 1;
        });
    }

    @Test
    void confirmsCalculatedScenarioAndCreatesReleasedPlan() {
        MfgScheduleScenario scenario = calculatedScenario();
        MfgScheduleItem item = scheduledItem();
        MfgProductionOrder order = draftOrder();
        when(scenarioMapper.selectOne(any())).thenReturn(scenario);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(orderMapper.selectOne(any())).thenReturn(order);

        MfgScheduleScenario result = service.confirmScenario(1L, 0, 7L, "人工确认");

        ArgumentCaptor<MfgProductionPlan> planCaptor = ArgumentCaptor.forClass(MfgProductionPlan.class);
        ArgumentCaptor<MfgProductionOrder> orderCaptor = ArgumentCaptor.forClass(MfgProductionOrder.class);
        ArgumentCaptor<MfgScheduleItem> itemCaptor = ArgumentCaptor.forClass(MfgScheduleItem.class);
        verify(planMapper).insert(planCaptor.capture());
        verify(orderMapper).updateById(orderCaptor.capture());
        verify(itemMapper).updateById(itemCaptor.capture());
        verify(statusChangeLogMapper, times(4)).insert(any(SysStatusChangeLog.class));
        verify(operationLogMapper).insert(any(SysOperationLog.class));

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(7L, result.getConfirmedBy());
        assertNotNull(result.getConfirmedTime());
        assertEquals("SP-1-1", planCaptor.getValue().getPlanNo());
        assertEquals("RELEASED", planCaptor.getValue().getStatus());
        assertEquals(101L, planCaptor.getValue().getProductionOrderId());
        assertEquals("RELEASED", orderCaptor.getValue().getStatus());
        assertEquals(501L, itemCaptor.getValue().getPlanId());
        assertEquals("CONFIRMED", itemCaptor.getValue().getStatus());
    }

    @Test
    void rejectsScenarioContainingConflictItemBeforeCreatingPlan() {
        MfgScheduleItem conflict = scheduledItem();
        conflict.setStatus("CONFLICT");
        conflict.setConflictReason("排程窗口有效产能不足");
        when(scenarioMapper.selectOne(any())).thenReturn(calculatedScenario());
        when(itemMapper.selectList(any())).thenReturn(List.of(conflict));

        assertThrows(BusinessException.class, () -> service.confirmScenario(1L, 0, 7L, null));

        verify(planMapper, never()).insert(any(MfgProductionPlan.class));
        verify(orderMapper, never()).updateById(any(MfgProductionOrder.class));
        verify(scenarioMapper, never()).updateById(any(MfgScheduleScenario.class));
    }

    @Test
    void rejectsProductionOrderThatAlreadyHasAnActivePlan() {
        when(scenarioMapper.selectOne(any())).thenReturn(calculatedScenario());
        when(itemMapper.selectList(any())).thenReturn(List.of(scheduledItem()));
        when(planMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.confirmScenario(1L, 0, 7L, null));

        verify(planMapper, never()).insert(any(MfgProductionPlan.class));
        verify(orderMapper, never()).updateById(any(MfgProductionOrder.class));
        verify(scenarioMapper, never()).updateById(any(MfgScheduleScenario.class));
    }

    @Test
    void propagatesItemUpdateFailureForTransactionalRollback() throws NoSuchMethodException {
        when(scenarioMapper.selectOne(any())).thenReturn(calculatedScenario());
        when(itemMapper.selectList(any())).thenReturn(List.of(scheduledItem()));
        when(orderMapper.selectOne(any())).thenReturn(draftOrder());
        when(itemMapper.updateById(any(MfgScheduleItem.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.confirmScenario(1L, 0, 7L, null));

        Method method = MfgScheduleScenarioServiceImpl.class.getMethod(
                "confirmScenario", Long.class, Integer.class, Long.class, String.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertTrue(Arrays.asList(transactional.rollbackFor()).contains(Exception.class));
        verify(planMapper).insert(any(MfgProductionPlan.class));
        verify(orderMapper).updateById(any(MfgProductionOrder.class));
        verify(scenarioMapper, never()).updateById(any(MfgScheduleScenario.class));
        verify(statusChangeLogMapper, never()).insert(any(SysStatusChangeLog.class));
    }

    private MfgScheduleScenario calculatedScenario() {
        MfgScheduleScenario scenario = new MfgScheduleScenario();
        scenario.setScenarioId(1L);
        scenario.setScenarioNo("SCH-001");
        scenario.setStatus("CALCULATED");
        scenario.setVersion(0);
        scenario.setDeletedFlag(0);
        return scenario;
    }

    private MfgScheduleItem scheduledItem() {
        MfgScheduleItem item = new MfgScheduleItem();
        item.setScheduleItemId(11L);
        item.setScenarioId(1L);
        item.setProductionOrderId(101L);
        item.setLineId(21L);
        item.setSequenceNo(1);
        item.setPlannedQuantity(new BigDecimal("100.0000"));
        item.setPlannedStartTime(LocalDateTime.of(2026, 7, 13, 8, 0));
        item.setPlannedEndTime(LocalDateTime.of(2026, 7, 13, 10, 0));
        item.setStatus("SCHEDULED");
        item.setVersion(0);
        item.setDeletedFlag(0);
        return item;
    }

    private MfgProductionOrder draftOrder() {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(101L);
        order.setProductionOrderNo("MO-001");
        order.setStatus("DRAFT");
        order.setVersion(0);
        order.setDeletedFlag(0);
        return order;
    }
}
