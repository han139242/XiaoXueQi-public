package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neuedu.ems.entity.factory.FactoryLine;
import com.neuedu.ems.entity.mfg.MfgLineCalendarRule;
import com.neuedu.ems.entity.mfg.MfgLineProductCapability;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgScheduleItem;
import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import com.neuedu.ems.entity.org.OrgShift;
import com.neuedu.ems.entity.process.ProcessRoute;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.mapper.factory.FactoryLineMapper;
import com.neuedu.ems.mapper.mfg.MfgLineCalendarRuleMapper;
import com.neuedu.ems.mapper.mfg.MfgLineProductCapabilityMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleItemMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleScenarioMapper;
import com.neuedu.ems.mapper.org.OrgShiftMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.process.ProcessRouteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgScheduleScenarioServiceImplTest {

    private MfgScheduleScenarioServiceImpl service;
    private MfgScheduleScenarioMapper scenarioMapper;
    private MfgScheduleItemMapper itemMapper;
    private MfgProductionOrderMapper orderMapper;
    private MfgProductionPlanMapper planMapper;
    private MfgLineProductCapabilityMapper capabilityMapper;
    private MfgLineCalendarRuleMapper calendarRuleMapper;
    private FactoryLineMapper lineMapper;
    private ProcessRouteMapper routeMapper;
    private ProcessRouteItemMapper routeItemMapper;
    private OrgShiftMapper shiftMapper;

    @BeforeEach
    void setUp() {
        service = new MfgScheduleScenarioServiceImpl();
        scenarioMapper = mock(MfgScheduleScenarioMapper.class);
        itemMapper = mock(MfgScheduleItemMapper.class);
        orderMapper = mock(MfgProductionOrderMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        capabilityMapper = mock(MfgLineProductCapabilityMapper.class);
        calendarRuleMapper = mock(MfgLineCalendarRuleMapper.class);
        lineMapper = mock(FactoryLineMapper.class);
        routeMapper = mock(ProcessRouteMapper.class);
        routeItemMapper = mock(ProcessRouteItemMapper.class);
        shiftMapper = mock(OrgShiftMapper.class);

        ReflectionTestUtils.setField(service, "baseMapper", scenarioMapper);
        ReflectionTestUtils.setField(service, "mfgScheduleItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "mfgLineProductCapabilityMapper", capabilityMapper);
        ReflectionTestUtils.setField(service, "mfgLineCalendarRuleMapper", calendarRuleMapper);
        ReflectionTestUtils.setField(service, "factoryLineMapper", lineMapper);
        ReflectionTestUtils.setField(service, "processRouteMapper", routeMapper);
        ReflectionTestUtils.setField(service, "processRouteItemMapper", routeItemMapper);
        ReflectionTestUtils.setField(service, "orgShiftMapper", shiftMapper);

        when(scenarioMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            MfgScheduleScenario scenario = invocation.getArgument(0);
            scenario.setScenarioId(101L);
            scenario.setVersion(0);
            return 1;
        }).when(scenarioMapper).insert(any(MfgScheduleScenario.class));
        when(scenarioMapper.updateById(any(MfgScheduleScenario.class))).thenReturn(1);
        when(itemMapper.insert(any(MfgScheduleItem.class))).thenReturn(1);
    }

    @Test
    void closedPlanDoesNotBlockOrderFromBeingScheduledAgain() {
        when(planMapper.selectCount(any())).thenReturn(0L);

        Boolean hasActivePlan = ReflectionTestUtils.invokeMethod(service, "hasActivePlan", 1L);

        assertEquals(Boolean.FALSE, hasActivePlan);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<QueryWrapper> queryCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(planMapper).selectCount(queryCaptor.capture());
        assertTrue(queryCaptor.getValue().getSqlSegment().contains("status <>"));
        assertTrue(queryCaptor.getValue().getParamNameValuePairs().containsValue("CLOSED"));
    }

    @Test
    void calculatesEarliestWindowAndPersistsOnlyScenarioAndItem() {
        stubOneOrder(BigDecimal.valueOf(100));
        stubCapability(1L, 1L, 1, BigDecimal.ONE);
        stubLine(1L, BigDecimal.valueOf(800));
        stubRoute(1L, 1L);
        stubRouteMinutes(1L, new BigDecimal("0.40"), new BigDecimal("0.60"));
        stubDayShiftCalendar(1L);

        MfgScheduleScenario result = service.calculateScenario(request("SCH-001"));

        ArgumentCaptor<MfgScheduleItem> itemCaptor = ArgumentCaptor.forClass(MfgScheduleItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        MfgScheduleItem item = itemCaptor.getValue();
        assertEquals("CALCULATED", result.getStatus());
        assertEquals(1, result.getOrderCount());
        assertEquals(1, result.getScheduledOrderCount());
        assertEquals(0, result.getConflictOrderCount());
        assertEquals("SCHEDULED", item.getStatus());
        assertEquals(1L, item.getLineId());
        assertEquals(1L, item.getRouteId());
        assertEquals(LocalDateTime.of(2026, 7, 13, 8, 0), item.getPlannedStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 13, 9, 0), item.getPlannedEndTime());
        assertEquals(new BigDecimal("100.00"), item.getEstimatedMinutes());
        assertEquals(0L, item.getTardinessMinutes());
    }

    @Test
    void choosesEarliestCompletionBeforeCapabilityPriority() {
        stubOneOrder(BigDecimal.valueOf(100));
        stubCapability(1L, 1L, 100, BigDecimal.ONE);
        MfgLineProductCapability fasterCapability = capability(2L, 2L, 1, BigDecimal.ONE);
        when(capabilityMapper.selectList(any())).thenReturn(List.of(capability(1L, 1L, 100, BigDecimal.ONE), fasterCapability));
        when(lineMapper.selectList(any())).thenReturn(List.of(line(1L, BigDecimal.valueOf(800)), line(2L, BigDecimal.valueOf(400))));
        when(routeMapper.selectList(any())).thenReturn(List.of(route(1L, 1L), route(2L, 1L)));
        when(routeItemMapper.selectList(any())).thenReturn(List.of(routeItem(1L, new BigDecimal("0.50")), routeItem(2L, new BigDecimal("0.50"))));
        when(calendarRuleMapper.selectList(any())).thenReturn(List.of(calendarRule(1L), calendarRule(2L)));
        when(shiftMapper.selectList(any())).thenReturn(List.of(dayShift()));

        service.calculateScenario(request("SCH-002"));

        ArgumentCaptor<MfgScheduleItem> itemCaptor = ArgumentCaptor.forClass(MfgScheduleItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        assertEquals(1L, itemCaptor.getValue().getLineId());
    }

    @Test
    void marksOrderAsConflictWhenWindowCapacityIsInsufficient() {
        stubOneOrder(BigDecimal.valueOf(900));
        stubCapability(1L, 1L, 1, BigDecimal.ONE);
        stubLine(1L, BigDecimal.valueOf(800));
        stubRoute(1L, 1L);
        stubRouteMinutes(1L, new BigDecimal("0.50"));
        stubDayShiftCalendar(1L);

        MfgScheduleScenario result = service.calculateScenario(request("SCH-003"));

        ArgumentCaptor<MfgScheduleItem> itemCaptor = ArgumentCaptor.forClass(MfgScheduleItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        MfgScheduleItem item = itemCaptor.getValue();
        assertEquals("CONFLICT", item.getStatus());
        assertEquals("排程窗口有效产能不足", item.getConflictReason());
        assertNull(item.getLineId());
        assertNull(item.getPlannedStartTime());
        assertEquals(1, result.getConflictOrderCount());
        assertEquals(0, result.getScheduledOrderCount());
    }

    @Test
    void allocatesOnlyTheProportionalDailyCapacityForTheCoveredShiftWindow() {
        stubOneOrder(BigDecimal.valueOf(500));
        stubCapability(1L, 1L, 1, BigDecimal.ONE);
        stubLine(1L, BigDecimal.valueOf(800));
        stubRoute(1L, 1L);
        stubRouteMinutes(1L, new BigDecimal("0.50"));
        MfgLineCalendarRule dayRule = calendarRule(1L);
        MfgLineCalendarRule swingRule = calendarRule(1L);
        swingRule.setShiftId(2L);
        when(calendarRuleMapper.selectList(any())).thenReturn(List.of(dayRule, swingRule));
        OrgShift swingShift = new OrgShift();
        swingShift.setShiftId(2L);
        swingShift.setStartTime(LocalTime.of(16, 0));
        swingShift.setEndTime(LocalTime.MIDNIGHT);
        when(shiftMapper.selectList(any())).thenReturn(List.of(dayShift(), swingShift));

        service.calculateScenario(request("SCH-003A"));

        ArgumentCaptor<MfgScheduleItem> itemCaptor = ArgumentCaptor.forClass(MfgScheduleItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        assertEquals("CONFLICT", itemCaptor.getValue().getStatus());
        assertEquals("排程窗口有效产能不足", itemCaptor.getValue().getConflictReason());
    }

    @Test
    void supportsCrossDayShiftUsingItsConfiguredEndTime() {
        stubOneOrder(BigDecimal.valueOf(100));
        stubCapability(1L, 1L, 1, BigDecimal.ONE);
        stubLine(1L, BigDecimal.valueOf(800));
        stubRoute(1L, 1L);
        stubRouteMinutes(1L, new BigDecimal("0.50"));
        MfgLineCalendarRule swingRule = calendarRule(1L);
        swingRule.setShiftId(2L);
        when(calendarRuleMapper.selectList(any())).thenReturn(List.of(swingRule));
        OrgShift swingShift = new OrgShift();
        swingShift.setShiftId(2L);
        swingShift.setStartTime(LocalTime.of(16, 0));
        swingShift.setEndTime(LocalTime.MIDNIGHT);
        when(shiftMapper.selectList(any())).thenReturn(List.of(swingShift));

        MfgScheduleScenario crossDayRequest = request("SCH-003B");
        crossDayRequest.setHorizonStart(LocalDateTime.of(2026, 7, 13, 16, 0));
        crossDayRequest.setHorizonEnd(LocalDateTime.of(2026, 7, 14, 0, 0));
        service.calculateScenario(crossDayRequest);

        ArgumentCaptor<MfgScheduleItem> itemCaptor = ArgumentCaptor.forClass(MfgScheduleItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        assertEquals("SCHEDULED", itemCaptor.getValue().getStatus());
        assertEquals(LocalDateTime.of(2026, 7, 13, 16, 0), itemCaptor.getValue().getPlannedStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 13, 17, 0), itemCaptor.getValue().getPlannedEndTime());
    }

    @Test
    void marksOrderAsConflictWhenNoCapabilityExists() {
        stubOneOrder(BigDecimal.TEN);
        when(capabilityMapper.selectList(any())).thenReturn(List.of());
        when(lineMapper.selectList(any())).thenReturn(List.of());
        when(routeMapper.selectList(any())).thenReturn(List.of());
        when(routeItemMapper.selectList(any())).thenReturn(List.of());
        when(calendarRuleMapper.selectList(any())).thenReturn(List.of());
        when(shiftMapper.selectList(any())).thenReturn(List.of());

        MfgScheduleScenario result = service.calculateScenario(request("SCH-004"));

        ArgumentCaptor<MfgScheduleItem> itemCaptor = ArgumentCaptor.forClass(MfgScheduleItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        assertEquals("CONFLICT", itemCaptor.getValue().getStatus());
        assertEquals("未配置启用的产线产品能力", itemCaptor.getValue().getConflictReason());
        assertTrue(result.getTotalLoadMinutes().compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void rejectsIncompleteDraftOrderBeforePersistingScenario() {
        MfgProductionOrder incompleteOrder = new MfgProductionOrder();
        incompleteOrder.setProductionOrderId(11L);
        incompleteOrder.setProductionOrderNo("MO-INCOMPLETE");
        incompleteOrder.setOrderQuantity(BigDecimal.TEN);
        incompleteOrder.setDeliveryDate(LocalDate.of(2026, 7, 13));
        incompleteOrder.setStatus("DRAFT");
        when(orderMapper.selectList(any())).thenReturn(List.of(incompleteOrder));

        assertThrows(com.neuedu.ems.exception.BusinessException.class, () -> service.calculateScenario(request("SCH-005")));
        verify(scenarioMapper, never()).insert(any(MfgScheduleScenario.class));
    }

    private void stubOneOrder(BigDecimal quantity) {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(11L);
        order.setProductionOrderNo("MO-001");
        order.setProductId(1L);
        order.setOrderQuantity(quantity);
        order.setDeliveryDate(LocalDate.of(2026, 7, 13));
        order.setStatus("DRAFT");
        when(orderMapper.selectList(any())).thenReturn(List.of(order));
    }

    private void stubCapability(Long capabilityId, Long lineId, int priorityLevel, BigDecimal factor) {
        when(capabilityMapper.selectList(any())).thenReturn(List.of(capability(capabilityId, lineId, priorityLevel, factor)));
    }

    private void stubLine(Long lineId, BigDecimal dailyCapacity) {
        when(lineMapper.selectList(any())).thenReturn(List.of(line(lineId, dailyCapacity)));
    }

    private void stubRoute(Long routeId, Long productId) {
        when(routeMapper.selectList(any())).thenReturn(List.of(route(routeId, productId)));
    }

    private void stubRouteMinutes(Long routeId, BigDecimal... minutes) {
        List<ProcessRouteItem> items = java.util.Arrays.stream(minutes).map(value -> routeItem(routeId, value)).toList();
        when(routeItemMapper.selectList(any())).thenReturn(items);
    }

    private void stubDayShiftCalendar(Long lineId) {
        when(calendarRuleMapper.selectList(any())).thenReturn(List.of(calendarRule(lineId)));
        when(shiftMapper.selectList(any())).thenReturn(List.of(dayShift()));
    }

    private MfgLineProductCapability capability(Long capabilityId, Long lineId, int priorityLevel, BigDecimal factor) {
        MfgLineProductCapability capability = new MfgLineProductCapability();
        capability.setCapabilityId(capabilityId);
        capability.setLineId(lineId);
        capability.setProductId(1L);
        capability.setRouteId(lineId);
        capability.setPriorityLevel(priorityLevel);
        capability.setCapacityFactor(factor);
        capability.setStatus("ENABLE");
        return capability;
    }

    private FactoryLine line(Long lineId, BigDecimal dailyCapacity) {
        FactoryLine line = new FactoryLine();
        line.setLineId(lineId);
        line.setDailyCapacity(dailyCapacity);
        line.setStatus("RUNNING");
        return line;
    }

    private ProcessRoute route(Long routeId, Long productId) {
        ProcessRoute route = new ProcessRoute();
        route.setRouteId(routeId);
        route.setProductId(productId);
        route.setStatus("RELEASED");
        return route;
    }

    private ProcessRouteItem routeItem(Long routeId, BigDecimal standardTime) {
        ProcessRouteItem item = new ProcessRouteItem();
        item.setRouteId(routeId);
        item.setStandardTime(standardTime);
        return item;
    }

    private MfgLineCalendarRule calendarRule(Long lineId) {
        MfgLineCalendarRule rule = new MfgLineCalendarRule();
        rule.setLineId(lineId);
        rule.setDayOfWeek(1);
        rule.setShiftId(1L);
        rule.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        rule.setCapacityFactor(BigDecimal.ONE);
        rule.setStatus("ENABLE");
        return rule;
    }

    private OrgShift dayShift() {
        OrgShift shift = new OrgShift();
        shift.setShiftId(1L);
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(16, 0));
        return shift;
    }

    private MfgScheduleScenario request(String scenarioNo) {
        MfgScheduleScenario request = new MfgScheduleScenario();
        request.setScenarioNo(scenarioNo);
        request.setScenarioName("7 月排程测试");
        request.setHorizonStart(LocalDateTime.of(2026, 7, 13, 8, 0));
        request.setHorizonEnd(LocalDateTime.of(2026, 7, 13, 16, 0));
        return request;
    }
}
