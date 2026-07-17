package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.sys.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgTaskServiceImplTest {

    private MfgTaskServiceImpl service;
    private MfgTaskMapper taskMapper;
    private SysStatusChangeLogMapper statusLogMapper;
    private SysOperationLogMapper operationLogMapper;
    private MfgWorkReportMapper workReportMapper;
    private MfgWorkOrderMapper workOrderMapper;
    private ProcessRouteItemMapper routeItemMapper;

    @BeforeEach
    void setUp() {
        service = new MfgTaskServiceImpl();
        taskMapper = mock(MfgTaskMapper.class);
        statusLogMapper = mock(SysStatusChangeLogMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        workReportMapper = mock(MfgWorkReportMapper.class);
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        routeItemMapper = mock(ProcessRouteItemMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", taskMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "mfgWorkReportMapper", workReportMapper);
        ReflectionTestUtils.setField(service, "mfgWorkOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "processRouteItemMapper", routeItemMapper);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void startOnlyAcceptsWaitingTaskAndKeepsStationWhenNotProvided() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.WAIT_START));
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder(BusinessStatus.Production.RUNNING));
        when(taskMapper.update(any(MfgTask.class), any())).thenReturn(1);

        assertTrue(service.startTask(1L, null, 9L));

        ArgumentCaptor<MfgTask> update = ArgumentCaptor.forClass(MfgTask.class);
        verify(taskMapper).update(update.capture(), any());
        assertEquals(BusinessStatus.Production.RUNNING, update.getValue().getStatus());
        assertNull(update.getValue().getStationId());
    }

    @Test
    void repeatedStartIsRejectedBeforeUpdate() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.RUNNING));

        assertFalse(service.startTask(1L, 2L, 9L));
        verify(taskMapper, never()).update(any(MfgTask.class), any());
    }

    @Test
    void finishRequiresRunningTaskAndNonnegativeQuantities() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.RUNNING));
        when(taskMapper.update(any(MfgTask.class), any())).thenReturn(1);
        when(workReportMapper.selectCount(any())).thenReturn(1L);

        assertTrue(service.finishTask(1L, BigDecimal.TEN, BigDecimal.ONE, "完成", 9L));

        ArgumentCaptor<MfgTask> update = ArgumentCaptor.forClass(MfgTask.class);
        verify(taskMapper).update(update.capture(), any());
        assertEquals(BusinessStatus.Production.FINISHED, update.getValue().getStatus());
        assertEquals(BigDecimal.TEN, update.getValue().getCompletedQuantity());
        assertEquals(BigDecimal.ONE, update.getValue().getDefectQuantity());
    }

    @Test
    void finishRejectsTaskWithoutSubmittedOrApprovedReport() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.RUNNING));
        when(workReportMapper.selectCount(any())).thenReturn(0L);

        assertFalse(service.finishTask(1L, BigDecimal.TEN, BigDecimal.ZERO, "完成", 9L));

        verify(taskMapper, never()).update(any(MfgTask.class), any());
    }

    @Test
    void saveDerivesTaskNumberFromWorkOrderAndRouteSequence() {
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setWorkOrderNo("WO-20260714-003-1");
        workOrder.setRouteId(4L);
        ProcessRouteItem routeItem = new ProcessRouteItem();
        routeItem.setOperationId(5L);
        routeItem.setSequenceNo(50);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        when(routeItemMapper.selectOne(any())).thenReturn(routeItem);
        when(taskMapper.selectCount(any())).thenReturn(0L);
        when(taskMapper.insert(any(MfgTask.class))).thenReturn(1);
        MfgTask request = new MfgTask();
        request.setWorkOrderId(3L);
        request.setOperationId(5L);

        assertTrue(service.saveTask(request));
        assertEquals("TASK-20260714-003-1-050", request.getTaskNo());
    }

    @Test
    void saveRejectsWorkOrderWithoutNormalizedNumber() {
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setWorkOrderNo("WO-3-LEGACY");
        workOrder.setRouteId(4L);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        MfgTask request = new MfgTask();
        request.setWorkOrderId(3L);
        request.setOperationId(5L);

        assertFalse(service.saveTask(request));

        verify(routeItemMapper, never()).selectOne(any());
        verify(taskMapper, never()).insert(any(MfgTask.class));
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
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder(BusinessStatus.Production.RUNNING));
        when(taskMapper.update(any(MfgTask.class), any())).thenReturn(0);

        assertFalse(service.startTask(1L, 2L, 9L));
        verify(taskMapper).update(any(MfgTask.class), any());
    }

    @Test
    void startRejectsTaskBeforeItsWorkOrderStarts() {
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.WAIT_START));
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder(BusinessStatus.Document.RELEASED));

        assertFalse(service.startTask(1L, 2L, 9L));

        verify(taskMapper, never()).update(any(MfgTask.class), any());
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
    @SuppressWarnings("unchecked")
    void fullViewReturnsAllTasksAndMarksOnlyCurrentUsersAssignments() {
        DataScopeService dataScopeService = mock(DataScopeService.class);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);
        MfgTask assigned = task(BusinessStatus.Production.RUNNING);
        MfgTask unassigned = task(BusinessStatus.Production.RUNNING);
        unassigned.setTaskId(2L);
        Page<MfgTask> selectedPage = new Page<>(1, 10);
        selectedPage.setRecords(List.of(assigned, unassigned));
        selectedPage.setTotal(2);
        when(taskMapper.selectPage(any(Page.class), any())).thenReturn(selectedPage);
        when(workReportMapper.selectList(any())).thenReturn(List.of());
        when(dataScopeService.findOperableTaskIds(any())).thenReturn(Set.of(1L));

        Page<MfgTask> result = service.pageTasksForView(
                1, 10, null, null, null, null, null, null, null, "oldest");

        verify(dataScopeService, never()).applyTaskScope(any());
        assertEquals(2, result.getRecords().size());
        assertTrue(result.getRecords().get(0).getAssignedToCurrentUser());
        assertFalse(result.getRecords().get(1).getAssignedToCurrentUser());
    }

    @Test
    void fullViewDetailDoesNotUseRestrictedTaskLookup() {
        DataScopeService dataScopeService = mock(DataScopeService.class);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);
        when(taskMapper.selectOne(any())).thenReturn(task(BusinessStatus.Production.RUNNING));
        when(workReportMapper.selectList(any())).thenReturn(List.of());
        when(dataScopeService.findOperableTaskIds(any())).thenReturn(Set.of());

        MfgTask result = service.getTaskForView(1L);

        verify(dataScopeService, never()).applyTaskScope(any());
        assertFalse(result.getAssignedToCurrentUser());
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

    private MfgTask task(String status) {
        MfgTask task = new MfgTask();
        task.setTaskId(1L);
        task.setWorkOrderId(3L);
        task.setStatus(status);
        return task;
    }

    private MfgWorkOrder workOrder(String status) {
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setStatus(status);
        return workOrder;
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
