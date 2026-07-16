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
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgManualScheduleItemConfirmationServiceImplTest {

    private MfgScheduleScenarioServiceImpl service;
    private MfgScheduleScenarioMapper scenarioMapper;
    private MfgScheduleItemMapper itemMapper;
    private MfgProductionOrderMapper orderMapper;
    private MfgProductionPlanMapper planMapper;
    private SysStatusChangeLogMapper statusLogMapper;
    private SysOperationLogMapper operationLogMapper;

    @BeforeEach
    void setUp() {
        service = new MfgScheduleScenarioServiceImpl();
        scenarioMapper = mock(MfgScheduleScenarioMapper.class);
        itemMapper = mock(MfgScheduleItemMapper.class);
        orderMapper = mock(MfgProductionOrderMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        statusLogMapper = mock(SysStatusChangeLogMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", scenarioMapper);
        ReflectionTestUtils.setField(service, "mfgScheduleItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "mfgLineProductCapabilityMapper", capabilityMapper());
        ReflectionTestUtils.setField(service, "factoryLineMapper", lineMapper());
        ReflectionTestUtils.setField(service, "processRouteMapper", routeMapper());
        ReflectionTestUtils.setField(service, "processRouteItemMapper", routeItemMapper());
        ReflectionTestUtils.setField(service, "mfgLineCalendarRuleMapper", calendarMapper());
        ReflectionTestUtils.setField(service, "orgShiftMapper", shiftMapper());
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        when(scenarioMapper.updateById(any(MfgScheduleScenario.class))).thenReturn(1);
        when(itemMapper.updateById(any(MfgScheduleItem.class))).thenReturn(1);
        when(orderMapper.updateById(any(MfgProductionOrder.class))).thenReturn(1);
        when(planMapper.selectCount(any())).thenReturn(0L);
        when(planMapper.selectList(any())).thenReturn(List.of());
        when(planMapper.insert(any(MfgProductionPlan.class))).thenAnswer(invocation -> {
            invocation.<MfgProductionPlan>getArgument(0).setPlanId(501L);
            return 1;
        });
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void confirmsOneManualItemAndCreatesReleasedProductionPlan() {
        MfgScheduleScenario scenario = scenario();
        MfgScheduleItem item = item();
        MfgProductionOrder order = order();
        when(scenarioMapper.selectOne(any())).thenReturn(scenario);
        when(itemMapper.selectOne(any())).thenReturn(item);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(orderMapper.selectOne(any())).thenReturn(order);

        MfgProductionPlan result = service.confirmManualItem(1L, 11L, 0, 0, "计划员确认", 7L);

        ArgumentCaptor<MfgProductionPlan> planCaptor = ArgumentCaptor.forClass(MfgProductionPlan.class);
        ArgumentCaptor<MfgProductionOrder> orderCaptor = ArgumentCaptor.forClass(MfgProductionOrder.class);
        ArgumentCaptor<MfgScheduleItem> itemCaptor = ArgumentCaptor.forClass(MfgScheduleItem.class);
        verify(planMapper).insert(planCaptor.capture());
        verify(orderMapper).updateById(orderCaptor.capture());
        verify(itemMapper).updateById(itemCaptor.capture());
        verify(statusLogMapper, times(3)).insert(any(SysStatusChangeLog.class));
        verify(operationLogMapper).insert(any(SysOperationLog.class));
        assertEquals(501L, result.getPlanId());
        assertEquals("RELEASED", planCaptor.getValue().getStatus());
        assertEquals("计划员确认", planCaptor.getValue().getRemark());
        assertEquals("RELEASED", orderCaptor.getValue().getStatus());
        assertEquals("CONFIRMED", itemCaptor.getValue().getStatus());
        assertEquals(501L, itemCaptor.getValue().getPlanId());
    }

    private MfgScheduleScenario scenario() {
        MfgScheduleScenario scenario = new MfgScheduleScenario();
        scenario.setScenarioId(1L);
        scenario.setScenarioNo("MANUAL-1");
        scenario.setAlgorithmCode("MANUAL_BOARD");
        scenario.setStatus("DRAFT");
        scenario.setVersion(0);
        scenario.setHorizonStart(LocalDateTime.of(2026, 7, 13, 0, 0));
        scenario.setHorizonEnd(LocalDateTime.of(2026, 7, 20, 23, 59));
        return scenario;
    }

    private MfgScheduleItem item() {
        MfgScheduleItem item = new MfgScheduleItem();
        item.setScheduleItemId(11L);
        item.setScenarioId(1L);
        item.setProductionOrderId(101L);
        item.setProductId(1L);
        item.setLineId(21L);
        item.setRouteId(31L);
        item.setSequenceNo(1);
        item.setPlannedQuantity(BigDecimal.valueOf(100));
        item.setPlannedStartTime(LocalDateTime.of(2026, 7, 13, 8, 0));
        item.setPlannedEndTime(LocalDateTime.of(2026, 7, 13, 10, 0));
        item.setDecisionSource("MANUAL");
        item.setStatus("SCHEDULED");
        item.setVersion(0);
        return item;
    }

    private MfgProductionOrder order() {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(101L);
        order.setProductId(1L);
        order.setOrderQuantity(BigDecimal.valueOf(100));
        order.setDeliveryDate(LocalDate.of(2026, 7, 20));
        order.setStatus("DRAFT");
        order.setVersion(0);
        return order;
    }

    private MfgLineProductCapabilityMapper capabilityMapper() {
        MfgLineProductCapabilityMapper mapper = mock(MfgLineProductCapabilityMapper.class);
        MfgLineProductCapability capability = new MfgLineProductCapability();
        capability.setLineId(21L);
        capability.setProductId(1L);
        capability.setRouteId(31L);
        capability.setCapacityFactor(BigDecimal.ONE);
        capability.setStatus("ENABLE");
        when(mapper.selectList(any())).thenReturn(List.of(capability));
        return mapper;
    }

    private FactoryLineMapper lineMapper() {
        FactoryLineMapper mapper = mock(FactoryLineMapper.class);
        FactoryLine line = new FactoryLine();
        line.setLineId(21L);
        line.setDailyCapacity(BigDecimal.valueOf(800));
        line.setStatus("RUNNING");
        when(mapper.selectList(any())).thenReturn(List.of(line));
        return mapper;
    }

    private ProcessRouteMapper routeMapper() {
        ProcessRouteMapper mapper = mock(ProcessRouteMapper.class);
        ProcessRoute route = new ProcessRoute();
        route.setRouteId(31L);
        route.setProductId(1L);
        route.setStatus("RELEASED");
        when(mapper.selectList(any())).thenReturn(List.of(route));
        return mapper;
    }

    private ProcessRouteItemMapper routeItemMapper() {
        ProcessRouteItemMapper mapper = mock(ProcessRouteItemMapper.class);
        ProcessRouteItem routeItem = new ProcessRouteItem();
        routeItem.setRouteId(31L);
        routeItem.setStandardTime(new BigDecimal("0.50"));
        when(mapper.selectList(any())).thenReturn(List.of(routeItem));
        return mapper;
    }

    private MfgLineCalendarRuleMapper calendarMapper() {
        MfgLineCalendarRuleMapper mapper = mock(MfgLineCalendarRuleMapper.class);
        MfgLineCalendarRule rule = new MfgLineCalendarRule();
        rule.setLineId(21L);
        rule.setDayOfWeek(1);
        rule.setShiftId(41L);
        rule.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        rule.setCapacityFactor(BigDecimal.ONE);
        rule.setStatus("ENABLE");
        when(mapper.selectList(any())).thenReturn(List.of(rule));
        return mapper;
    }

    private OrgShiftMapper shiftMapper() {
        OrgShiftMapper mapper = mock(OrgShiftMapper.class);
        OrgShift shift = new OrgShift();
        shift.setShiftId(41L);
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(16, 0));
        when(mapper.selectList(any())).thenReturn(List.of(shift));
        return mapper;
    }
}
