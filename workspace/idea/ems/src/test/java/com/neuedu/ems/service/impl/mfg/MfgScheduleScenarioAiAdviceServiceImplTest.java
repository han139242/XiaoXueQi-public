package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.common.AiResponse;
import com.neuedu.ems.entity.mfg.MfgScheduleItem;
import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import com.neuedu.ems.exception.AiServiceException;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgScheduleItemMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleScenarioMapper;
import com.neuedu.ems.service.ai.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgScheduleScenarioAiAdviceServiceImplTest {

    private MfgScheduleScenarioServiceImpl service;
    private MfgScheduleScenarioMapper scenarioMapper;
    private MfgScheduleItemMapper itemMapper;
    private AiService aiService;

    @BeforeEach
    void setUp() {
        service = new MfgScheduleScenarioServiceImpl();
        scenarioMapper = mock(MfgScheduleScenarioMapper.class);
        itemMapper = mock(MfgScheduleItemMapper.class);
        aiService = mock(AiService.class);
        ReflectionTestUtils.setField(service, "baseMapper", scenarioMapper);
        ReflectionTestUtils.setField(service, "mfgScheduleItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "aiService", aiService);
        when(scenarioMapper.updateById(any(MfgScheduleScenario.class))).thenReturn(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatesAdviceFromDeidentifiedScenarioSummaryAndPersistsIt() {
        MfgScheduleScenario scenario = calculatedScenario();
        AiResponse aiResponse = aiResponse();
        when(scenarioMapper.selectOne(any())).thenReturn(scenario);
        when(itemMapper.selectList(any())).thenReturn(List.of(scheduledItem(), conflictItem()));
        when(aiService.getScheduleScenarioAdvice(any())).thenReturn(aiResponse);

        AiResponse result = service.generateAiAdvice(1L, 0);

        ArgumentCaptor<Map<String, Object>> summaryCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<MfgScheduleScenario> scenarioCaptor = ArgumentCaptor.forClass(MfgScheduleScenario.class);
        verify(aiService).getScheduleScenarioAdvice(summaryCaptor.capture());
        verify(scenarioMapper).updateById(scenarioCaptor.capture());

        Map<String, Object> summary = summaryCaptor.getValue();
        Map<String, Object> scenarioSummary = mapValue(summary.get("scenario"));
        List<Map<String, Object>> lineLoads = listValue(summary.get("lineLoads"));
        Map<String, Integer> conflictReasons = mapValue(summary.get("conflictReasons"));
        assertEquals(2, scenarioSummary.get("orderCount"));
        assertEquals(1, lineLoads.size());
        assertEquals(8L, lineLoads.get(0).get("lineId"));
        assertEquals(1, conflictReasons.get("排程窗口有效产能不足"));
        assertTrue(!summary.toString().contains("productionOrderId"));
        assertEquals(aiResponse, result);
        assertEquals(String.join(System.lineSeparator(), "产线 8 负荷较高", "- 复核延期订单"),
                scenarioCaptor.getValue().getAiAdvice());
        assertEquals("deepseek-chat", scenarioCaptor.getValue().getAiModel());
        assertNotNull(scenarioCaptor.getValue().getAiGeneratedTime());
    }

    @Test
    void rejectsNonCalculatedScenarioWithoutCallingAi() {
        MfgScheduleScenario scenario = calculatedScenario();
        scenario.setStatus("CONFIRMED");
        when(scenarioMapper.selectOne(any())).thenReturn(scenario);

        assertThrows(BusinessException.class, () -> service.generateAiAdvice(1L, 0));

        verify(aiService, never()).getScheduleScenarioAdvice(any());
        verify(scenarioMapper, never()).updateById(any(MfgScheduleScenario.class));
    }

    @Test
    void doesNotPersistAdviceWhenAiServiceFails() {
        when(scenarioMapper.selectOne(any())).thenReturn(calculatedScenario());
        when(itemMapper.selectList(any())).thenReturn(List.of(scheduledItem()));
        when(aiService.getScheduleScenarioAdvice(any())).thenThrow(new AiServiceException("AI服务尚未启用"));

        assertThrows(AiServiceException.class, () -> service.generateAiAdvice(1L, 0));

        verify(scenarioMapper, never()).updateById(any(MfgScheduleScenario.class));
    }

    private MfgScheduleScenario calculatedScenario() {
        MfgScheduleScenario scenario = new MfgScheduleScenario();
        scenario.setScenarioId(1L);
        scenario.setAlgorithmCode("RULE_V1");
        scenario.setStatus("CALCULATED");
        scenario.setOrderCount(2);
        scenario.setScheduledOrderCount(1);
        scenario.setConflictOrderCount(1);
        scenario.setLateOrderCount(1);
        scenario.setTotalLoadMinutes(new BigDecimal("120.00"));
        scenario.setVersion(0);
        scenario.setDeletedFlag(0);
        return scenario;
    }

    private MfgScheduleItem scheduledItem() {
        MfgScheduleItem item = new MfgScheduleItem();
        item.setScheduleItemId(11L);
        item.setProductionOrderId(101L);
        item.setLineId(8L);
        item.setPlannedQuantity(new BigDecimal("100.0000"));
        item.setEstimatedMinutes(new BigDecimal("120.00"));
        item.setPlannedStartTime(LocalDateTime.of(2026, 7, 13, 8, 0));
        item.setPlannedEndTime(LocalDateTime.of(2026, 7, 13, 10, 0));
        item.setTardinessMinutes(30L);
        item.setStatus("SCHEDULED");
        return item;
    }

    private MfgScheduleItem conflictItem() {
        MfgScheduleItem item = new MfgScheduleItem();
        item.setScheduleItemId(12L);
        item.setProductionOrderId(102L);
        item.setStatus("CONFLICT");
        item.setConflictReason("排程窗口有效产能不足");
        return item;
    }

    private AiResponse aiResponse() {
        AiResponse response = new AiResponse();
        response.setAnswer("产线 8 负荷较高");
        response.setSuggestions(List.of("复核延期订单"));
        response.setModel("deepseek-chat");
        response.setGeneratedAt(LocalDateTime.of(2026, 7, 13, 9, 0));
        return response;
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> mapValue(Object value) {
        return (Map<K, V>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listValue(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
