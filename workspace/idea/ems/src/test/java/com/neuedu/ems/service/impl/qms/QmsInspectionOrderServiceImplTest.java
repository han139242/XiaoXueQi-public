package com.neuedu.ems.service.impl.qms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.wms.WmsMaterialBatch;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionResultMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.wms.WmsMaterialBatchMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QmsInspectionOrderServiceImplTest {

    private QmsInspectionOrderServiceImpl service;
    private QmsInspectionOrderMapper orderMapper;
    private WmsMaterialBatchMapper batchMapper;
    private SysStatusChangeLogMapper statusLogMapper;
    private SysOperationLogMapper operationLogMapper;

    @BeforeEach
    void setUp() {
        service = new QmsInspectionOrderServiceImpl();
        orderMapper = mock(QmsInspectionOrderMapper.class);
        batchMapper = mock(WmsMaterialBatchMapper.class);
        statusLogMapper = mock(SysStatusChangeLogMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "qmsInspectionResultMapper", mock(QmsInspectionResultMapper.class));
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "wmsMaterialBatchMapper", batchMapper);
        ReflectionTestUtils.setField(service, "responsibilityNotificationService",
                mock(QmsResponsibilityNotificationService.class));
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void submitNormalizesQualityResultAndFinishesOrder() {
        when(orderMapper.selectOne(any())).thenReturn(existingOrder());
        when(orderMapper.updateById(any(QmsInspectionOrder.class))).thenReturn(1);

        boolean result = service.submitInspectionOrder(1L, " pass ", "完成检验", 9L);

        assertTrue(result);
        ArgumentCaptor<QmsInspectionOrder> update = ArgumentCaptor.forClass(QmsInspectionOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Quality.PASS, update.getValue().getInspectionResult());
        assertEquals(BusinessStatus.Production.FINISHED, update.getValue().getStatus());
    }

    @Test
    void submitRejectsChineseQualityResultBeforeDatabaseMutation() {
        assertFalse(service.submitInspectionOrder(1L, "合格", null, 9L));
        verify(orderMapper, never()).selectOne(any());
        verify(orderMapper, never()).updateById(any(QmsInspectionOrder.class));
    }

    @Test
    void passingInspectionReleasesLinkedMaterialBatch() {
        QmsInspectionOrder order = existingOrder();
        order.setBatchId(7L);
        WmsMaterialBatch batch = batch("WAIT_INSPECTION", "FROZEN");
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(batchMapper.selectOne(any())).thenReturn(batch);
        when(orderMapper.updateById(any(QmsInspectionOrder.class))).thenReturn(1);
        when(batchMapper.updateById(any(WmsMaterialBatch.class))).thenReturn(1);

        assertTrue(service.submitInspectionOrder(1L, "PASS", null, 9L));

        ArgumentCaptor<WmsMaterialBatch> update = ArgumentCaptor.forClass(WmsMaterialBatch.class);
        verify(batchMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Quality.PASS, update.getValue().getQualityStatus());
        assertEquals("AVAILABLE", update.getValue().getStatus());
        assertEquals(2, update.getValue().getVersion());
    }

    @Test
    void failedInspectionKeepsLinkedMaterialBatchFrozen() {
        QmsInspectionOrder order = existingOrder();
        order.setBatchId(7L);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(batchMapper.selectOne(any())).thenReturn(batch("WAIT_INSPECTION", "FROZEN"));
        when(orderMapper.updateById(any(QmsInspectionOrder.class))).thenReturn(1);
        when(batchMapper.updateById(any(WmsMaterialBatch.class))).thenReturn(1);

        assertTrue(service.submitInspectionOrder(1L, "FAIL", null, 9L));

        ArgumentCaptor<WmsMaterialBatch> update = ArgumentCaptor.forClass(WmsMaterialBatch.class);
        verify(batchMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Quality.FAIL, update.getValue().getQualityStatus());
        assertEquals("FROZEN", update.getValue().getStatus());
    }

    @Test
    void batchOptimisticConflictFailsWholeInspectionCommand() {
        QmsInspectionOrder order = existingOrder();
        order.setBatchId(7L);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(batchMapper.selectOne(any())).thenReturn(batch("WAIT_INSPECTION", "FROZEN"));
        when(orderMapper.updateById(any(QmsInspectionOrder.class))).thenReturn(1);
        when(batchMapper.updateById(any(WmsMaterialBatch.class))).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.submitInspectionOrder(1L, "PASS", null, 9L));
    }

    @Test
    void ordinaryUpdateCannotOverwriteResultOrStatus() {
        when(orderMapper.selectOne(any())).thenReturn(existingOrder());
        when(orderMapper.updateById(any(QmsInspectionOrder.class))).thenReturn(1);
        QmsInspectionOrder request = new QmsInspectionOrder();
        request.setInspectionId(1L);
        request.setInspectionResult(BusinessStatus.Quality.FAIL);
        request.setStatus(BusinessStatus.Production.FINISHED);

        assertTrue(service.updateInspectionOrder(request));

        ArgumentCaptor<QmsInspectionOrder> update = ArgumentCaptor.forClass(QmsInspectionOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertNull(update.getValue().getInspectionResult());
        assertNull(update.getValue().getStatus());
    }

    private QmsInspectionOrder existingOrder() {
        QmsInspectionOrder order = new QmsInspectionOrder();
        order.setInspectionId(1L);
        order.setStatus(BusinessStatus.Document.DRAFT);
        return order;
    }

    private WmsMaterialBatch batch(String qualityStatus, String status) {
        WmsMaterialBatch batch = new WmsMaterialBatch();
        batch.setBatchId(7L);
        batch.setMaterialId(11L);
        batch.setQualityStatus(qualityStatus);
        batch.setStatus(status);
        batch.setVersion(2);
        return batch;
    }
}
