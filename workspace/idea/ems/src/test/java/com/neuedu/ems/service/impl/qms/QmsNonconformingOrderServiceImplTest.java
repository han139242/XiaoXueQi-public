package com.neuedu.ems.service.impl.qms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.qms.QmsNonconformingOrder;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.qms.QmsNonconformingDetail;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.trace.TraceProductLot;
import com.neuedu.ems.entity.sys.SysApprovalRecord;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.mapper.qms.QmsNonconformingDetailMapper;
import com.neuedu.ems.mapper.qms.QmsNonconformingOrderMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.trace.TraceProductLotMapper;
import com.neuedu.ems.mapper.sys.SysApprovalRecordMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.mfg.MfgTaskService;
import com.neuedu.ems.service.mfg.MfgWorkOrderService;
import com.neuedu.ems.service.mfg.MfgWorkReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QmsNonconformingOrderServiceImplTest {

    private QmsNonconformingOrderServiceImpl service;
    private QmsNonconformingOrderMapper orderMapper;
    private QmsInspectionOrderMapper inspectionOrderMapper;
    private SysStatusChangeLogMapper statusChangeLogMapper;
    private SysApprovalRecordMapper approvalRecordMapper;
    private SysOperationLogMapper operationLogMapper;
    private QmsNonconformingDetailMapper detailMapper;
    private MfgTaskMapper taskMapper;
    private TraceProductLotMapper productLotMapper;
    private MfgTaskService taskService;
    private MfgWorkOrderService workOrderService;
    private MfgWorkReportService workReportService;

    @BeforeEach
    void setUp() {
        service = new QmsNonconformingOrderServiceImpl();
        orderMapper = mock(QmsNonconformingOrderMapper.class);
        inspectionOrderMapper = mock(QmsInspectionOrderMapper.class);
        statusChangeLogMapper = mock(SysStatusChangeLogMapper.class);
        approvalRecordMapper = mock(SysApprovalRecordMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        detailMapper = mock(QmsNonconformingDetailMapper.class);
        taskMapper = mock(MfgTaskMapper.class);
        productLotMapper = mock(TraceProductLotMapper.class);
        taskService = mock(MfgTaskService.class);
        workOrderService = mock(MfgWorkOrderService.class);
        workReportService = mock(MfgWorkReportService.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "qmsNonconformingDetailMapper", detailMapper);
        ReflectionTestUtils.setField(service, "qmsInspectionOrderMapper", inspectionOrderMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusChangeLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "sysApprovalRecordMapper", approvalRecordMapper);
        ReflectionTestUtils.setField(service, "mfgTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "mfgWorkReportMapper", mock(MfgWorkReportMapper.class));
        ReflectionTestUtils.setField(service, "traceProductLotMapper", productLotMapper);
        ReflectionTestUtils.setField(service, "mfgTaskService", taskService);
        ReflectionTestUtils.setField(service, "mfgWorkOrderService", workOrderService);
        ReflectionTestUtils.setField(service, "mfgWorkReportService", workReportService);
        ReflectionTestUtils.setField(service, "responsibilityNotificationService",
                mock(QmsResponsibilityNotificationService.class));
        when(statusChangeLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
        when(detailMapper.insert(any(QmsNonconformingDetail.class))).thenReturn(1);
    }

    @Test
    void approvedResultMovesBusinessStatusToProcessing() {
        when(orderMapper.selectOne(any())).thenReturn(existingOrder());
        when(orderMapper.updateById(any(QmsNonconformingOrder.class))).thenReturn(1);

        boolean result = service.approveNonconformingOrder(1L, "approved", "通过", 9L);

        assertTrue(result);
        ArgumentCaptor<QmsNonconformingOrder> update = ArgumentCaptor.forClass(QmsNonconformingOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.APPROVED, update.getValue().getApprovalStatus());
        assertEquals(BusinessStatus.Workflow.PROCESSING, update.getValue().getStatus());
        ArgumentCaptor<SysStatusChangeLog> statusLog = ArgumentCaptor.forClass(SysStatusChangeLog.class);
        verify(statusChangeLogMapper).insert(statusLog.capture());
        assertEquals(BusinessStatus.Document.SUBMITTED, statusLog.getValue().getOldStatus());
        assertEquals(BusinessStatus.Workflow.PROCESSING, statusLog.getValue().getNewStatus());
        ArgumentCaptor<SysApprovalRecord> approvalRecord = ArgumentCaptor.forClass(SysApprovalRecord.class);
        verify(approvalRecordMapper).insert(approvalRecord.capture());
        assertEquals(BusinessStatus.Document.APPROVED, approvalRecord.getValue().getApprovalResult());
    }

    @Test
    void rejectedResultClosesBusinessOrder() {
        when(orderMapper.selectOne(any())).thenReturn(existingOrder());
        when(orderMapper.updateById(any(QmsNonconformingOrder.class))).thenReturn(1);

        boolean result = service.approveNonconformingOrder(1L, BusinessStatus.Document.REJECTED, "驳回", 9L);

        assertTrue(result);
        ArgumentCaptor<QmsNonconformingOrder> update = ArgumentCaptor.forClass(QmsNonconformingOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.REJECTED, update.getValue().getApprovalStatus());
        assertEquals(BusinessStatus.Document.CLOSED, update.getValue().getStatus());
    }

    @Test
    void invalidApprovalResultIsRejectedBeforeDatabaseMutation() {
        boolean result = service.approveNonconformingOrder(1L, "同意", null, 9L);

        assertFalse(result);
        verify(orderMapper, never()).selectOne(any());
        verify(orderMapper, never()).updateById(any(QmsNonconformingOrder.class));
    }

    @Test
    void ordinaryUpdateCannotOverwriteApprovalOrBusinessStatus() {
        when(orderMapper.selectOne(any())).thenReturn(existingOrder());
        when(orderMapper.updateById(any(QmsNonconformingOrder.class))).thenReturn(1);
        QmsNonconformingOrder request = new QmsNonconformingOrder();
        request.setNcrId(1L);
        request.setApprovalStatus(BusinessStatus.Document.APPROVED);
        request.setStatus(BusinessStatus.Document.CLOSED);

        assertTrue(service.updateNonconformingOrder(request));

        ArgumentCaptor<QmsNonconformingOrder> update = ArgumentCaptor.forClass(QmsNonconformingOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertNull(update.getValue().getApprovalStatus());
        assertNull(update.getValue().getStatus());
    }

    @Test
    void blankCreationIsRejectedInFavorOfInspectionSourceCommand() {
        QmsNonconformingOrder request = new QmsNonconformingOrder();
        request.setInspectionId(10L);
        request.setDispositionType("rework");

        assertThrows(com.neuedu.ems.exception.BusinessException.class,
                () -> service.saveNonconformingOrder(request));
        verify(orderMapper, never()).insert(any(QmsNonconformingOrder.class));
    }

    @Test
    void generateFromFailedInspectionNormalizesDispositionAndIsIdempotent() {
        QmsInspectionOrder inspection = failedInspection();
        when(inspectionOrderMapper.selectOne(any())).thenReturn(inspection);
        when(orderMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            QmsNonconformingOrder saved = invocation.getArgument(0);
            saved.setNcrId(20L);
            return 1;
        }).when(orderMapper).insert(any(QmsNonconformingOrder.class));

        QmsNonconformingOrder created = service.generateFromInspection(
                10L, "rework", "外观破损", null, 9L, "自动发起");

        assertEquals(20L, created.getNcrId());
        assertEquals(BusinessStatus.Quality.REWORK, created.getDispositionType());
        assertEquals(BusinessStatus.Document.SUBMITTED, created.getApprovalStatus());
        ArgumentCaptor<QmsNonconformingDetail> detail = ArgumentCaptor.forClass(QmsNonconformingDetail.class);
        verify(detailMapper).insert(detail.capture());
        assertEquals(new BigDecimal("2"), detail.getValue().getDefectQuantity());

        when(orderMapper.selectList(any())).thenReturn(List.of(created));
        QmsNonconformingOrder repeated = service.generateFromInspection(
                10L, "SCRAP", "重复点击", BigDecimal.ONE, 9L, null);
        assertEquals(20L, repeated.getNcrId());
        verify(orderMapper).insert(any(QmsNonconformingOrder.class));
    }

    @Test
    void approvedReworkCompletionCreatesReworkTaskWithoutDefaultSupplementMaterial() {
        prepareCompletion(BusinessStatus.Quality.REWORK);
        MfgTask task = new MfgTask();
        task.setTaskId(99L);
        when(taskService.createCompensationTask(eq(4L), eq(1L), eq(new BigDecimal("2")),
                eq("REWORK"), eq(9L), any())).thenReturn(task);

        Long taskId = service.completeDisposition(1L, false, 9L, "返工方案已确认");

        assertEquals(99L, taskId);
        verify(taskService).createCompensationTask(eq(4L), eq(1L), eq(new BigDecimal("2")),
                eq("REWORK"), eq(9L), any());
        verify(workOrderService, never()).addSupplementMaterialRequirements(any(), any(), any(), any());
        verify(workReportService).recalculateWorkOrderCompletion(3L, 9L,
                "不合格品处理单 1 完成处置 REWORK");
    }

    @Test
    void approvedScrapCompletionCreatesSupplementTaskAndMaterialRequirement() {
        prepareCompletion(BusinessStatus.Quality.SCRAP);
        MfgTask task = new MfgTask();
        task.setTaskId(100L);
        when(taskService.createCompensationTask(eq(4L), eq(1L), eq(new BigDecimal("2")),
                eq("SUPPLEMENT"), eq(9L), any())).thenReturn(task);

        Long taskId = service.completeDisposition(1L, false, 9L, "报废并补产");

        assertEquals(100L, taskId);
        verify(workOrderService).addSupplementMaterialRequirements(3L, new BigDecimal("2"), 9L,
                "不合格品处理单 NCR-001，处置=SCRAP");
    }

    @Test
    void approvedConcessionCompletionReleasesLotWithoutCompensationTask() {
        prepareCompletion(BusinessStatus.Quality.CONCESSION);

        Long taskId = service.completeDisposition(1L, false, 9L, "偏差在许可范围内");

        assertNull(taskId);
        verify(taskService, never()).createCompensationTask(any(), any(), any(), any(), any(), any());
        verify(workOrderService, never()).addSupplementMaterialRequirements(any(), any(), any(), any());
        ArgumentCaptor<TraceProductLot> lotUpdate = ArgumentCaptor.forClass(TraceProductLot.class);
        verify(productLotMapper).updateById(lotUpdate.capture());
        assertEquals(BusinessStatus.Quality.CONCESSION, lotUpdate.getValue().getStatus());
    }

    private void prepareCompletion(String disposition) {
        QmsNonconformingOrder order = new QmsNonconformingOrder();
        order.setNcrId(1L);
        order.setNcrNo("NCR-001");
        order.setInspectionId(10L);
        order.setProductLotId(31L);
        order.setDispositionType(disposition);
        order.setApprovalStatus(BusinessStatus.Document.APPROVED);
        order.setStatus(BusinessStatus.Workflow.PROCESSING);
        order.setVersion(2);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.updateById(any(QmsNonconformingOrder.class))).thenReturn(1);
        when(detailMapper.selectList(any())).thenReturn(List.of(detail()));
        when(detailMapper.updateById(any(QmsNonconformingDetail.class))).thenReturn(1);
        when(inspectionOrderMapper.selectOne(any())).thenReturn(failedInspection());
        TraceProductLot lot = new TraceProductLot();
        lot.setProductLotId(31L);
        lot.setStatus(BusinessStatus.Quality.FAIL);
        lot.setVersion(3);
        when(productLotMapper.selectOne(any())).thenReturn(lot);
        when(productLotMapper.updateById(any(TraceProductLot.class))).thenReturn(1);
    }

    private QmsInspectionOrder failedInspection() {
        QmsInspectionOrder inspection = new QmsInspectionOrder();
        inspection.setInspectionId(10L);
        inspection.setInspectionNo("QI-010");
        inspection.setTaskId(4L);
        inspection.setWorkOrderId(3L);
        inspection.setProductLotId(31L);
        inspection.setUnqualifiedQuantity(new BigDecimal("2"));
        inspection.setStatus(BusinessStatus.Production.FINISHED);
        inspection.setInspectionResult(BusinessStatus.Quality.FAIL);
        return inspection;
    }

    private QmsNonconformingDetail detail() {
        QmsNonconformingDetail detail = new QmsNonconformingDetail();
        detail.setDetailId(5L);
        detail.setNcrId(1L);
        detail.setDefectQuantity(new BigDecimal("2"));
        return detail;
    }

    private QmsNonconformingOrder existingOrder() {
        QmsNonconformingOrder order = new QmsNonconformingOrder();
        order.setNcrId(1L);
        order.setApprovalStatus(BusinessStatus.Document.SUBMITTED);
        order.setStatus(BusinessStatus.Document.SUBMITTED);
        return order;
    }
}
