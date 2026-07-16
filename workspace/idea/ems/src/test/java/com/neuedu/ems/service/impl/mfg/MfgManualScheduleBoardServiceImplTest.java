package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.common.AiResponse;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgScheduleItem;
import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleItemMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleScenarioMapper;
import com.neuedu.ems.service.ai.AiService;
import com.neuedu.ems.vo.mfg.ManualScheduleBoard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgManualScheduleBoardServiceImplTest {

    private MfgScheduleScenarioServiceImpl service;
    private MfgScheduleScenarioMapper scenarioMapper;
    private MfgScheduleItemMapper itemMapper;
    private MfgProductionOrderMapper orderMapper;
    private MfgProductionPlanMapper planMapper;
    private AiService aiService;

    @BeforeEach
    void setUp() {
        service = new MfgScheduleScenarioServiceImpl();
        scenarioMapper = mock(MfgScheduleScenarioMapper.class);
        itemMapper = mock(MfgScheduleItemMapper.class);
        orderMapper = mock(MfgProductionOrderMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        aiService = mock(AiService.class);
        ReflectionTestUtils.setField(service, "baseMapper", scenarioMapper);
        ReflectionTestUtils.setField(service, "mfgScheduleItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "aiService", aiService);
    }

    @Test
    void mergesEditableManualItemsAndProductionPlansAndLeavesOnlyPendingOrders() {
        stubBoardData();

        ManualScheduleBoard board = service.getManualScheduleBoard(7L);

        assertEquals(2, board.getTimelineItems().size());
        assertEquals(1, board.getManualItemCount());
        assertEquals(1, board.getProductionPlanCount());
        assertEquals(1, board.getPendingOrders().size());
        assertEquals(303L, board.getPendingOrders().getFirst().getProductionOrderId());
        assertEquals(1L, board.getWorkspaceDraft().getScenarioId());
        assertEquals("MANUAL", board.getTimelineItems().getFirst().getSourceType());
        assertEquals("PLAN", board.getTimelineItems().get(1).getSourceType());
    }

    @Test
    void sendsGlobalBoardSummaryToAiWithoutWritingBusinessData() {
        stubBoardData();
        AiResponse response = new AiResponse();
        response.setAnswer("当前排产可行，建议优先处理临近交期订单");
        when(aiService.getScheduleScenarioAdvice(any())).thenReturn(response);

        AiResponse result = service.analyzeManualScheduleBoard(7L);

        assertSame(response, result);
        ArgumentCaptor<Map<String, Object>> summaryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aiService).getScheduleScenarioAdvice(summaryCaptor.capture());
        assertEquals("MANUAL_SCHEDULE_BOARD_FEASIBILITY", summaryCaptor.getValue().get("analysisType"));
        Map<?, ?> scope = (Map<?, ?>) summaryCaptor.getValue().get("scope");
        assertEquals(1, scope.get("manualItemCount"));
        assertEquals(1, scope.get("productionPlanCount"));
        assertEquals(1, scope.get("pendingOrderCount"));
    }

    @Test
    void showsFormalPlanInsteadOfStaleManualItemForTheSameOrder() {
        stubBoardData();
        MfgProductionPlan formalPlan = new MfgProductionPlan();
        formalPlan.setPlanId(52L);
        formalPlan.setPlanNo("PLAN-52");
        formalPlan.setProductionOrderId(101L);
        formalPlan.setLineId(21L);
        formalPlan.setPlannedQuantity(BigDecimal.valueOf(100));
        formalPlan.setPlannedStartTime(LocalDateTime.of(2026, 7, 13, 8, 0));
        formalPlan.setPlannedEndTime(LocalDateTime.of(2026, 7, 13, 10, 0));
        formalPlan.setStatus("RELEASED");
        when(planMapper.selectList(any())).thenReturn(List.of(formalPlan));

        ManualScheduleBoard board = service.getManualScheduleBoard(7L);

        assertEquals(0, board.getManualItemCount());
        assertEquals(1, board.getTimelineItems().size());
        assertEquals("PLAN", board.getTimelineItems().getFirst().getSourceType());
        assertEquals(52L, board.getTimelineItems().getFirst().getPlanId());
    }

    private void stubBoardData() {
        MfgScheduleScenario ownDraft = scenario(1L, 7L, "DRAFT", 1);
        MfgScheduleScenario anotherDraft = scenario(2L, 8L, "CALCULATED", 1);
        when(scenarioMapper.selectList(any())).thenReturn(List.of(ownDraft, anotherDraft));

        MfgScheduleItem manualItem = new MfgScheduleItem();
        manualItem.setScheduleItemId(11L);
        manualItem.setScenarioId(2L);
        manualItem.setProductionOrderId(101L);
        manualItem.setProductId(1L);
        manualItem.setLineId(21L);
        manualItem.setPlannedQuantity(BigDecimal.valueOf(100));
        manualItem.setPlannedStartTime(LocalDateTime.of(2026, 7, 13, 8, 0));
        manualItem.setPlannedEndTime(LocalDateTime.of(2026, 7, 13, 10, 0));
        manualItem.setStatus("SCHEDULED");
        manualItem.setVersion(0);
        when(itemMapper.selectList(any())).thenReturn(List.of(manualItem));

        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(51L);
        plan.setPlanNo("PLAN-51");
        plan.setProductionOrderId(202L);
        plan.setLineId(22L);
        plan.setPlannedQuantity(BigDecimal.valueOf(80));
        plan.setPlannedStartTime(LocalDateTime.of(2026, 7, 14, 8, 0));
        plan.setPlannedEndTime(LocalDateTime.of(2026, 7, 14, 9, 0));
        plan.setStatus("RELEASED");
        when(planMapper.selectList(any())).thenReturn(List.of(plan));

        MfgProductionOrder manualOrder = order(101L, "DRAFT", 1L);
        MfgProductionOrder plannedOrder = order(202L, "RELEASED", 2L);
        MfgProductionOrder pendingOrder = order(303L, "DRAFT", 3L);
        when(orderMapper.selectList(any()))
                .thenReturn(List.of(manualOrder, plannedOrder), List.of(manualOrder, pendingOrder));
    }

    private MfgScheduleScenario scenario(Long id, Long creator, String status, int orderCount) {
        MfgScheduleScenario scenario = new MfgScheduleScenario();
        scenario.setScenarioId(id);
        scenario.setScenarioNo("MANUAL-" + id);
        scenario.setAlgorithmCode("MANUAL_BOARD");
        scenario.setStatus(status);
        scenario.setOrderCount(orderCount);
        scenario.setVersion(0);
        scenario.setCreatedBy(creator);
        scenario.setHorizonStart(LocalDateTime.of(2026, 7, 13, 0, 0));
        scenario.setHorizonEnd(LocalDateTime.of(2026, 8, 12, 23, 59));
        return scenario;
    }

    private MfgProductionOrder order(Long id, String status, Long productId) {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(id);
        order.setProductionOrderNo("PO-" + id);
        order.setProductId(productId);
        order.setOrderQuantity(BigDecimal.valueOf(100));
        order.setDeliveryDate(LocalDate.of(2026, 7, 20));
        order.setStatus(status);
        return order;
    }
}
