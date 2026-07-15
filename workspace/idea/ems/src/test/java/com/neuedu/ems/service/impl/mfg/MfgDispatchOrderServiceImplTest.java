package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.factory.FactoryStation;
import com.neuedu.ems.entity.factory.FactoryStationOperation;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.org.OrgTeam;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.factory.FactoryStationMapper;
import com.neuedu.ems.mapper.factory.FactoryStationOperationMapper;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.org.OrgTeamMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgDispatchOrderServiceImplTest {

    private MfgDispatchOrderServiceImpl service;
    private MfgDispatchOrderMapper dispatchMapper;
    private MfgTaskMapper taskMapper;
    private MfgWorkOrderMapper workOrderMapper;
    private MfgProductionPlanMapper planMapper;
    private ProcessRouteItemMapper routeItemMapper;
    private FactoryStationOperationMapper stationOperationMapper;
    private FactoryStationMapper stationMapper;
    private OrgTeamMapper teamMapper;
    private MfgWorkReportMapper reportMapper;
    private MfgResponsibilityNotificationService notificationService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "factoryStation"), FactoryStation.class);
        service = new MfgDispatchOrderServiceImpl();
        dispatchMapper = mock(MfgDispatchOrderMapper.class);
        taskMapper = mock(MfgTaskMapper.class);
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        routeItemMapper = mock(ProcessRouteItemMapper.class);
        stationOperationMapper = mock(FactoryStationOperationMapper.class);
        stationMapper = mock(FactoryStationMapper.class);
        teamMapper = mock(OrgTeamMapper.class);
        reportMapper = mock(MfgWorkReportMapper.class);
        notificationService = mock(MfgResponsibilityNotificationService.class);
        SysStatusChangeLogMapper statusLogMapper = mock(SysStatusChangeLogMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);

        ReflectionTestUtils.setField(service, "baseMapper", dispatchMapper);
        ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "workReportMapper", reportMapper);
        ReflectionTestUtils.setField(service, "workOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "productionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "routeItemMapper", routeItemMapper);
        ReflectionTestUtils.setField(service, "stationOperationMapper", stationOperationMapper);
        ReflectionTestUtils.setField(service, "stationMapper", stationMapper);
        ReflectionTestUtils.setField(service, "teamMapper", teamMapper);
        ReflectionTestUtils.setField(service, "statusLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "operationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "notificationService", notificationService);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void generateDispatchInheritsReleasedWorkOrderInsteadOfClientSourceFields() {
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        when(teamMapper.selectOne(any())).thenReturn(team());
        when(dispatchMapper.selectCount(any())).thenReturn(0L);
        when(dispatchMapper.insert(any(MfgDispatchOrder.class))).thenAnswer(invocation -> {
            invocation.<MfgDispatchOrder>getArgument(0).setDispatchId(20L);
            return 1;
        });
        MfgDispatchOrder request = new MfgDispatchOrder();
        request.setTeamId(7L);
        request.setDueTime(LocalDateTime.of(2026, 7, 16, 18, 0));
        request.setWorkOrderId(999L);
        request.setPlannedQuantity(BigDecimal.ONE);

        MfgDispatchOrder generated = service.generateDispatchOrder(3L, request, 9L);

        assertEquals(3L, generated.getWorkOrderId());
        assertEquals(new BigDecimal("100"), generated.getPlannedQuantity());
        assertEquals("WAIT_DISPATCH", generated.getStatus());
        assertTrue(generated.getDispatchNo().startsWith("DO-3-"));
    }

    @Test
    void detachedDispatchCreationIsRejected() {
        assertThrows(BusinessException.class, () -> service.saveDispatchOrder(new MfgDispatchOrder()));
        verify(dispatchMapper, never()).insert(any(MfgDispatchOrder.class));
    }

    @Test
    void releaseDispatchGeneratesRouteTasksWithPlanLineStations() {
        when(dispatchMapper.selectOne(any())).thenReturn(dispatch("WAIT_DISPATCH"));
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(4L);
        plan.setLineId(8L);
        when(planMapper.selectOne(any())).thenReturn(plan);
        when(routeItemMapper.selectList(any())).thenReturn(List.of(routeItem(11L, 1), routeItem(12L, 2)));
        when(taskMapper.selectCount(any())).thenReturn(0L);
        when(teamMapper.selectOne(any())).thenReturn(team());
        FactoryStationOperation relation = new FactoryStationOperation();
        relation.setStationId(30L);
        relation.setIsDefault(1);
        when(stationOperationMapper.selectList(any())).thenReturn(List.of(relation));
        FactoryStation station = new FactoryStation();
        station.setStationId(30L);
        station.setLineId(8L);
        when(stationMapper.selectOne(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<FactoryStation> query = invocation.getArgument(0);
            query.getSqlSegment();
            assertTrue(query.getParamNameValuePairs().containsValue(BusinessStatus.Factory.RUNNING));
            assertFalse(query.getParamNameValuePairs().containsValue(BusinessStatus.Common.ENABLE));
            return station;
        });
        when(taskMapper.insert(any(MfgTask.class))).thenAnswer(invocation -> {
            MfgTask task = invocation.getArgument(0);
            task.setTaskId(task.getOperationId() + 100L);
            return 1;
        });
        when(dispatchMapper.updateById(any(MfgDispatchOrder.class))).thenReturn(1);

        List<MfgTask> tasks = service.releaseDispatchOrder(20L, 9L, "安排一班生产");

        assertEquals(2, tasks.size());
        assertEquals(11L, tasks.get(0).getOperationId());
        assertEquals(30L, tasks.get(0).getStationId());
        assertEquals(3L, tasks.get(0).getWorkOrderId());
        assertEquals(new BigDecimal("100"), tasks.get(0).getPlannedQuantity());
        verify(notificationService).publishTaskUnassigned(tasks.get(0), 7L, 70L);
    }

    @Test
    void releaseDispatchRejectsRouteWithoutCurrentLineStation() {
        when(dispatchMapper.selectOne(any())).thenReturn(dispatch("WAIT_DISPATCH"));
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(4L);
        plan.setLineId(8L);
        when(planMapper.selectOne(any())).thenReturn(plan);
        when(routeItemMapper.selectList(any())).thenReturn(List.of(routeItem(11L, 1)));
        when(taskMapper.selectCount(any())).thenReturn(0L);
        when(teamMapper.selectOne(any())).thenReturn(team());
        when(stationOperationMapper.selectList(any())).thenReturn(List.of());

        assertThrows(BusinessException.class,
                () -> service.releaseDispatchOrder(20L, 9L, null));

        verify(taskMapper, never()).insert(any(MfgTask.class));
        verify(dispatchMapper, never()).updateById(any(MfgDispatchOrder.class));
    }

    @Test
    void withdrawReleasedDispatchClosesWaitingTasksAndReturnsToWaitDispatch() {
        MfgTask task = new MfgTask();
        task.setTaskId(31L);
        task.setStatus(BusinessStatus.Production.WAIT_START);
        task.setVersion(2);
        when(dispatchMapper.selectOne(any())).thenReturn(dispatch(BusinessStatus.Document.RELEASED));
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(reportMapper.selectCount(any())).thenReturn(0L);
        when(taskMapper.updateById(any(MfgTask.class))).thenReturn(1);
        when(dispatchMapper.updateById(any(MfgDispatchOrder.class))).thenReturn(1);

        service.withdrawDispatchOrder(20L, 9L, "调整班组");

        ArgumentCaptor<MfgTask> taskUpdate = ArgumentCaptor.forClass(MfgTask.class);
        verify(taskMapper).updateById(taskUpdate.capture());
        assertEquals(BusinessStatus.Document.CLOSED, taskUpdate.getValue().getStatus());
        ArgumentCaptor<MfgDispatchOrder> dispatchUpdate = ArgumentCaptor.forClass(MfgDispatchOrder.class);
        verify(dispatchMapper).updateById(dispatchUpdate.capture());
        assertEquals("WAIT_DISPATCH", dispatchUpdate.getValue().getStatus());
    }

    @Test
    void ordinaryUpdateCannotModifyReleasedDispatch() {
        when(dispatchMapper.selectOne(any())).thenReturn(dispatch(BusinessStatus.Document.RELEASED));
        MfgDispatchOrder request = new MfgDispatchOrder();
        request.setDispatchId(20L);
        request.setTeamId(99L);

        assertFalse(service.updateDispatchOrder(request));
        verify(dispatchMapper, never()).updateById(any(MfgDispatchOrder.class));
    }

    private MfgWorkOrder workOrder() {
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setPlanId(4L);
        workOrder.setRouteId(5L);
        workOrder.setPlannedQuantity(new BigDecimal("100"));
        workOrder.setStatus(BusinessStatus.Document.RELEASED);
        return workOrder;
    }

    private MfgDispatchOrder dispatch(String status) {
        MfgDispatchOrder dispatch = new MfgDispatchOrder();
        dispatch.setDispatchId(20L);
        dispatch.setWorkOrderId(3L);
        dispatch.setTeamId(7L);
        dispatch.setPlannedQuantity(new BigDecimal("100"));
        dispatch.setStatus(status);
        dispatch.setVersion(1);
        return dispatch;
    }

    private OrgTeam team() {
        OrgTeam team = new OrgTeam();
        team.setTeamId(7L);
        team.setLeaderId(70L);
        return team;
    }

    private ProcessRouteItem routeItem(Long operationId, int sequence) {
        ProcessRouteItem item = new ProcessRouteItem();
        item.setRouteItemId(operationId);
        item.setOperationId(operationId);
        item.setSequenceNo(sequence);
        return item;
    }
}
