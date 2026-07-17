package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.mapper.mfg.MfgScheduleScenarioMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgManualScheduleDraftServiceImplTest {

    private MfgScheduleScenarioServiceImpl service;
    private MfgScheduleScenarioMapper scenarioMapper;
    private SysOperationLogMapper operationLogMapper;

    @BeforeEach
    void setUp() {
        service = new MfgScheduleScenarioServiceImpl();
        scenarioMapper = mock(MfgScheduleScenarioMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", scenarioMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void reusesExistingManualDraftForCurrentOperator() {
        MfgScheduleScenario existing = new MfgScheduleScenario();
        existing.setScenarioId(21L);
        existing.setScenarioNo("MANUAL-7-EXISTING");
        existing.setAlgorithmCode("MANUAL_BOARD");
        existing.setStatus("DRAFT");
        existing.setCreatedBy(7L);
        when(scenarioMapper.selectList(any())).thenReturn(List.of(existing));

        MfgScheduleScenario result = service.openManualDraft(7L);

        assertSame(existing, result);
        verify(scenarioMapper, never()).insert(any(MfgScheduleScenario.class));
        verify(operationLogMapper, never()).insert(any(SysOperationLog.class));
    }

    @Test
    void reusesCalculatedManualDraftForCurrentOperator() {
        MfgScheduleScenario existing = new MfgScheduleScenario();
        existing.setScenarioId(23L);
        existing.setScenarioNo("MANUAL-7-CALCULATED");
        existing.setAlgorithmCode("MANUAL_BOARD");
        existing.setStatus("CALCULATED");
        existing.setCreatedBy(7L);
        existing.setOrderCount(1);
        when(scenarioMapper.selectList(any())).thenReturn(List.of(existing));

        MfgScheduleScenario result = service.openManualDraft(7L);

        assertSame(existing, result);
        verify(scenarioMapper, never()).insert(any(MfgScheduleScenario.class));
        verify(operationLogMapper, never()).insert(any(SysOperationLog.class));
    }

    @Test
    void ignoresAiBatchWhenOpeningManualDraft() {
        MfgScheduleScenario aiBatch = new MfgScheduleScenario();
        aiBatch.setScenarioId(24L);
        aiBatch.setScenarioNo("AI-BATCH-7-20260717190000000");
        aiBatch.setAlgorithmCode("MANUAL_BOARD");
        aiBatch.setStatus("DRAFT");
        aiBatch.setCreatedBy(7L);
        aiBatch.setOrderCount(6);
        MfgScheduleScenario manualDraft = new MfgScheduleScenario();
        manualDraft.setScenarioId(25L);
        manualDraft.setScenarioNo("MANUAL-7-20260717190100000");
        manualDraft.setAlgorithmCode("MANUAL_BOARD");
        manualDraft.setStatus("DRAFT");
        manualDraft.setCreatedBy(7L);
        when(scenarioMapper.selectList(any())).thenReturn(List.of(aiBatch, manualDraft));

        MfgScheduleScenario result = service.openManualDraft(7L);

        assertSame(manualDraft, result);
        verify(scenarioMapper, never()).insert(any(MfgScheduleScenario.class));
        verify(operationLogMapper, never()).insert(any(SysOperationLog.class));
    }

    @Test
    void createsManualBoardDraftAndWritesAuditLog() {
        when(scenarioMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            MfgScheduleScenario scenario = invocation.getArgument(0);
            scenario.setScenarioId(22L);
            return 1;
        }).when(scenarioMapper).insert(any(MfgScheduleScenario.class));

        MfgScheduleScenario result = service.openManualDraft(8L);

        ArgumentCaptor<MfgScheduleScenario> scenarioCaptor = ArgumentCaptor.forClass(MfgScheduleScenario.class);
        verify(scenarioMapper).insert(scenarioCaptor.capture());
        MfgScheduleScenario inserted = scenarioCaptor.getValue();
        assertEquals("MANUAL_BOARD", inserted.getAlgorithmCode());
        assertEquals("DRAFT", inserted.getStatus());
        assertEquals(8L, inserted.getCreatedBy());
        assertTrue(inserted.getScenarioNo().startsWith("MANUAL-8-"));
        assertEquals(22L, result.getScenarioId());
        verify(operationLogMapper).insert(any(SysOperationLog.class));
    }
}
