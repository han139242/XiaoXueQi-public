package com.neuedu.ems.service.impl.qms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionResultMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QmsInspectionOrderServiceImplTest {

    private QmsInspectionOrderServiceImpl service;
    private QmsInspectionOrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        service = new QmsInspectionOrderServiceImpl();
        orderMapper = mock(QmsInspectionOrderMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "qmsInspectionResultMapper", mock(QmsInspectionResultMapper.class));
        ReflectionTestUtils.setField(service, "mfgWorkReportMapper", mock(MfgWorkReportMapper.class));
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", mock(SysStatusChangeLogMapper.class));
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", mock(SysOperationLogMapper.class));
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
        assertNotNull(update.getValue().getInspectionTime());
    }

    @Test
    void submitRejectsChineseQualityResultBeforeDatabaseMutation() {
        assertFalse(service.submitInspectionOrder(1L, "合格", null, 9L));
        verify(orderMapper, never()).selectOne(any());
        verify(orderMapper, never()).updateById(any(QmsInspectionOrder.class));
    }

    @Test
    void ordinaryUpdateCannotOverwriteResultOrStatus() {
        when(orderMapper.selectOne(any())).thenReturn(existingOrder());
        when(orderMapper.updateById(any(QmsInspectionOrder.class))).thenReturn(1);
        QmsInspectionOrder request = new QmsInspectionOrder();
        request.setInspectionId(1L);
        request.setInspectionResult(BusinessStatus.Quality.FAIL);
        request.setStatus(BusinessStatus.Production.FINISHED);
        request.setSampleQuantity(BigDecimal.TEN);

        assertTrue(service.updateInspectionOrder(request));

        ArgumentCaptor<QmsInspectionOrder> update = ArgumentCaptor.forClass(QmsInspectionOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertNull(update.getValue().getInspectionResult());
        assertNull(update.getValue().getStatus());
        assertNull(update.getValue().getSampleQuantity());
    }

    @Test
    void submitAcceptsConsistentInspectionQuantities() {
        when(orderMapper.selectOne(any())).thenReturn(existingOrder());
        when(orderMapper.updateById(any(QmsInspectionOrder.class))).thenReturn(1);

        assertTrue(service.submitInspectionOrder(1L, "FAIL", "发现不良", 9L,
                BigDecimal.TEN, new BigDecimal("8"), new BigDecimal("2")));

        ArgumentCaptor<QmsInspectionOrder> update = ArgumentCaptor.forClass(QmsInspectionOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BigDecimal.TEN, update.getValue().getSampleQuantity());
        assertEquals(new BigDecimal("8"), update.getValue().getQualifiedQuantity());
        assertEquals(new BigDecimal("2"), update.getValue().getUnqualifiedQuantity());
        assertEquals(BusinessStatus.Quality.FAIL, update.getValue().getInspectionResult());
        assertNotNull(update.getValue().getInspectionTime());
    }

    @Test
    void submitTreatsUnqualifiedRateAtLimitAsPass() {
        when(orderMapper.selectOne(any())).thenReturn(existingOrder());
        when(orderMapper.updateById(any(QmsInspectionOrder.class))).thenReturn(1);

        assertTrue(service.submitInspectionOrder(1L, "FAIL", null, 9L,
                new BigDecimal("1000"), new BigDecimal("995"), new BigDecimal("5")));

        ArgumentCaptor<QmsInspectionOrder> update = ArgumentCaptor.forClass(QmsInspectionOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Quality.PASS, update.getValue().getInspectionResult());
    }

    @Test
    void submitTreatsUnqualifiedRateAboveLimitAsFail() {
        when(orderMapper.selectOne(any())).thenReturn(existingOrder());
        when(orderMapper.updateById(any(QmsInspectionOrder.class))).thenReturn(1);

        assertTrue(service.submitInspectionOrder(1L, "PASS", null, 9L,
                new BigDecimal("1000"), new BigDecimal("994"), new BigDecimal("6")));

        ArgumentCaptor<QmsInspectionOrder> update = ArgumentCaptor.forClass(QmsInspectionOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Quality.FAIL, update.getValue().getInspectionResult());
    }

    @Test
    void submitRejectsInconsistentInspectionQuantities() {
        when(orderMapper.selectOne(any())).thenReturn(existingOrder());

        assertFalse(service.submitInspectionOrder(1L, "PASS", null, 9L,
                BigDecimal.TEN, new BigDecimal("9"), BigDecimal.ZERO));
        assertFalse(service.submitInspectionOrder(1L, "PASS", null, 9L,
                BigDecimal.TEN, new BigDecimal("9"), new BigDecimal("2")));

        verify(orderMapper, never()).updateById(any(QmsInspectionOrder.class));
    }

    @Test
    void submitRejectsSampleQuantityAboveLinkedReportQuantity() {
        QmsInspectionOrder order = existingOrder();
        order.setReportId(3L);
        when(orderMapper.selectOne(any())).thenReturn(order);
        MfgWorkReportMapper reportMapper = (MfgWorkReportMapper) ReflectionTestUtils.getField(service, "mfgWorkReportMapper");
        MfgWorkReport report = new MfgWorkReport();
        report.setQualifiedQuantity(new BigDecimal("8"));
        report.setDefectQuantity(new BigDecimal("2"));
        when(reportMapper.selectOne(any())).thenReturn(report);

        assertFalse(service.submitInspectionOrder(1L, "PASS", null, 9L,
                new BigDecimal("11"), new BigDecimal("11"), BigDecimal.ZERO));
        verify(orderMapper, never()).updateById(any(QmsInspectionOrder.class));
    }

    private QmsInspectionOrder existingOrder() {
        QmsInspectionOrder order = new QmsInspectionOrder();
        order.setInspectionId(1L);
        order.setStatus(BusinessStatus.Document.DRAFT);
        return order;
    }
}
