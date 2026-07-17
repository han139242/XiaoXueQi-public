package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.factory.FactoryLine;
import com.neuedu.ems.entity.mfg.MfgLineCalendarRule;
import com.neuedu.ems.entity.mfg.MfgLineProductCapability;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgScheduleItem;
import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import com.neuedu.ems.entity.org.OrgShift;
import com.neuedu.ems.entity.process.ProcessRoute;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.entity.sys.SysOperationLog;
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
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.vo.mfg.ManualScheduleCandidateLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class MfgManualScheduleCandidateCacheTest {

    private MfgScheduleScenarioServiceImpl service;
    private MfgScheduleScenarioMapper scenarioMapper;
    private MfgScheduleItemMapper itemMapper;
    private MfgProductionPlanMapper planMapper;
    private MfgLineProductCapabilityMapper capabilityMapper;
    private FactoryLineMapper lineMapper;
    private ProcessRouteMapper routeMapper;
    private ProcessRouteItemMapper routeItemMapper;
    private MfgLineCalendarRuleMapper calendarRuleMapper;
    private OrgShiftMapper shiftMapper;

    @BeforeEach
    void setUp() {
        service = new MfgScheduleScenarioServiceImpl();
        scenarioMapper = mock(MfgScheduleScenarioMapper.class);
        itemMapper = mock(MfgScheduleItemMapper.class);
        MfgProductionOrderMapper orderMapper = mock(MfgProductionOrderMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);
        capabilityMapper = mock(MfgLineProductCapabilityMapper.class);
        lineMapper = mock(FactoryLineMapper.class);
        routeMapper = mock(ProcessRouteMapper.class);
        routeItemMapper = mock(ProcessRouteItemMapper.class);
        calendarRuleMapper = mock(MfgLineCalendarRuleMapper.class);
        shiftMapper = mock(OrgShiftMapper.class);

        ReflectionTestUtils.setField(service, "baseMapper", scenarioMapper);
        ReflectionTestUtils.setField(service, "mfgScheduleItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "mfgLineProductCapabilityMapper", capabilityMapper);
        ReflectionTestUtils.setField(service, "factoryLineMapper", lineMapper);
        ReflectionTestUtils.setField(service, "processRouteMapper", routeMapper);
        ReflectionTestUtils.setField(service, "processRouteItemMapper", routeItemMapper);
        ReflectionTestUtils.setField(service, "mfgLineCalendarRuleMapper", calendarRuleMapper);
        ReflectionTestUtils.setField(service, "orgShiftMapper", shiftMapper);

        MfgScheduleScenario scenario = new MfgScheduleScenario();
        scenario.setScenarioId(101L);
        scenario.setAlgorithmCode("MANUAL_BOARD");
        scenario.setStatus("DRAFT");
        scenario.setVersion(0);
        scenario.setHorizonStart(LocalDate.now().atStartOfDay());
        scenario.setHorizonEnd(LocalDate.now().plusDays(30).atTime(23, 59, 59));
        when(scenarioMapper.selectOne(any())).thenReturn(scenario);
        when(scenarioMapper.updateById(any(MfgScheduleScenario.class))).thenReturn(1);

        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(201L);
        order.setProductId(301L);
        order.setOrderQuantity(BigDecimal.TEN);
        order.setDeliveryDate(LocalDate.now().plusDays(7));
        order.setStatus(BusinessStatus.Document.DRAFT);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(planMapper.selectCount(any())).thenReturn(0L);
        when(planMapper.selectList(any())).thenReturn(List.of());
        when(itemMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            MfgScheduleItem item = invocation.getArgument(0);
            item.setScheduleItemId(401L);
            return 1;
        }).when(itemMapper).insert(any(MfgScheduleItem.class));
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);

        MfgLineProductCapability capability = new MfgLineProductCapability();
        capability.setCapabilityId(501L);
        capability.setProductId(301L);
        capability.setLineId(601L);
        capability.setRouteId(701L);
        capability.setPriorityLevel(1);
        capability.setCapacityFactor(BigDecimal.ONE);
        when(capabilityMapper.selectList(any())).thenReturn(List.of(capability));

        FactoryLine line = new FactoryLine();
        line.setLineId(601L);
        line.setLineCode("LINE-01");
        line.setLineName("测试产线");
        line.setWorkshopId(801L);
        line.setDailyCapacity(BigDecimal.valueOf(800));
        line.setStatus("RUNNING");
        when(lineMapper.selectList(any())).thenReturn(List.of(line));

        ProcessRoute route = new ProcessRoute();
        route.setRouteId(701L);
        route.setProductId(301L);
        when(routeMapper.selectList(any())).thenReturn(List.of(route));

        ProcessRouteItem routeItem = new ProcessRouteItem();
        routeItem.setRouteId(701L);
        routeItem.setStandardTime(BigDecimal.ONE);
        when(routeItemMapper.selectList(any())).thenReturn(List.of(routeItem));

        OrgShift shift = new OrgShift();
        shift.setShiftId(901L);
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(16, 0));
        when(shiftMapper.selectList(any())).thenReturn(List.of(shift));

        MfgLineCalendarRule rule = new MfgLineCalendarRule();
        rule.setLineId(601L);
        rule.setShiftId(901L);
        rule.setDayOfWeek(LocalDate.now().getDayOfWeek().getValue());
        rule.setEffectiveFrom(LocalDate.now().minusDays(1));
        rule.setEffectiveTo(LocalDate.now().plusDays(60));
        rule.setCapacityFactor(BigDecimal.ONE);
        when(calendarRuleMapper.selectList(any())).thenReturn(List.of(rule));
    }

    @Test
    void recalculatesCandidateWhenAddingOrder() {
        assertEquals(1, service.listEligibleLines(101L, 201L).size());
        assertEquals(1, service.listEligibleLines(101L, 201L).size());

        MfgScheduleItem result = service.addManualItem(101L, 0, 201L, 601L, 1L);

        assertEquals(601L, result.getLineId());
        verify(capabilityMapper, times(2)).selectList(any());
        verify(lineMapper, times(2)).selectList(any());
        verify(routeMapper, times(2)).selectList(any());
        verify(routeItemMapper, times(2)).selectList(any());
        verify(calendarRuleMapper, times(2)).selectList(any());
        verify(shiftMapper, times(2)).selectList(any());
    }

    @Test
    void placesCandidateAfterExistingProductionPlanOnSameLine() {
        LocalDate today = LocalDate.now();
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setLineId(601L);
        plan.setStatus("RELEASED");
        plan.setPlannedStartTime(today.atTime(8, 0));
        plan.setPlannedEndTime(today.atTime(10, 0));
        when(planMapper.selectList(any())).thenReturn(List.of(plan));

        ManualScheduleCandidateLine candidate = service.listEligibleLines(101L, 201L).getFirst();

        assertEquals(today.atTime(10, 0), candidate.getEarliestStartTime());
    }

    @Test
    void returnsWorkingMinutesWithoutCountingNonWorkingTime() {
        ManualScheduleCandidateLine candidate = service.listEligibleLines(101L, 201L).getFirst();

        assertEquals(6L, candidate.getMinimumWorkingMinutes());
    }

    @Test
    void validatesByAvailableLineCapacityInsteadOfRouteMinutes() {
        LocalDate futureDate = LocalDate.now().plusDays(7);

        ManualScheduleCandidateLine result = service.validatePlanningPlacement(201L, 601L,
                futureDate.atTime(8, 0), futureDate.atTime(8, 6));

        assertEquals(601L, result.getLineId());
    }

    @Test
    void allowsPlacementAcrossNonWorkingNightWhenBothBoundariesAreInActiveShifts() {
        LocalDate startDate = LocalDate.now().plusDays(7)
                .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
        List<MfgLineCalendarRule> allWeekRules = new ArrayList<>();
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            MfgLineCalendarRule rule = new MfgLineCalendarRule();
            rule.setLineId(601L);
            rule.setShiftId(901L);
            rule.setDayOfWeek(dayOfWeek);
            rule.setEffectiveFrom(startDate.minusDays(1));
            rule.setEffectiveTo(startDate.plusDays(7));
            rule.setCapacityFactor(BigDecimal.ONE);
            rule.setStatus("ENABLE");
            allWeekRules.add(rule);
        }
        when(calendarRuleMapper.selectList(any())).thenReturn(allWeekRules);

        ManualScheduleCandidateLine result = service.validatePlanningPlacement(201L, 601L,
                startDate.atTime(15, 55), startDate.plusDays(1).atTime(8, 5));

        assertEquals(601L, result.getLineId());
    }

    @Test
    void restoresRemovedItemWhenOrderIsAddedAgain() {
        MfgScheduleItem removed = new MfgScheduleItem();
        removed.setScheduleItemId(402L);
        removed.setScenarioId(101L);
        removed.setProductionOrderId(201L);
        removed.setDecisionSource("MANUAL");
        removed.setVersion(2);
        removed.setDeletedFlag(1);
        removed.setCreatedBy(1L);
        removed.setCreatedTime(LocalDateTime.now().minusHours(1));
        when(itemMapper.selectOne(any())).thenReturn(removed);
        when(itemMapper.update(any(), any())).thenReturn(1);
        assertEquals(1, service.listEligibleLines(101L, 201L).size());

        MfgScheduleItem result = service.addManualItem(101L, 0, 201L, 601L, 1L);

        assertEquals(402L, result.getScheduleItemId());
        assertEquals(3, result.getVersion());
        assertEquals(0, result.getDeletedFlag());
        verify(itemMapper, never()).insert(any(MfgScheduleItem.class));
        verify(itemMapper).update(any(), any());
    }

    @Test
    void extendsManualDraftHorizonWhenPlacementEndsAfterItsPreviousWindow() {
        LocalDate placementDate = LocalDate.now().plusDays(7);
        MfgScheduleScenario scenario = new MfgScheduleScenario();
        scenario.setScenarioId(101L);
        scenario.setAlgorithmCode("MANUAL_BOARD");
        scenario.setStatus("DRAFT");
        scenario.setVersion(0);
        scenario.setHorizonStart(LocalDate.now().atStartOfDay());
        scenario.setHorizonEnd(placementDate.minusDays(1).atTime(16, 0));
        when(scenarioMapper.selectOne(any())).thenReturn(scenario);

        MfgScheduleItem item = new MfgScheduleItem();
        item.setScheduleItemId(401L);
        item.setScenarioId(101L);
        item.setProductionOrderId(201L);
        item.setLineId(601L);
        item.setDecisionSource("MANUAL");
        item.setVersion(0);
        item.setPlannedStartTime(placementDate.minusDays(1).atTime(8, 0));
        item.setPlannedEndTime(placementDate.minusDays(1).atTime(9, 0));
        when(itemMapper.selectOne(any())).thenReturn(item);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(itemMapper.updateById(any(MfgScheduleItem.class))).thenAnswer(invocation -> {
            MfgScheduleItem update = invocation.getArgument(0);
            item.setLineId(update.getLineId());
            item.setPlannedStartTime(update.getPlannedStartTime());
            item.setPlannedEndTime(update.getPlannedEndTime());
            return 1;
        });

        MfgLineCalendarRule placementRule = new MfgLineCalendarRule();
        placementRule.setLineId(601L);
        placementRule.setShiftId(901L);
        placementRule.setDayOfWeek(placementDate.getDayOfWeek().getValue());
        placementRule.setEffectiveFrom(placementDate.minusDays(1));
        placementRule.setEffectiveTo(placementDate.plusDays(1));
        placementRule.setCapacityFactor(BigDecimal.ONE);
        placementRule.setStatus("ENABLE");
        when(calendarRuleMapper.selectList(any())).thenReturn(List.of(placementRule));

        LocalDateTime placementStart = placementDate.atTime(8, 0);
        LocalDateTime placementEnd = placementDate.atTime(9, 0);
        MfgScheduleItem result = service.updateManualPlacement(101L, 401L, 0, 0, 601L,
                placementStart, placementEnd, null, 1L);

        assertEquals(placementEnd, result.getPlannedEndTime());
        ArgumentCaptor<MfgScheduleScenario> scenarioCaptor = ArgumentCaptor.forClass(MfgScheduleScenario.class);
        verify(scenarioMapper).updateById(scenarioCaptor.capture());
        assertEquals(placementEnd, scenarioCaptor.getValue().getHorizonEnd());
        assertTrue(scenarioCaptor.getValue().getHorizonStart().isEqual(scenario.getHorizonStart()));
    }
}
