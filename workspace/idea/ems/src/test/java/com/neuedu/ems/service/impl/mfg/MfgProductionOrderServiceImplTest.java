package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgProductionOrderServiceImplTest {

    private MfgProductionOrderServiceImpl service;
    private MfgProductionOrderMapper orderMapper;
    private SysStatusChangeLogMapper statusLogMapper;
    private SysOperationLogMapper operationLogMapper;

    @BeforeEach
    void setUp() {
        service = new MfgProductionOrderServiceImpl();
        orderMapper = mock(MfgProductionOrderMapper.class);
        statusLogMapper = mock(SysStatusChangeLogMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void releaseMovesDraftOrderToReleasedAndWritesAudit() {
        MfgProductionOrder existing = order(BusinessStatus.Document.DRAFT);
        when(orderMapper.selectOne(any())).thenReturn(existing);
        when(orderMapper.updateById(any(MfgProductionOrder.class))).thenReturn(1);

        service.releaseOrder(1L, 9L, "允许下达");

        ArgumentCaptor<MfgProductionOrder> update = ArgumentCaptor.forClass(MfgProductionOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.RELEASED, update.getValue().getStatus());
        assertEquals(2, update.getValue().getVersion());

        ArgumentCaptor<SysStatusChangeLog> statusLog = ArgumentCaptor.forClass(SysStatusChangeLog.class);
        verify(statusLogMapper).insert(statusLog.capture());
        assertEquals("RELEASE_PRODUCTION_ORDER", statusLog.getValue().getChangeReason());
        ArgumentCaptor<SysOperationLog> operationLog = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(operationLogMapper).insert(operationLog.capture());
        assertEquals(9L, operationLog.getValue().getUserId());
    }

    @Test
    void releaseRejectsOrderOutsideDraftState() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.RELEASED));

        assertThrows(BusinessException.class, () -> service.releaseOrder(1L, 9L, null));
        verify(orderMapper, never()).updateById(any(MfgProductionOrder.class));
    }

    private MfgProductionOrder order(String status) {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(1L);
        order.setStatus(status);
        order.setVersion(2);
        return order;
    }
}
