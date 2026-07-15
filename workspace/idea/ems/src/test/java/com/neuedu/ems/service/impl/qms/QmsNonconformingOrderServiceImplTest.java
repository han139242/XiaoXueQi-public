package com.neuedu.ems.service.impl.qms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.qms.QmsNonconformingOrder;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.sys.SysApprovalRecord;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.mapper.qms.QmsNonconformingDetailMapper;
import com.neuedu.ems.mapper.qms.QmsNonconformingOrderMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.sys.SysApprovalRecordMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() {
        service = new QmsNonconformingOrderServiceImpl();
        orderMapper = mock(QmsNonconformingOrderMapper.class);
        inspectionOrderMapper = mock(QmsInspectionOrderMapper.class);
        statusChangeLogMapper = mock(SysStatusChangeLogMapper.class);
        approvalRecordMapper = mock(SysApprovalRecordMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "qmsNonconformingDetailMapper", mock(QmsNonconformingDetailMapper.class));
        ReflectionTestUtils.setField(service, "qmsInspectionOrderMapper", inspectionOrderMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusChangeLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", mock(SysOperationLogMapper.class));
        ReflectionTestUtils.setField(service, "sysApprovalRecordMapper", approvalRecordMapper);
        ReflectionTestUtils.setField(service, "responsibilityNotificationService",
                mock(QmsResponsibilityNotificationService.class));
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
    void saveRequiresFinishedFailedInspectionAndNormalizesDisposition() {
        QmsInspectionOrder inspectionOrder = new QmsInspectionOrder();
        inspectionOrder.setInspectionId(10L);
        inspectionOrder.setStatus(BusinessStatus.Production.FINISHED);
        inspectionOrder.setInspectionResult(BusinessStatus.Quality.FAIL);
        when(inspectionOrderMapper.selectOne(any())).thenReturn(inspectionOrder);
        when(orderMapper.insert(any(QmsNonconformingOrder.class))).thenReturn(1);
        QmsNonconformingOrder request = new QmsNonconformingOrder();
        request.setInspectionId(10L);
        request.setDispositionType("rework");

        assertTrue(service.saveNonconformingOrder(request));

        ArgumentCaptor<QmsNonconformingOrder> saved = ArgumentCaptor.forClass(QmsNonconformingOrder.class);
        verify(orderMapper).insert(saved.capture());
        assertEquals(BusinessStatus.Quality.REWORK, saved.getValue().getDispositionType());
        assertEquals(10L, saved.getValue().getInspectionId());
    }

    @Test
    void saveRejectsNonFailedInspectionAndMissingDisposition() {
        QmsNonconformingOrder request = new QmsNonconformingOrder();
        request.setInspectionId(10L);
        request.setDispositionType("REWORK");

        assertFalse(service.saveNonconformingOrder(request));
        verify(orderMapper, never()).insert(any(QmsNonconformingOrder.class));

        QmsInspectionOrder failedInspection = new QmsInspectionOrder();
        failedInspection.setInspectionId(10L);
        when(inspectionOrderMapper.selectOne(any())).thenReturn(failedInspection);
        request.setDispositionType(null);
        assertFalse(service.saveNonconformingOrder(request));
    }

    private QmsNonconformingOrder existingOrder() {
        QmsNonconformingOrder order = new QmsNonconformingOrder();
        order.setNcrId(1L);
        order.setApprovalStatus(BusinessStatus.Document.SUBMITTED);
        order.setStatus(BusinessStatus.Document.SUBMITTED);
        return order;
    }
}
