package com.neuedu.ems.service.impl.mfg;

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
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleItemMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleScenarioMapper;
import com.neuedu.ems.mapper.factory.FactoryLineMapper;
import com.neuedu.ems.mapper.mfg.MfgLineCalendarRuleMapper;
import com.neuedu.ems.mapper.mfg.MfgLineProductCapabilityMapper;
import com.neuedu.ems.mapper.org.OrgShiftMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.process.ProcessRouteMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
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

    private static final LocalDate TEST_DATE = LocalDate.now().plusDays(1)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    private MfgScheduleScenarioServiceImpl service;
    private MfgScheduleScenarioMapper scenarioMapper;
    private MfgScheduleItemMapper itemMapper;
    private MfgProductionOrderMapper orderMapper;
    private MfgProductionPlanMapper planMapper;
    private SysStatusChangeLogMapper statusChangeLogMapper;
    private SysOperationLogMapper operationLogMapper;
    private MfgLineProductCapabilityMapper capabilityMapper;
    private MfgLineCalendarRuleMapper calendarRuleMapper;
    private FactoryLineMapper lineMapper;
    private ProcessRouteMapper routeMapper;
    private ProcessRouteItemMapper routeItemMapper;
    private OrgShiftMapper shiftMapper;

    @BeforeEach
    void setUp() {
        service = new MfgScheduleScenarioServiceImpl();
        scenarioMapper = org.mockito.Mockito.mock(MfgScheduleScenarioMapper.class);
        itemMapper = org.mockito.Mockito.mock(MfgScheduleItemMapper.class);
        orderMapper = org.mockito.Mockito.mock(MfgProductionOrderMapper.class);
        planMapper = org.mockito.Mockito.mock(MfgProductionPlanMapper.class);
        statusChangeLogMapper = org.mockito.Mockito.mock(SysStatusChangeLogMapper.class);
        operationLogMapper = org.mockito.Mockito.mock(SysOperationLogMapper.class);
        capabilityMapper = org.mockito.Mockito.mock(MfgLineProductCapabilityMapper.class);
        calendarRuleMapper = org.mockito.Mockito.mock(MfgLineCalendarRuleMapper.class);
        lineMapper = org.mockito.Mockito.mock(FactoryLineMapper.class);
        routeMapper = org.mockito.Mockito.mock(ProcessRouteMapper.class);
        routeItemMapper = org.mockito.Mockito.mock(ProcessRouteItemMapper.class);
        shiftMapper = org.mockito.Mockito.mock(OrgShiftMapper.class);

        ReflectionTestUtils.setField(service, "baseMapper", scenarioMapper);
        ReflectionTestUtils.setField(service, "mfgScheduleItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusChangeLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "mfgLineProductCapabilityMapper", capabilityMapper);
        ReflectionTestUtils.setField(service, "mfgLineCalendarRuleMapper", calendarRuleMapper);
        ReflectionTestUtils.setField(service, "factoryLineMapper", lineMapper);
        ReflectionTestUtils.setField(service, "processRouteMapper", routeMapper);
        ReflectionTestUtils.setField(service, "processRouteItemMapper", routeItemMapper);
        ReflectionTestUtils.setField(service, "orgShiftMapper", shiftMapper);

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
        stubSchedulingContext();
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
        verify(operationLogMapper, times(2)).insert(any(SysOperationLog.class));

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
    void rejectsRepeatedConfirmationBeforeCreatingAnotherPlan() {
        MfgScheduleScenario confirmed = calculatedScenario();
        confirmed.setStatus("CONFIRMED");
        when(scenarioMapper.selectOne(any())).thenReturn(confirmed);

        assertThrows(BusinessException.class, () -> service.confirmScenario(1L, 0, 7L, null));

        verify(itemMapper, never()).selectList(any());
        verify(planMapper, never()).insert(any(MfgProductionPlan.class));
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
        verify(orderMapper, never()).updateById(any(MfgProductionOrder.class));
        verify(scenarioMapper, never()).updateById(any(MfgScheduleScenario.class));
        verify(statusChangeLogMapper, never()).insert(any(SysStatusChangeLog.class));
    }

    private MfgScheduleScenario calculatedScenario() {
        MfgScheduleScenario scenario = new MfgScheduleScenario();
        scenario.setScenarioId(1L);
        scenario.setScenarioNo("SCH-001");
        scenario.setHorizonStart(TEST_DATE.atTime(8, 0));
        scenario.setHorizonEnd(TEST_DATE.atTime(18, 0));
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
        item.setPlannedStartTime(TEST_DATE.atTime(8, 0));
        item.setPlannedEndTime(TEST_DATE.atTime(10, 0));
        item.setStatus("SCHEDULED");
        item.setVersion(0);
        item.setDeletedFlag(0);
        return item;
    }

    private MfgProductionOrder draftOrder() {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(101L);
        order.setProductionOrderNo("MO-001");
        order.setProductId(1L);
        order.setOrderQuantity(new BigDecimal("100"));
        order.setDeliveryDate(TEST_DATE.plusDays(1));
        order.setStatus("DRAFT");
        order.setVersion(0);
        order.setDeletedFlag(0);
        return order;
    }

    private void stubSchedulingContext() {
        MfgLineProductCapability capability = new MfgLineProductCapability();
        capability.setLineId(21L);
        capability.setProductId(1L);
        capability.setRouteId(31L);
        capability.setPriorityLevel(1);
        capability.setCapacityFactor(BigDecimal.ONE);
        capability.setStatus("ENABLE");
        when(capabilityMapper.selectList(any())).thenReturn(List.of(capability));

        FactoryLine line = new FactoryLine();
        line.setLineId(21L);
        line.setDailyCapacity(new BigDecimal("1000"));
        line.setStatus("RUNNING");
        when(lineMapper.selectList(any())).thenReturn(List.of(line));

        ProcessRoute route = new ProcessRoute();
        route.setRouteId(31L);
        route.setProductId(1L);
        route.setStatus("RELEASED");
        when(routeMapper.selectList(any())).thenReturn(List.of(route));

        ProcessRouteItem routeItem = new ProcessRouteItem();
        routeItem.setRouteId(31L);
        routeItem.setStandardTime(BigDecimal.ONE);
        when(routeItemMapper.selectList(any())).thenReturn(List.of(routeItem));

        MfgLineCalendarRule rule = new MfgLineCalendarRule();
        rule.setLineId(21L);
        rule.setDayOfWeek(TEST_DATE.getDayOfWeek().getValue());
        rule.setShiftId(41L);
        rule.setEffectiveFrom(TEST_DATE.minusDays(1));
        rule.setCapacityFactor(BigDecimal.ONE);
        rule.setStatus("ENABLE");
        when(calendarRuleMapper.selectList(any())).thenReturn(List.of(rule));

        OrgShift shift = new OrgShift();
        shift.setShiftId(41L);
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(18, 0));
        when(shiftMapper.selectList(any())).thenReturn(List.of(shift));
        when(planMapper.selectList(any())).thenReturn(List.of());
    }
}
