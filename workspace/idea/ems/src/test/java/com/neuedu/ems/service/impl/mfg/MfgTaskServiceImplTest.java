package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.eam.EamEquipment;
import com.neuedu.ems.entity.factory.FactoryStation;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkOrderMaterial;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.mfg.MfgTaskOperator;
import com.neuedu.ems.entity.org.OrgEmployee;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.mapper.eam.EamEquipmentMapper;
import com.neuedu.ems.mapper.factory.FactoryStationMapper;
import com.neuedu.ems.mapper.factory.FactoryStationOperationMapper;
import com.neuedu.ems.mapper.mfg.MfgAndonAlertMapper;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskOperatorMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.org.OrgEmployeeMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgTaskServiceImplTest {

    private MfgTaskServiceImpl service;
    private MfgTaskMapper taskMapper;
    private SysStatusChangeLogMapper statusLogMapper;
    private SysOperationLogMapper operationLogMapper;
    private MfgWorkReportMapper workReportMapper;
    private SysUserMapper userMapper;
    private OrgEmployeeMapper employeeMapper;
    private MfgTaskOperatorMapper taskOperatorMapper;
    private MfgWorkOrderMapper workOrderMapper;
    private MfgDispatchOrderMapper dispatchOrderMapper;
    private MfgWorkOrderMaterialMapper workOrderMaterialMapper;
    private ProcessRouteItemMapper routeItemMapper;
    private FactoryStationMapper stationMapper;
    private FactoryStationOperationMapper stationOperationMapper;
    private MfgProductionPlanMapper productionPlanMapper;
    private MfgProductionOrderMapper productionOrderMapper;
    private EamEquipmentMapper equipmentMapper;
    private MfgAndonAlertMapper andonAlertMapper;

    @BeforeEach
    void setUp() {
        service = new MfgTaskServiceImpl();
        taskMapper = mock(MfgTaskMapper.class);
        statusLogMapper = mock(SysStatusChangeLogMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        workReportMapper = mock(MfgWorkReportMapper.class);
        userMapper = mock(SysUserMapper.class);
        employeeMapper = mock(OrgEmployeeMapper.class);
        taskOperatorMapper = mock(MfgTaskOperatorMapper.class);
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        dispatchOrderMapper = mock(MfgDispatchOrderMapper.class);
        workOrderMaterialMapper = mock(MfgWorkOrderMaterialMapper.class);
        routeItemMapper = mock(ProcessRouteItemMapper.class);
        stationMapper = mock(FactoryStationMapper.class);
        stationOperationMapper = mock(FactoryStationOperationMapper.class);
        productionPlanMapper = mock(MfgProductionPlanMapper.class);
        productionOrderMapper = mock(MfgProductionOrderMapper.class);
        equipmentMapper = mock(EamEquipmentMapper.class);
        andonAlertMapper = mock(MfgAndonAlertMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", taskMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "mfgWorkReportMapper", workReportMapper);
        ReflectionTestUtils.setField(service, "sysUserMapper", userMapper);
        ReflectionTestUtils.setField(service, "orgEmployeeMapper", employeeMapper);
        ReflectionTestUtils.setField(service, "mfgTaskOperatorMapper", taskOperatorMapper);
        ReflectionTestUtils.setField(service, "mfgWorkOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "mfgDispatchOrderMapper", dispatchOrderMapper);
        ReflectionTestUtils.setField(service, "mfgWorkOrderMaterialMapper", workOrderMaterialMapper);
        ReflectionTestUtils.setField(service, "processRouteItemMapper", routeItemMapper);
        ReflectionTestUtils.setField(service, "factoryStationMapper", stationMapper);
        ReflectionTestUtils.setField(service, "factoryStationOperationMapper", stationOperationMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", productionPlanMapper);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", productionOrderMapper);
        ReflectionTestUtils.setField(service, "eamEquipmentMapper", equipmentMapper);
        ReflectionTestUtils.setField(service, "mfgAndonAlertMapper", andonAlertMapper);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
        when(workReportMapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    void startOnlyAcceptsWaitingTaskAndKeepsStationWhenNotProvided() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.WAIT_START));
        when(taskMapper.update(any(MfgTask.class), any())).thenReturn(1);
        prepareStartGate();

        assertTrue(service.startTask(1L, null, 9L));

        ArgumentCaptor<MfgTask> update = ArgumentCaptor.forClass(MfgTask.class);
        verify(taskMapper).update(update.capture(), any());
        assertEquals(BusinessStatus.Production.RUNNING, update.getValue().getStatus());
        assertNull(update.getValue().getStationId());
    }

    @Test
    void startTaskMovesReleasedParentsToRunning() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.WAIT_START));
        when(taskMapper.update(any(MfgTask.class), any())).thenReturn(1);
        prepareStartGate();
        MfgWorkOrder workOrder = workOrder(BusinessStatus.Document.RELEASED, 2);
        workOrder.setRouteId(6L);
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(7L);
        plan.setProductionOrderId(11L);
        plan.setLineId(8L);
        plan.setStatus(BusinessStatus.Document.RELEASED);
        plan.setVersion(3);
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(11L);
        order.setStatus(BusinessStatus.Document.RELEASED);
        order.setVersion(4);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        when(productionPlanMapper.selectOne(any())).thenReturn(plan);
        when(productionOrderMapper.selectOne(any())).thenReturn(order);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);
        when(productionPlanMapper.updateById(any(MfgProductionPlan.class))).thenReturn(1);
        when(productionOrderMapper.updateById(any(MfgProductionOrder.class))).thenReturn(1);

        assertTrue(service.startTask(1L, null, 9L));

        ArgumentCaptor<MfgWorkOrder> workOrderUpdate = ArgumentCaptor.forClass(MfgWorkOrder.class);
        ArgumentCaptor<MfgProductionPlan> planUpdate = ArgumentCaptor.forClass(MfgProductionPlan.class);
        ArgumentCaptor<MfgProductionOrder> orderUpdate = ArgumentCaptor.forClass(MfgProductionOrder.class);
        verify(workOrderMapper).updateById(workOrderUpdate.capture());
        verify(productionPlanMapper).updateById(planUpdate.capture());
        verify(productionOrderMapper).updateById(orderUpdate.capture());
        assertEquals(BusinessStatus.Production.RUNNING, workOrderUpdate.getValue().getStatus());
        assertEquals(BusinessStatus.Production.RUNNING, planUpdate.getValue().getStatus());
        assertEquals(BusinessStatus.Production.RUNNING, orderUpdate.getValue().getStatus());
    }

    @Test
    void repeatedStartIsRejectedBeforeUpdate() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.RUNNING));

        assertThrows(BusinessException.class, () -> service.startTask(1L, 2L, 9L));
        verify(taskMapper, never()).update(any(MfgTask.class), any());
    }

    @Test
    void detachedTaskCreationIsRejected() {
        assertThrows(BusinessException.class, () -> service.saveTask(new MfgTask()));
        verify(taskMapper, never()).insert(any(MfgTask.class));
    }

    @Test
    void withdrawStartReturnsUnreportedTaskToWaitStart() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.RUNNING));
        when(taskMapper.update(any(MfgTask.class), any())).thenReturn(1);

        service.withdrawTaskStart(1L, 9L, null);

        ArgumentCaptor<MfgTask> update = ArgumentCaptor.forClass(MfgTask.class);
        verify(taskMapper).update(update.capture(), any());
        assertEquals(BusinessStatus.Production.WAIT_START, update.getValue().getStatus());
    }

    @Test
    void finishAggregatesApprovedReportsInsteadOfClientQuantities() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.RUNNING));
        when(taskMapper.update(any(MfgTask.class), any())).thenReturn(1);
        when(workReportMapper.selectList(any())).thenReturn(List.of(
                report("99", "1", BusinessStatus.Document.APPROVED)));

        assertTrue(service.finishTask(1L, null, null, "完成", 9L));

        ArgumentCaptor<MfgTask> update = ArgumentCaptor.forClass(MfgTask.class);
        verify(taskMapper).update(update.capture(), any());
        assertEquals(BusinessStatus.Production.FINISHED, update.getValue().getStatus());
        assertEquals(new BigDecimal("99"), update.getValue().getCompletedQuantity());
        assertEquals(BigDecimal.ONE, update.getValue().getDefectQuantity());
    }

    @Test
    void finishRejectsPartialReportedQuantityWithoutControlledEarlyClose() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.RUNNING));
        when(workReportMapper.selectList(any())).thenReturn(List.of(
                report("10", "1", BusinessStatus.Document.APPROVED)));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.finishTask(1L, null, null, null, 9L));

        assertTrue(exception.getMessage().contains("尚有 89 未报工"));
        verify(taskMapper, never()).update(any(MfgTask.class), any());
    }

    @Test
    void finishRejectsTaskWithoutApprovedReport() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.RUNNING));
        when(workReportMapper.selectList(any())).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.finishTask(1L, null, null, null, 9L));

        assertTrue(exception.getMessage().contains("无已审核通过的报工"));
        verify(taskMapper, never()).update(any(MfgTask.class), any());
    }

    @Test
    void ordinaryUpdateCannotBypassRunningState() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.RUNNING));
        MfgTask request = new MfgTask();
        request.setTaskId(1L);
        request.setStatus(BusinessStatus.Production.FINISHED);

        assertFalse(service.updateTask(request));
        verify(taskMapper, never()).update(any(MfgTask.class), any());
    }

    @Test
    void startFailsWhenAnotherRequestChangesStateBeforeUpdate() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.WAIT_START));
        when(taskMapper.update(any(MfgTask.class), any())).thenReturn(0);
        prepareStartGate();

        assertThrows(BusinessException.class, () -> service.startTask(1L, 2L, 9L));
        verify(taskMapper).update(any(MfgTask.class), any());
    }

    @Test
    void startRejectsUserWhoIsNotAssignedToTask() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.WAIT_START));
        prepareStartGate();
        when(taskOperatorMapper.selectCount(any())).thenReturn(0L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.startTask(1L, null, 9L));

        assertTrue(exception.getMessage().contains("未分配给当前员工"));
        verify(taskMapper, never()).update(any(MfgTask.class), any());
    }

    @Test
    void startRejectsMaterialShortage() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.WAIT_START));
        prepareStartGate();
        MfgWorkOrderMaterial shortage = material("10", "6");
        when(workOrderMaterialMapper.selectList(any())).thenReturn(List.of(shortage));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.startTask(1L, null, 9L));

        assertTrue(exception.getMessage().contains("物料尚未实际领料齐套"));
    }

    @Test
    void startRejectsUnfinishedPredecessor() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.WAIT_START));
        prepareStartGate();
        ProcessRouteItem predecessor = routeItem(3L, 10);
        ProcessRouteItem current = routeItem(4L, 20);
        when(routeItemMapper.selectList(any())).thenReturn(List.of(predecessor, current));
        when(taskMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.startTask(1L, null, 9L));

        assertTrue(exception.getMessage().contains("前置工序尚未完成"));
    }

    @Test
    void startRejectsUnavailableStationEquipment() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.WAIT_START));
        prepareStartGate();
        EamEquipment equipment = new EamEquipment();
        equipment.setStatus(BusinessStatus.Equipment.FAULT);
        when(equipmentMapper.selectList(any())).thenReturn(List.of(equipment));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.startTask(1L, null, 9L));

        assertTrue(exception.getMessage().contains("设备均不可用"));
    }

    @Test
    void startRejectsOpenAndonAtStation() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.WAIT_START));
        prepareStartGate();
        when(andonAlertMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.startTask(1L, null, 9L));

        assertTrue(exception.getMessage().contains("未关闭安灯"));
    }

    @Test
    void employeeFilterUsesActiveTaskOperatorAssignments() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), MfgTask.class);
        LambdaQueryWrapper<MfgTask> query = ReflectionTestUtils.invokeMethod(
                service, "buildTaskQuery", null, null, null, null, null, null, 1001L, "oldest");

        String sql = query.getSqlSegment();
        assertTrue(sql.contains("mfg_task_operator"));
        assertTrue(sql.contains("employee_id = 1001"));
        assertTrue(sql.contains("deleted_flag = 0"));
        assertTrue(sql.contains("mfg_work_order"));
        assertTrue(sql.contains("'RUNNING', 'FINISHED', 'CLOSED'"));
        assertTrue(sql.contains("ORDER BY created_time ASC"));
    }

    @Test
    void workOrderDetailQueryKeepsPreProductionTasksVisible() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), MfgTask.class);
        LambdaQueryWrapper<MfgTask> query = ReflectionTestUtils.invokeMethod(
                service, "buildTaskQuery", null, null, null, 66L, null, null, null, "oldest");

        assertFalse(query.getSqlSegment().contains("mfg_work_order"));
    }

    @Test
    void dispatchDetailQueryKeepsPreProductionTasksVisible() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), MfgTask.class);
        LambdaQueryWrapper<MfgTask> query = ReflectionTestUtils.invokeMethod(
                service, "buildTaskQuery", null, null, 72L, null, null, null, null, "oldest");

        String sql = query.getSqlSegment();
        assertTrue(sql.contains("dispatch_id"));
        assertFalse(sql.contains("mfg_work_order"));
    }

    @Test
    void detailIncludesSubmittedAndApprovedReportTotals() {
        MfgTask task = task(BusinessStatus.Production.RUNNING);
        MfgWorkReport submitted = report("12", "1", "SUBMITTED");
        MfgWorkReport approved = report("8", "2", "APPROVED");
        when(taskMapper.selectOne(any())).thenReturn(task);
        when(workReportMapper.selectList(any())).thenReturn(List.of(submitted, approved));

        MfgTask result = service.getTaskById(1L);

        assertEquals(new BigDecimal("20"), result.getReportedQualifiedQuantity());
        assertEquals(new BigDecimal("3"), result.getReportedDefectQuantity());
    }

    @Test
    void controlledCloseCreatesSupplementTaskCopiesOperatorsAndReopensParents() {
        MfgTask source = task(BusinessStatus.Production.RUNNING);
        source.setTaskNo("TASK-001");
        source.setVersion(2);
        when(taskMapper.selectOne(any())).thenReturn(source);
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.update(any(MfgTask.class), any())).thenReturn(1);
        doAnswer(invocation -> {
            MfgTask inserted = invocation.getArgument(0);
            inserted.setTaskId(99L);
            return 1;
        }).when(taskMapper).insert(any(MfgTask.class));
        when(workReportMapper.selectList(any())).thenReturn(List.of(
                report("25", "5", BusinessStatus.Document.APPROVED)));

        MfgWorkOrder workOrder = workOrder(BusinessStatus.Production.FINISHED, 2);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(7L);
        plan.setProductionOrderId(8L);
        plan.setStatus(BusinessStatus.Production.FINISHED);
        plan.setVersion(3);
        when(productionPlanMapper.selectOne(any())).thenReturn(plan);
        when(productionPlanMapper.updateById(any(MfgProductionPlan.class))).thenReturn(1);
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(8L);
        order.setStatus(BusinessStatus.Production.FINISHED);
        order.setVersion(4);
        when(productionOrderMapper.selectOne(any())).thenReturn(order);
        when(productionOrderMapper.updateById(any(MfgProductionOrder.class))).thenReturn(1);

        MfgTaskOperator sourceOperator = new MfgTaskOperator();
        sourceOperator.setEmployeeId(10L);
        sourceOperator.setRoleInTask("PRIMARY");
        when(taskOperatorMapper.selectList(any())).thenReturn(List.of(sourceOperator));
        when(taskOperatorMapper.insert(any(MfgTaskOperator.class))).thenReturn(1);

        MfgTask compensation = service.controlledCloseTask(1L, 9L, "设备异常转线续作");

        assertEquals(99L, compensation.getTaskId());
        assertEquals("SUPPLEMENT", compensation.getTaskType());
        assertEquals(1L, compensation.getSourceTaskId());
        assertEquals(new BigDecimal("70"), compensation.getPlannedQuantity());
        assertEquals(BusinessStatus.Production.WAIT_START, compensation.getStatus());
        ArgumentCaptor<MfgTaskOperator> assignment = ArgumentCaptor.forClass(MfgTaskOperator.class);
        verify(taskOperatorMapper).insert(assignment.capture());
        assertEquals(99L, assignment.getValue().getTaskId());
        assertEquals(10L, assignment.getValue().getEmployeeId());

        ArgumentCaptor<MfgWorkOrder> reopenedWorkOrder = ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(reopenedWorkOrder.capture());
        assertEquals(BusinessStatus.Production.RUNNING, reopenedWorkOrder.getValue().getStatus());
        ArgumentCaptor<MfgProductionPlan> reopenedPlan = ArgumentCaptor.forClass(MfgProductionPlan.class);
        verify(productionPlanMapper).updateById(reopenedPlan.capture());
        assertEquals(BusinessStatus.Document.RELEASED, reopenedPlan.getValue().getStatus());
        ArgumentCaptor<MfgProductionOrder> reopenedOrder = ArgumentCaptor.forClass(MfgProductionOrder.class);
        verify(productionOrderMapper).updateById(reopenedOrder.capture());
        assertEquals(BusinessStatus.Document.RELEASED, reopenedOrder.getValue().getStatus());

        ArgumentCaptor<SysStatusChangeLog> statusLogs = ArgumentCaptor.forClass(SysStatusChangeLog.class);
        verify(statusLogMapper, org.mockito.Mockito.atLeast(5)).insert(statusLogs.capture());
        List<String> bizTypes = statusLogs.getAllValues().stream().map(SysStatusChangeLog::getBizType).toList();
        assertTrue(bizTypes.containsAll(List.of(
                "MFG_TASK", "MFG_WORK_ORDER", "MFG_PRODUCTION_PLAN", "MFG_PRODUCTION_ORDER")));
    }

    @Test
    void compensationAcceptsParentsAlreadyRunningWithoutRedundantStatusUpdates() {
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(7L);
        plan.setProductionOrderId(8L);
        plan.setStatus(BusinessStatus.Production.RUNNING);
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(8L);
        order.setStatus(BusinessStatus.Production.RUNNING);
        when(productionPlanMapper.selectOne(any())).thenReturn(plan);
        when(productionOrderMapper.selectOne(any())).thenReturn(order);

        ReflectionTestUtils.invokeMethod(service, "reopenPlanAndOrderForCompensation",
                7L, 9L, "运行中补偿", java.time.LocalDateTime.now());

        verify(productionPlanMapper, never()).updateById(any(MfgProductionPlan.class));
        verify(productionOrderMapper, never()).updateById(any(MfgProductionOrder.class));
    }

    @Test
    void controlledCloseRejectsPendingReportBeforeAnyMutation() {
        MfgTask source = task(BusinessStatus.Production.RUNNING);
        when(taskMapper.selectOne(any())).thenReturn(source);
        when(workReportMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.controlledCloseTask(1L, 9L, "班次结束"));

        assertTrue(exception.getMessage().contains("待审核报工"));
        verify(taskMapper, never()).update(any(MfgTask.class), any());
        verify(taskMapper, never()).insert(any(MfgTask.class));
    }

    @Test
    void repeatedControlledCloseReturnsExistingSupplementWithoutDuplicateInsert() {
        MfgTask closed = task(BusinessStatus.Document.CLOSED);
        MfgTask existing = task(BusinessStatus.Production.WAIT_START);
        existing.setTaskId(99L);
        existing.setTaskType("SUPPLEMENT");
        existing.setSourceTaskId(1L);
        when(taskMapper.selectOne(any())).thenReturn(closed);
        when(taskMapper.selectList(any())).thenReturn(List.of(existing));

        MfgTask result = service.controlledCloseTask(1L, 9L, "重复提交");

        assertEquals(99L, result.getTaskId());
        verify(taskMapper, never()).insert(any(MfgTask.class));
        verify(workOrderMapper, never()).updateById(any(MfgWorkOrder.class));
    }

    private MfgTask task(String status) {
        MfgTask task = new MfgTask();
        task.setTaskId(1L);
        task.setDispatchId(2L);
        task.setWorkOrderId(3L);
        task.setOperationId(4L);
        task.setStationId(5L);
        task.setPlannedQuantity(new BigDecimal("100"));
        task.setStatus(status);
        return task;
    }

    private MfgWorkOrder workOrder(String status, int version) {
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setPlanId(7L);
        workOrder.setStatus(status);
        workOrder.setVersion(version);
        return workOrder;
    }

    private void prepareStartGate() {
        SysUser user = new SysUser();
        user.setUserId(9L);
        user.setEmployeeId(10L);
        user.setStatus(BusinessStatus.Common.ENABLE);
        OrgEmployee employee = new OrgEmployee();
        employee.setEmployeeId(10L);
        employee.setStatus(BusinessStatus.Common.ENABLE);
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setPlanId(7L);
        workOrder.setRouteId(6L);
        workOrder.setStatus(BusinessStatus.Production.RUNNING);
        MfgDispatchOrder dispatch = new MfgDispatchOrder();
        dispatch.setDispatchId(2L);
        dispatch.setStatus(BusinessStatus.Document.RELEASED);
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(7L);
        plan.setProductionOrderId(11L);
        plan.setLineId(8L);
        plan.setStatus(BusinessStatus.Production.RUNNING);
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(11L);
        order.setStatus(BusinessStatus.Production.RUNNING);
        FactoryStation station = new FactoryStation();
        station.setStationId(5L);
        station.setLineId(8L);
        station.setStatus(BusinessStatus.Factory.RUNNING);

        when(userMapper.selectOne(any())).thenReturn(user);
        when(employeeMapper.selectOne(any())).thenReturn(employee);
        when(taskOperatorMapper.selectCount(any())).thenReturn(1L);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        when(dispatchOrderMapper.selectOne(any())).thenReturn(dispatch);
        when(workOrderMaterialMapper.selectList(any())).thenReturn(List.of(material("10", "10")));
        when(routeItemMapper.selectList(any())).thenReturn(List.of(routeItem(4L, 10)));
        when(productionPlanMapper.selectOne(any())).thenReturn(plan);
        when(productionOrderMapper.selectOne(any())).thenReturn(order);
        when(stationMapper.selectOne(any())).thenReturn(station);
        when(stationOperationMapper.selectCount(any())).thenReturn(1L);
        when(equipmentMapper.selectList(any())).thenReturn(List.of());
        when(andonAlertMapper.selectCount(any())).thenReturn(0L);
    }

    private MfgWorkOrderMaterial material(String required, String issued) {
        MfgWorkOrderMaterial material = new MfgWorkOrderMaterial();
        material.setWorkOrderId(3L);
        material.setRequiredQuantity(new BigDecimal(required));
        material.setIssuedQuantity(new BigDecimal(issued));
        return material;
    }

    private ProcessRouteItem routeItem(Long operationId, int sequence) {
        ProcessRouteItem item = new ProcessRouteItem();
        item.setRouteId(6L);
        item.setOperationId(operationId);
        item.setSequenceNo(sequence);
        return item;
    }

    private MfgWorkReport report(String qualified, String defect, String auditStatus) {
        MfgWorkReport report = new MfgWorkReport();
        report.setTaskId(1L);
        report.setQualifiedQuantity(new BigDecimal(qualified));
        report.setDefectQuantity(new BigDecimal(defect));
        report.setAuditStatus(auditStatus);
        return report;
    }
}
