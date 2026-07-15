package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgReportMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskOperatorMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.entity.sys.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgWorkReportServiceImplTest {

    private MfgWorkReportServiceImpl service;
    private MfgWorkReportMapper reportMapper;
    private MfgTaskMapper taskMapper;
    private MfgWorkOrderMapper workOrderMapper;
    private MfgProductionPlanMapper planMapper;
    private MfgProductionOrderMapper orderMapper;
    private MfgTaskOperatorMapper taskOperatorMapper;
    private SysUserMapper userMapper;

    @BeforeEach
    void setUp() {
        service = new MfgWorkReportServiceImpl();
        reportMapper = mock(MfgWorkReportMapper.class);
        taskMapper = mock(MfgTaskMapper.class);
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        orderMapper = mock(MfgProductionOrderMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", reportMapper);
        ReflectionTestUtils.setField(service, "mfgReportMaterialMapper", mock(MfgReportMaterialMapper.class));
        ReflectionTestUtils.setField(service, "mfgTaskMapper", taskMapper);
        taskOperatorMapper = mock(MfgTaskOperatorMapper.class);
        userMapper = mock(SysUserMapper.class);
        ReflectionTestUtils.setField(service, "mfgTaskOperatorMapper", taskOperatorMapper);
        ReflectionTestUtils.setField(service, "sysUserMapper", userMapper);
        ReflectionTestUtils.setField(service, "mfgWorkOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", orderMapper);
        SysStatusChangeLogMapper statusLogMapper = mock(SysStatusChangeLogMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
        SysUser user = new SysUser();
        user.setUserId(9L);
        user.setEmployeeId(3L);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(taskOperatorMapper.selectCount(any())).thenReturn(1L);
    }

    @Test
    void approvedReportRollsCompletionUpToWorkOrderPlanAndOrder() {
        MfgWorkReport report = report("100");
        when(reportMapper.selectOne(any())).thenReturn(report);
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        when(reportMapper.selectList(any())).thenReturn(List.of(), List.of(report));
        when(reportMapper.updateById(any(MfgWorkReport.class))).thenReturn(1);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);

        MfgWorkOrder finishedWorkOrder = workOrder();
        finishedWorkOrder.setStatus(BusinessStatus.Production.FINISHED);
        when(workOrderMapper.selectList(any())).thenReturn(List.of(finishedWorkOrder));
        when(planMapper.selectOne(any())).thenReturn(plan());
        when(planMapper.updateById(any(MfgProductionPlan.class))).thenReturn(1);
        MfgProductionPlan finishedPlan = plan();
        finishedPlan.setStatus(BusinessStatus.Production.FINISHED);
        when(planMapper.selectList(any())).thenReturn(List.of(finishedPlan));
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(orderMapper.updateById(any(MfgProductionOrder.class))).thenReturn(1);

        service.auditWorkReport(1L, " approved ", "审核通过", 9L);

        ArgumentCaptor<MfgWorkOrder> workOrderUpdate = ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(workOrderUpdate.capture());
        assertEquals(new BigDecimal("100"), workOrderUpdate.getValue().getCompletedQuantity());
        assertEquals(BusinessStatus.Production.FINISHED, workOrderUpdate.getValue().getStatus());

        ArgumentCaptor<MfgProductionPlan> planUpdate = ArgumentCaptor.forClass(MfgProductionPlan.class);
        verify(planMapper).updateById(planUpdate.capture());
        assertEquals(BusinessStatus.Production.FINISHED, planUpdate.getValue().getStatus());

        ArgumentCaptor<MfgProductionOrder> orderUpdate = ArgumentCaptor.forClass(MfgProductionOrder.class);
        verify(orderMapper).updateById(orderUpdate.capture());
        assertEquals(BusinessStatus.Production.FINISHED, orderUpdate.getValue().getStatus());
    }

    @Test
    void approvalRejectsQuantityAboveWorkOrderPlanBeforeMutation() {
        MfgWorkReport candidate = report("20");
        when(reportMapper.selectOne(any())).thenReturn(candidate);
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        when(reportMapper.selectList(any())).thenReturn(List.of(approvedReport("90")));

        assertThrows(BusinessException.class,
                () -> service.auditWorkReport(1L, BusinessStatus.Document.APPROVED, null, 9L));

        verify(reportMapper, never()).updateById(any(MfgWorkReport.class));
        verify(workOrderMapper, never()).updateById(any(MfgWorkOrder.class));
    }

    @Test
    void concurrentApprovalOverPlanRaisesExceptionForTransactionRollback() {
        MfgWorkReport candidate = report("20");
        when(reportMapper.selectOne(any())).thenReturn(candidate);
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        when(reportMapper.selectList(any())).thenReturn(
                List.of(approvedReport("80")),
                List.of(approvedReport("90"), candidate));
        when(reportMapper.updateById(any(MfgWorkReport.class))).thenReturn(1);

        assertThrows(BusinessException.class,
                () -> service.auditWorkReport(1L, BusinessStatus.Document.APPROVED, null, 9L));

        verify(reportMapper).updateById(any(MfgWorkReport.class));
        verify(workOrderMapper, never()).updateById(any(MfgWorkOrder.class));
    }

    @Test
    void finishedPartialPlanDoesNotPrematurelyFinishProductionOrder() {
        MfgProductionPlan partialPlan = plan();
        partialPlan.setPlannedQuantity(new BigDecimal("60"));
        partialPlan.setStatus(BusinessStatus.Production.FINISHED);
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(planMapper.selectList(any())).thenReturn(List.of(partialPlan));

        ReflectionTestUtils.invokeMethod(service, "finishProductionOrderIfComplete", 1L, 9L,
                java.time.LocalDateTime.of(2026, 7, 14, 8, 0));

        verify(orderMapper, never()).updateById(any(MfgProductionOrder.class));
    }

    @Test
    void submitRejectsNegativeQuantityBeforeDatabaseMutation() {
        MfgWorkReport request = new MfgWorkReport();
        request.setTaskId(2L);
        request.setOperatorId(3L);
        request.setStationId(4L);
        request.setQualifiedQuantity(new BigDecimal("-1"));
        request.setDefectQuantity(BigDecimal.ZERO);
        request.setActualWorkHours(BigDecimal.ONE);

        MfgTask runningTask = task();
        runningTask.setStatus(BusinessStatus.Production.RUNNING);
        runningTask.setStationId(4L);
        when(taskMapper.selectOne(any())).thenReturn(runningTask);

        assertThrows(BusinessException.class, () -> service.submitWorkReport(request, List.of(), 9L));

        verify(reportMapper, never()).insert(any(MfgWorkReport.class));
    }

    @Test
    void submitUsesJwtEmployeeTaskStationAndBackendReportNumber() {
        MfgTask runningTask = task();
        runningTask.setStatus(BusinessStatus.Production.RUNNING);
        runningTask.setStationId(4L);
        when(taskMapper.selectOne(any())).thenReturn(runningTask);
        when(reportMapper.insert(any(MfgWorkReport.class))).thenAnswer(invocation -> {
            invocation.<MfgWorkReport>getArgument(0).setReportId(10L);
            return 1;
        });
        MfgWorkReport request = new MfgWorkReport();
        request.setReportId(999L);
        request.setReportNo("FORGED-REPORT-NO");
        request.setTaskId(2L);
        request.setOperatorId(999L);
        request.setStationId(999L);
        request.setQualifiedQuantity(BigDecimal.ONE);
        request.setDefectQuantity(BigDecimal.ZERO);
        request.setActualWorkHours(BigDecimal.ONE);

        service.submitWorkReport(request, List.of(), 9L);

        ArgumentCaptor<MfgWorkReport> inserted = ArgumentCaptor.forClass(MfgWorkReport.class);
        verify(reportMapper).insert(inserted.capture());
        assertEquals(3L, inserted.getValue().getOperatorId());
        assertEquals(4L, inserted.getValue().getStationId());
        assertNotEquals("FORGED-REPORT-NO", inserted.getValue().getReportNo());
        assertEquals(10L, inserted.getValue().getReportId());
        assertEquals(9L, inserted.getValue().getCreatedBy());
    }

    @Test
    void submitRejectsJwtEmployeeNotAssignedToTask() {
        MfgTask runningTask = task();
        runningTask.setStatus(BusinessStatus.Production.RUNNING);
        runningTask.setStationId(4L);
        when(taskMapper.selectOne(any())).thenReturn(runningTask);
        when(taskOperatorMapper.selectCount(any())).thenReturn(0L);
        MfgWorkReport request = new MfgWorkReport();
        request.setTaskId(2L);
        request.setQualifiedQuantity(BigDecimal.ONE);
        request.setDefectQuantity(BigDecimal.ZERO);
        request.setActualWorkHours(BigDecimal.ONE);

        assertThrows(BusinessException.class,
                () -> service.submitWorkReport(request, List.of(), 9L));

        verify(reportMapper, never()).insert(any(MfgWorkReport.class));
    }

    @Test
    void withdrawRejectsAnotherEmployeesSubmittedReport() {
        MfgWorkReport submitted = report("1");
        submitted.setOperatorId(99L);
        when(reportMapper.selectOne(any())).thenReturn(submitted);

        assertThrows(BusinessException.class,
                () -> service.withdrawWorkReport(1L, 9L, "不是本人报工"));

        verify(reportMapper, never()).updateById(any(MfgWorkReport.class));
    }

    private MfgWorkReport report(String quantity) {
        MfgWorkReport report = new MfgWorkReport();
        report.setReportId(1L);
        report.setTaskId(2L);
        report.setQualifiedQuantity(new BigDecimal(quantity));
        report.setAuditStatus(BusinessStatus.Document.SUBMITTED);
        report.setVersion(1);
        return report;
    }

    private MfgWorkReport approvedReport(String quantity) {
        MfgWorkReport report = report(quantity);
        report.setAuditStatus(BusinessStatus.Document.APPROVED);
        return report;
    }

    private MfgTask task() {
        MfgTask task = new MfgTask();
        task.setTaskId(2L);
        task.setWorkOrderId(3L);
        return task;
    }

    private MfgWorkOrder workOrder() {
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setPlanId(4L);
        workOrder.setPlannedQuantity(new BigDecimal("100"));
        workOrder.setStatus(BusinessStatus.Document.RELEASED);
        workOrder.setVersion(2);
        return workOrder;
    }

    private MfgProductionPlan plan() {
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(4L);
        plan.setProductionOrderId(1L);
        plan.setPlannedQuantity(new BigDecimal("100"));
        plan.setStatus(BusinessStatus.Document.RELEASED);
        plan.setVersion(3);
        return plan;
    }

    private MfgProductionOrder order() {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(1L);
        order.setOrderQuantity(new BigDecimal("100"));
        order.setStatus(BusinessStatus.Document.RELEASED);
        order.setVersion(4);
        return order;
    }
}
