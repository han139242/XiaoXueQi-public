package com.neuedu.ems.service.impl.qms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionResultMapper;
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

class QmsInspectionOrderServiceImplTest {

    private QmsInspectionOrderServiceImpl service;
    private QmsInspectionOrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        service = new QmsInspectionOrderServiceImpl();
        orderMapper = mock(QmsInspectionOrderMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "qmsInspectionResultMapper", mock(QmsInspectionResultMapper.class));
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
}
