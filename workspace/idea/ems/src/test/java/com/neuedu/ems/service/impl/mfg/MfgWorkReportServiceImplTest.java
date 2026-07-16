package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgReportMaterial;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.qms.QmsInspectionScheme;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.trace.TraceMaterialUsage;
import com.neuedu.ems.entity.trace.TraceProductLot;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgReportMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskOperatorMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionSchemeMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.mapper.trace.TraceMaterialUsageMapper;
import com.neuedu.ems.mapper.trace.TraceProductLotMapper;
import com.neuedu.ems.mapper.wms.WmsMaterialBatchMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundDetailMapper;
import com.neuedu.ems.mapper.wms.WmsReturnDetailMapper;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.entity.wms.WmsMaterialBatch;
import com.neuedu.ems.entity.wms.WmsOutboundDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
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
    private ProcessRouteItemMapper routeItemMapper;
    private WmsMaterialBatchMapper materialBatchMapper;
    private WmsOutboundDetailMapper outboundDetailMapper;
    private WmsReturnDetailMapper returnDetailMapper;
    private MfgReportMaterialMapper reportMaterialMapper;
    private TraceProductLotMapper productLotMapper;
    private TraceMaterialUsageMapper materialUsageMapper;
    private QmsInspectionSchemeMapper inspectionSchemeMapper;
    private QmsInspectionOrderMapper inspectionOrderMapper;

    @BeforeEach
    void setUp() {
        service = new MfgWorkReportServiceImpl();
        reportMapper = mock(MfgWorkReportMapper.class);
        taskMapper = mock(MfgTaskMapper.class);
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        orderMapper = mock(MfgProductionOrderMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", reportMapper);
        reportMaterialMapper = mock(MfgReportMaterialMapper.class);
        productLotMapper = mock(TraceProductLotMapper.class);
        materialUsageMapper = mock(TraceMaterialUsageMapper.class);
        inspectionSchemeMapper = mock(QmsInspectionSchemeMapper.class);
        inspectionOrderMapper = mock(QmsInspectionOrderMapper.class);
        ReflectionTestUtils.setField(service, "mfgReportMaterialMapper", reportMaterialMapper);
        ReflectionTestUtils.setField(service, "mfgTaskMapper", taskMapper);
        taskOperatorMapper = mock(MfgTaskOperatorMapper.class);
        userMapper = mock(SysUserMapper.class);
        routeItemMapper = mock(ProcessRouteItemMapper.class);
        materialBatchMapper = mock(WmsMaterialBatchMapper.class);
        outboundDetailMapper = mock(WmsOutboundDetailMapper.class);
        returnDetailMapper = mock(WmsReturnDetailMapper.class);
        ReflectionTestUtils.setField(service, "mfgTaskOperatorMapper", taskOperatorMapper);
        ReflectionTestUtils.setField(service, "sysUserMapper", userMapper);
        ReflectionTestUtils.setField(service, "mfgWorkOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "mfgProductionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "processRouteItemMapper", routeItemMapper);
        ReflectionTestUtils.setField(service, "wmsMaterialBatchMapper", materialBatchMapper);
        ReflectionTestUtils.setField(service, "wmsOutboundDetailMapper", outboundDetailMapper);
        ReflectionTestUtils.setField(service, "wmsReturnDetailMapper", returnDetailMapper);
        ReflectionTestUtils.setField(service, "traceProductLotMapper", productLotMapper);
        ReflectionTestUtils.setField(service, "traceMaterialUsageMapper", materialUsageMapper);
        ReflectionTestUtils.setField(service, "qmsInspectionSchemeMapper", inspectionSchemeMapper);
        ReflectionTestUtils.setField(service, "qmsInspectionOrderMapper", inspectionOrderMapper);
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
        ProcessRouteItem finalRouteItem = new ProcessRouteItem();
        finalRouteItem.setRouteId(6L);
        finalRouteItem.setOperationId(5L);
        finalRouteItem.setSequenceNo(10);
        when(routeItemMapper.selectList(any())).thenReturn(List.of(finalRouteItem));
        QmsInspectionScheme finalScheme = new QmsInspectionScheme();
        finalScheme.setSchemeId(21L);
        finalScheme.setProductId(8L);
        finalScheme.setOperationId(5L);
        finalScheme.setInspectionType("FINAL");
        finalScheme.setStatus(BusinessStatus.Document.RELEASED);
        when(inspectionSchemeMapper.selectList(any())).thenReturn(List.of(finalScheme));
        when(inspectionOrderMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            TraceProductLot lot = invocation.getArgument(0);
            lot.setProductLotId(31L);
            return 1;
        }).when(productLotMapper).insert(any(TraceProductLot.class));
        doAnswer(invocation -> {
            QmsInspectionOrder inspection = invocation.getArgument(0);
            inspection.setInspectionId(41L);
            return 1;
        }).when(inspectionOrderMapper).insert(any(QmsInspectionOrder.class));
        when(materialUsageMapper.insert(any(TraceMaterialUsage.class))).thenReturn(1);
    }

    @Test
    void approvedFinalReportCreatesPendingQualityChainWithoutPrematureCompletion() {
        MfgWorkReport report = report("100");
        MfgReportMaterial reportMaterial = new MfgReportMaterial();
        reportMaterial.setMaterialId(11L);
        reportMaterial.setBatchId(12L);
        reportMaterial.setUsedQuantity(new BigDecimal("8"));
        reportMaterial.setScrapQuantity(BigDecimal.ONE);
        when(reportMapper.selectOne(any())).thenReturn(report);
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        when(reportMapper.selectList(any())).thenReturn(List.of(), List.of(report));
        when(reportMaterialMapper.selectList(any())).thenReturn(List.of(reportMaterial));
        when(reportMapper.updateById(any(MfgWorkReport.class))).thenReturn(1);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);

        service.auditWorkReport(1L, " approved ", "审核通过", 9L);

        ArgumentCaptor<MfgWorkOrder> workOrderUpdate = ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(workOrderUpdate.capture());
        assertEquals(BigDecimal.ZERO, workOrderUpdate.getValue().getCompletedQuantity());
        assertEquals(BusinessStatus.Document.RELEASED, workOrderUpdate.getValue().getStatus());
        verify(planMapper, never()).updateById(any(MfgProductionPlan.class));
        verify(orderMapper, never()).updateById(any(MfgProductionOrder.class));

        ArgumentCaptor<TraceProductLot> productLot = ArgumentCaptor.forClass(TraceProductLot.class);
        verify(productLotMapper).insert(productLot.capture());
        assertEquals(1L, productLot.getValue().getSourceReportId());
        assertEquals("WAIT_INSPECTION", productLot.getValue().getStatus());
        ArgumentCaptor<QmsInspectionOrder> inspection = ArgumentCaptor.forClass(QmsInspectionOrder.class);
        verify(inspectionOrderMapper).insert(inspection.capture());
        assertEquals(31L, inspection.getValue().getProductLotId());
        assertEquals("FINAL", inspection.getValue().getInspectionType());

        ArgumentCaptor<TraceMaterialUsage> usage = ArgumentCaptor.forClass(TraceMaterialUsage.class);
        verify(materialUsageMapper).insert(usage.capture());
        assertEquals(31L, usage.getValue().getProductLotId());
        assertEquals(3L, usage.getValue().getWorkOrderId());
        assertEquals(1L, usage.getValue().getReportId());
        assertEquals(11L, usage.getValue().getMaterialId());
        assertEquals(12L, usage.getValue().getBatchId());
        assertEquals(new BigDecimal("9"), usage.getValue().getUsedQuantity());
    }

    @Test
    void approvalRejectsFinalReportWhenReleasedInspectionSchemeIsMissing() {
        MfgWorkReport report = report("20");
        when(reportMapper.selectOne(any())).thenReturn(report);
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        when(reportMapper.selectList(any())).thenReturn(List.of(), List.of(report));
        when(reportMapper.updateById(any(MfgWorkReport.class))).thenReturn(1);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);
        when(inspectionSchemeMapper.selectList(any())).thenReturn(List.of());

        assertThrows(BusinessException.class,
                () -> service.auditWorkReport(1L, BusinessStatus.Document.APPROVED, null, 9L));

        verify(productLotMapper).insert(any(TraceProductLot.class));
        verify(inspectionOrderMapper, never()).insert(any(QmsInspectionOrder.class));
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
    void intermediateOperationApprovalDoesNotIncreaseWorkOrderCompletion() {
        MfgWorkReport candidate = report("40");
        MfgTask intermediateTask = task();
        intermediateTask.setOperationId(4L);
        ProcessRouteItem intermediate = new ProcessRouteItem();
        intermediate.setRouteId(6L);
        intermediate.setOperationId(4L);
        intermediate.setSequenceNo(10);
        ProcessRouteItem terminal = new ProcessRouteItem();
        terminal.setRouteId(6L);
        terminal.setOperationId(5L);
        terminal.setSequenceNo(20);
        when(routeItemMapper.selectList(any())).thenReturn(List.of(terminal, intermediate));
        when(reportMapper.selectOne(any())).thenReturn(candidate);
        when(taskMapper.selectOne(any())).thenReturn(intermediateTask);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        when(reportMapper.selectList(any())).thenReturn(List.of());
        when(inspectionSchemeMapper.selectList(any())).thenReturn(List.of());
        when(reportMapper.updateById(any(MfgWorkReport.class))).thenReturn(1);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);

        service.auditWorkReport(1L, BusinessStatus.Document.APPROVED, null, 9L);

        ArgumentCaptor<MfgWorkOrder> update = ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(update.capture());
        assertEquals(BigDecimal.ZERO, update.getValue().getCompletedQuantity());
        assertEquals(BusinessStatus.Document.RELEASED, update.getValue().getStatus());
    }

    @Test
    void concurrentWorkOrderAggregateConflictRaisesExceptionForTransactionRollback() {
        MfgWorkReport candidate = report("20");
        when(reportMapper.selectOne(any())).thenReturn(candidate);
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        when(reportMapper.selectList(any())).thenReturn(List.of(approvedReport("80")));
        when(reportMapper.updateById(any(MfgWorkReport.class))).thenReturn(1);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.auditWorkReport(1L, BusinessStatus.Document.APPROVED, null, 9L));

        verify(reportMapper).updateById(any(MfgWorkReport.class));
        verify(workOrderMapper).updateById(any(MfgWorkOrder.class));
    }

    @Test
    void releasedProductLotRollsCompletionUpToWorkOrderPlanAndOrder() {
        MfgWorkOrder workOrder = workOrder();
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        TraceProductLot releasedLot = new TraceProductLot();
        releasedLot.setWorkOrderId(3L);
        releasedLot.setQualifiedQuantity(new BigDecimal("100"));
        releasedLot.setStatus(BusinessStatus.Quality.PASS);
        when(productLotMapper.selectList(any())).thenReturn(List.of(releasedLot));
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

        service.recalculateWorkOrderCompletion(3L, 9L, "终检放行");

        ArgumentCaptor<MfgWorkOrder> workOrderUpdate = ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(workOrderUpdate.capture());
        assertEquals(new BigDecimal("100"), workOrderUpdate.getValue().getCompletedQuantity());
        assertEquals(BusinessStatus.Production.FINISHED, workOrderUpdate.getValue().getStatus());
        verify(planMapper).updateById(any(MfgProductionPlan.class));
        verify(orderMapper).updateById(any(MfgProductionOrder.class));
    }

    @Test
    void dispositionedLotsCountReleasedConformingAndConcessionQuantities() {
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        TraceProductLot scrappedLot = new TraceProductLot();
        scrappedLot.setQualifiedQuantity(new BigDecimal("90"));
        scrappedLot.setDefectQuantity(new BigDecimal("10"));
        scrappedLot.setBatchQuantity(new BigDecimal("100"));
        scrappedLot.setStatus("SCRAPPED");
        TraceProductLot concessionLot = new TraceProductLot();
        concessionLot.setQualifiedQuantity(new BigDecimal("8"));
        concessionLot.setDefectQuantity(new BigDecimal("2"));
        concessionLot.setBatchQuantity(new BigDecimal("10"));
        concessionLot.setStatus(BusinessStatus.Quality.CONCESSION);
        when(productLotMapper.selectList(any())).thenReturn(List.of(scrappedLot, concessionLot));
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);

        service.recalculateWorkOrderCompletion(3L, 9L, "处置完成");

        ArgumentCaptor<MfgWorkOrder> update = ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(update.capture());
        assertEquals(new BigDecimal("100"), update.getValue().getCompletedQuantity());
        assertEquals(BusinessStatus.Production.FINISHED, update.getValue().getStatus());
    }

    @Test
    void intermediateOperationWithReleasedSchemeCreatesProcessInspection() {
        MfgWorkReport candidate = report("40");
        MfgTask intermediateTask = task();
        intermediateTask.setOperationId(4L);
        ProcessRouteItem intermediate = new ProcessRouteItem();
        intermediate.setRouteId(6L);
        intermediate.setOperationId(4L);
        intermediate.setSequenceNo(10);
        ProcessRouteItem terminal = new ProcessRouteItem();
        terminal.setRouteId(6L);
        terminal.setOperationId(5L);
        terminal.setSequenceNo(20);
        when(routeItemMapper.selectList(any())).thenReturn(List.of(terminal, intermediate));
        when(reportMapper.selectOne(any())).thenReturn(candidate);
        when(taskMapper.selectOne(any())).thenReturn(intermediateTask);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        when(reportMapper.selectList(any())).thenReturn(List.of());
        when(reportMapper.updateById(any(MfgWorkReport.class))).thenReturn(1);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);
        QmsInspectionScheme processScheme = new QmsInspectionScheme();
        processScheme.setSchemeId(22L);
        processScheme.setInspectionType("PROCESS");
        processScheme.setStatus(BusinessStatus.Document.RELEASED);
        when(inspectionSchemeMapper.selectList(any())).thenReturn(List.of(processScheme));

        service.auditWorkReport(1L, BusinessStatus.Document.APPROVED, null, 9L);

        ArgumentCaptor<QmsInspectionOrder> inspection = ArgumentCaptor.forClass(QmsInspectionOrder.class);
        verify(inspectionOrderMapper).insert(inspection.capture());
        assertEquals("PROCESS", inspection.getValue().getInspectionType());
        assertEquals(1L, inspection.getValue().getReportId());
        assertEquals(intermediateTask.getTaskId(), inspection.getValue().getTaskId());
    }

    @Test
    void multipleReleasedProcessSchemesBlockApprovalTransaction() {
        MfgWorkReport candidate = report("40");
        MfgTask intermediateTask = task();
        intermediateTask.setOperationId(4L);
        ProcessRouteItem intermediate = new ProcessRouteItem();
        intermediate.setOperationId(4L);
        intermediate.setSequenceNo(10);
        ProcessRouteItem terminal = new ProcessRouteItem();
        terminal.setOperationId(5L);
        terminal.setSequenceNo(20);
        when(routeItemMapper.selectList(any())).thenReturn(List.of(terminal, intermediate));
        when(reportMapper.selectOne(any())).thenReturn(candidate);
        when(taskMapper.selectOne(any())).thenReturn(intermediateTask);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder());
        when(reportMapper.selectList(any())).thenReturn(List.of());
        when(reportMapper.updateById(any(MfgWorkReport.class))).thenReturn(1);
        QmsInspectionScheme first = new QmsInspectionScheme();
        first.setSchemeId(22L);
        QmsInspectionScheme second = new QmsInspectionScheme();
        second.setSchemeId(23L);
        when(inspectionSchemeMapper.selectList(any())).thenReturn(List.of(first, second));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.auditWorkReport(1L, BusinessStatus.Document.APPROVED, null, 9L));

        assertTrue(exception.getMessage().contains("多个已发布过程检方案"));
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
    void submitRejectsMaterialConsumptionAboveConfirmedIssueForBatch() {
        MfgTask runningTask = task();
        runningTask.setStatus(BusinessStatus.Production.RUNNING);
        runningTask.setStationId(4L);
        when(taskMapper.selectOne(any())).thenReturn(runningTask);
        WmsMaterialBatch batch = new WmsMaterialBatch();
        batch.setBatchId(12L);
        batch.setMaterialId(11L);
        when(materialBatchMapper.selectOne(any())).thenReturn(batch);
        WmsOutboundDetail issued = new WmsOutboundDetail();
        issued.setMaterialId(11L);
        issued.setBatchId(12L);
        issued.setOutboundQuantity(new BigDecimal("5"));
        when(outboundDetailMapper.selectList(any())).thenReturn(List.of(issued));
        when(returnDetailMapper.selectList(any())).thenReturn(List.of());
        MfgWorkReport request = new MfgWorkReport();
        request.setTaskId(2L);
        request.setQualifiedQuantity(BigDecimal.ONE);
        request.setDefectQuantity(BigDecimal.ZERO);
        request.setActualWorkHours(BigDecimal.ONE);
        MfgReportMaterial material = new MfgReportMaterial();
        material.setMaterialId(11L);
        material.setBatchId(12L);
        material.setUsedQuantity(new BigDecimal("6"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submitWorkReport(request, List.of(material), 9L));

        assertTrue(exception.getMessage().contains("超过本工单该批次可用领料量"));
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

    @Test
    void withdrawOwnSubmittedReportAllowsBlankReason() {
        MfgWorkReport submitted = report("1");
        submitted.setOperatorId(3L);
        when(reportMapper.selectOne(any())).thenReturn(submitted);
        when(reportMapper.updateById(any(MfgWorkReport.class))).thenReturn(1);

        service.withdrawWorkReport(1L, 9L, null);

        ArgumentCaptor<MfgWorkReport> update = ArgumentCaptor.forClass(MfgWorkReport.class);
        verify(reportMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.WITHDRAWN, update.getValue().getAuditStatus());
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
        task.setOperationId(5L);
        return task;
    }

    private MfgWorkOrder workOrder() {
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setPlanId(4L);
        workOrder.setRouteId(6L);
        workOrder.setProductId(8L);
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
