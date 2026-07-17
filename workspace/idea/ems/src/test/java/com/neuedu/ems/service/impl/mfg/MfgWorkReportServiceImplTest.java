package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgReportMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.mfg.MfgTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private ProcessRouteItemMapper routeItemMapper;
    private MfgTaskService taskService;

    @BeforeEach
    void setUp() {
        service = new MfgWorkReportServiceImpl();
        reportMapper = mock(MfgWorkReportMapper.class);
        taskMapper = mock(MfgTaskMapper.class);
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        orderMapper = mock(MfgProductionOrderMapper.class);
        routeItemMapper = mock(ProcessRouteItemMapper.class);
        taskService = mock(MfgTaskService.class);
        ReflectionTestUtils.setField(service, "baseMapper", reportMapper);
        ReflectionTestUtils.setField(service, "mfgReportMaterialMapper", mock(MfgReportMaterialMapper.class));
        ReflectionTestUtils.setField(service, "mfgTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "mfgWorkOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "processRouteItemMapper", routeItemMapper);
        ReflectionTestUtils.setField(service, "mfgTaskService", taskService);
        SysStatusChangeLogMapper statusLogMapper = mock(SysStatusChangeLogMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
        when(routeItemMapper.selectOne(any())).thenReturn(finalRouteItem());
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
        when(taskMapper.selectOne(any())).thenReturn(task());
        MfgWorkReport request = new MfgWorkReport();
        request.setTaskId(2L);
        request.setOperatorId(3L);
        request.setStationId(4L);
        request.setQualifiedQuantity(new BigDecimal("-1"));
        request.setDefectQuantity(BigDecimal.ZERO);
        request.setActualWorkHours(BigDecimal.ONE);

        assertThrows(BusinessException.class, () -> service.submitWorkReport(request, List.of()));

        verify(reportMapper, never()).insert(any(MfgWorkReport.class));
    }

    @Test
    void keywordSearchMatchesReportNumberOrTaskNumber() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), MfgWorkReport.class);
        LambdaQueryWrapper<MfgWorkReport> query = ReflectionTestUtils.invokeMethod(
                service, "buildWorkReportQuery", "TASK-20260714-003-1-050", null, null, null, null);

        assertTrue(query.getSqlSegment().contains("report_no LIKE"));
        assertTrue(query.getSqlSegment().contains("SELECT task_id FROM mfg_task"));
        assertTrue(query.getSqlSegment().contains("task_no LIKE"));
        assertTrue(query.getParamNameValuePairs().containsValue("TASK-20260714-003-1-050"));
    }

    @Test
    void submitAutomaticallyFinishesTaskWhenCumulativeQuantityReachesPlan() {
        MfgTask task = task();
        MfgWorkReport existing = report("80");
        MfgWorkReport request = validReportRequest();
        request.setQualifiedQuantity(new BigDecimal("15"));
        request.setDefectQuantity(new BigDecimal("5"));
        when(taskMapper.selectOne(any())).thenReturn(task);
        when(reportMapper.selectList(any())).thenReturn(List.of(existing), List.of(existing, request));
        when(reportMapper.selectCount(any())).thenReturn(0L);
        when(reportMapper.insert(any(MfgWorkReport.class))).thenAnswer(invocation -> {
            request.setReportId(2L);
            return 1;
        });
        when(taskService.finishTask(2L, new BigDecimal("95"), new BigDecimal("5"),
                "累计报工达到任务计划数量，系统自动完工", 3L)).thenReturn(true);

        service.submitWorkReport(request, List.of());

        verify(taskService).finishTask(2L, new BigDecimal("95"), new BigDecimal("5"),
                "累计报工达到任务计划数量，系统自动完工", 3L);
    }

    @Test
    void submitKeepsTaskRunningWhenCumulativeQuantityIsBelowPlan() {
        MfgWorkReport existing = report("50");
        MfgWorkReport request = validReportRequest();
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(reportMapper.selectList(any())).thenReturn(List.of(existing), List.of(existing, request));
        when(reportMapper.selectCount(any())).thenReturn(0L);
        when(reportMapper.insert(any(MfgWorkReport.class))).thenAnswer(invocation -> {
            request.setReportId(2L);
            return 1;
        });

        service.submitWorkReport(request, List.of());

        verify(taskService, never()).finishTask(any(), any(), any(), any(), any());
    }

    @Test
    void saveDerivesReportNumberFromTaskNumberAndSequence() {
        MfgTask task = task();
        task.setTaskNo("TASK-20260714-003-1-050");
        when(taskMapper.selectOne(any())).thenReturn(task);
        when(reportMapper.selectCount(any())).thenReturn(1L, 0L);
        when(reportMapper.insert(any(MfgWorkReport.class))).thenReturn(1);
        MfgWorkReport request = new MfgWorkReport();
        request.setTaskId(2L);
        request.setOperatorId(3L);
        request.setCreatedBy(3L);
        request.setStationId(4L);
        request.setQualifiedQuantity(BigDecimal.TEN);
        request.setDefectQuantity(BigDecimal.ZERO);
        request.setActualWorkHours(BigDecimal.ONE);

        assertTrue(service.saveWorkReport(request));
        assertEquals("WR-20260714-003-1-050-2", request.getReportNo());
    }

    @Test
    void saveRejectsTaskThatIsNotRunning() {
        MfgTask task = task();
        task.setStatus(BusinessStatus.Production.FINISHED);
        when(taskMapper.selectOne(any())).thenReturn(task);
        MfgWorkReport request = validReportRequest();

        assertThrows(BusinessException.class, () -> service.saveWorkReport(request));

        verify(reportMapper, never()).insert(any(MfgWorkReport.class));
    }

    @Test
    void saveRejectsCumulativeQuantityAboveTaskPlan() {
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(reportMapper.selectList(any())).thenReturn(List.of(report("95")));
        MfgWorkReport request = validReportRequest();

        assertThrows(BusinessException.class, () -> service.saveWorkReport(request));

        verify(reportMapper, never()).insert(any(MfgWorkReport.class));
    }

    private MfgWorkReport validReportRequest() {
        MfgWorkReport request = new MfgWorkReport();
        request.setTaskId(2L);
        request.setOperatorId(3L);
        request.setCreatedBy(3L);
        request.setStationId(4L);
        request.setQualifiedQuantity(BigDecimal.TEN);
        request.setDefectQuantity(BigDecimal.ZERO);
        request.setActualWorkHours(BigDecimal.ONE);
        return request;
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
        task.setTaskNo("TASK-20260714-003-1-050");
        task.setWorkOrderId(3L);
        task.setOperationId(7L);
        task.setPlannedQuantity(new BigDecimal("100"));
        task.setStatus(BusinessStatus.Production.RUNNING);
        return task;
    }

    private MfgWorkOrder workOrder() {
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setPlanId(4L);
        workOrder.setRouteId(5L);
        workOrder.setPlannedQuantity(new BigDecimal("100"));
        workOrder.setStatus(BusinessStatus.Document.RELEASED);
        workOrder.setVersion(2);
        return workOrder;
    }

    private ProcessRouteItem finalRouteItem() {
        ProcessRouteItem routeItem = new ProcessRouteItem();
        routeItem.setRouteId(5L);
        routeItem.setOperationId(7L);
        routeItem.setSequenceNo(70);
        return routeItem;
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
