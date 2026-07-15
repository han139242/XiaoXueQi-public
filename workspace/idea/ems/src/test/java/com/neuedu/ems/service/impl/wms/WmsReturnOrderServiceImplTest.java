package com.neuedu.ems.service.impl.wms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.entity.wms.WmsReturnOrder;
import com.neuedu.ems.entity.wms.WmsStockMovement;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.mapper.wms.WmsReturnDetailMapper;
import com.neuedu.ems.mapper.wms.WmsReturnOrderMapper;
import com.neuedu.ems.mapper.wms.WmsStockMovementMapper;
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

class WmsReturnOrderServiceImplTest {

    private WmsReturnOrderServiceImpl service;
    private WmsReturnOrderMapper orderMapper;
    private WmsInventoryMapper inventoryMapper;
    private WmsStockMovementMapper movementMapper;
    private WmsResponsibilityNotificationService notificationService;

    @BeforeEach
    void setUp() {
        service = new WmsReturnOrderServiceImpl();
        orderMapper = mock(WmsReturnOrderMapper.class);
        inventoryMapper = mock(WmsInventoryMapper.class);
        movementMapper = mock(WmsStockMovementMapper.class);
        notificationService = mock(WmsResponsibilityNotificationService.class);
        SysStatusChangeLogMapper statusLogMapper = mock(SysStatusChangeLogMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "wmsReturnDetailMapper", mock(WmsReturnDetailMapper.class));
        ReflectionTestUtils.setField(service, "wmsInventoryMapper", inventoryMapper);
        ReflectionTestUtils.setField(service, "wmsStockMovementMapper", movementMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "responsibilityNotificationService", notificationService);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void withdrawSubmittedReturnClosesWithoutInventoryMovement() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.SUBMITTED));
        when(orderMapper.updateById(any(WmsReturnOrder.class))).thenReturn(1);

        service.withdrawReturnOrder(1L, 9L, "领料需求调整");

        ArgumentCaptor<WmsReturnOrder> update = ArgumentCaptor.forClass(WmsReturnOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.CLOSED, update.getValue().getAuditStatus());
        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
        verify(notificationService).cancelReturnPending(1L, 9L);
    }

    @Test
    void withdrawApprovedReturnIsRejectedBeforeMutation() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.APPROVED));

        assertThrows(BusinessException.class,
                () -> service.withdrawReturnOrder(1L, 9L, "错误撤回"));

        verify(orderMapper, never()).updateById(any(WmsReturnOrder.class));
    }

    private WmsReturnOrder order(String status) {
        WmsReturnOrder order = new WmsReturnOrder();
        order.setReturnId(1L);
        order.setAuditStatus(status);
        order.setVersion(2);
        return order;
    }
}
