package com.neuedu.ems.service.impl.wms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.mfg.MfgReportMaterial;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.entity.wms.WmsOutboundDetail;
import com.neuedu.ems.entity.wms.WmsOutboundOrder;
import com.neuedu.ems.entity.wms.WmsReturnDetail;
import com.neuedu.ems.entity.wms.WmsReturnOrder;
import com.neuedu.ems.entity.wms.WmsStockMovement;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.mfg.MfgReportMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundDetailMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundOrderMapper;
import com.neuedu.ems.mapper.wms.WmsReturnDetailMapper;
import com.neuedu.ems.mapper.wms.WmsReturnOrderMapper;
import com.neuedu.ems.mapper.wms.WmsStockMovementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

class WmsReturnOrderServiceImplTest {

    private WmsReturnOrderServiceImpl service;
    private WmsReturnOrderMapper orderMapper;
    private WmsReturnDetailMapper detailMapper;
    private MfgReportMaterialMapper reportMaterialMapper;
    private MfgWorkOrderMapper workOrderMapper;
    private WmsOutboundDetailMapper outboundDetailMapper;
    private WmsOutboundOrderMapper outboundOrderMapper;
    private WmsInventoryMapper inventoryMapper;
    private WmsStockMovementMapper movementMapper;
    private WmsResponsibilityNotificationService notificationService;

    @BeforeEach
    void setUp() {
        service = new WmsReturnOrderServiceImpl();
        orderMapper = mock(WmsReturnOrderMapper.class);
        detailMapper = mock(WmsReturnDetailMapper.class);
        reportMaterialMapper = mock(MfgReportMaterialMapper.class);
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        outboundDetailMapper = mock(WmsOutboundDetailMapper.class);
        outboundOrderMapper = mock(WmsOutboundOrderMapper.class);
        inventoryMapper = mock(WmsInventoryMapper.class);
        movementMapper = mock(WmsStockMovementMapper.class);
        notificationService = mock(WmsResponsibilityNotificationService.class);
        SysStatusChangeLogMapper statusLogMapper = mock(SysStatusChangeLogMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "wmsReturnDetailMapper", detailMapper);
        ReflectionTestUtils.setField(service, "mfgReportMaterialMapper", reportMaterialMapper);
        ReflectionTestUtils.setField(service, "mfgWorkOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "wmsOutboundDetailMapper", outboundDetailMapper);
        ReflectionTestUtils.setField(service, "wmsOutboundOrderMapper", outboundOrderMapper);
        ReflectionTestUtils.setField(service, "wmsInventoryMapper", inventoryMapper);
        ReflectionTestUtils.setField(service, "wmsStockMovementMapper", movementMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "responsibilityNotificationService", notificationService);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setStatus(BusinessStatus.Production.RUNNING);
        workOrder.setDeletedFlag(0);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
    }

    @Test
    void saveReturnDerivesImmutableSourceFieldsAndJwtApplicant() {
        stubValidSource(BigDecimal.ZERO);
        when(orderMapper.insert(any(WmsReturnOrder.class))).thenAnswer(invocation -> {
            invocation.<WmsReturnOrder>getArgument(0).setReturnId(20L);
            return 1;
        });
        when(detailMapper.insert(any(WmsReturnDetail.class))).thenReturn(1);
        WmsReturnOrder order = order(null);
        order.setReturnId(null);
        WmsReturnDetail detail = returnDetail("2.0000");
        detail.setMaterialId(null);
        detail.setBatchId(null);
        detail.setLocationId(null);

        Long returnId = service.saveReturnOrderFull(order, List.of(detail), 9L);

        assertEquals(20L, returnId);
        assertEquals(9L, order.getApplicantId());
        assertEquals(BusinessStatus.Document.SUBMITTED, order.getAuditStatus());
        assertTrue(order.getReturnNo().startsWith("RT-WO3-"));
        assertEquals(11L, detail.getMaterialId());
        assertEquals(7L, detail.getBatchId());
        assertEquals(8L, detail.getLocationId());
        verify(notificationService).publishReturnPending(order, 9L);
    }

    @Test
    void forgedSourceFieldsAreRejectedBeforeInsert() {
        stubValidSource(BigDecimal.ZERO);
        WmsReturnDetail detail = returnDetail("2.0000");
        detail.setMaterialId(99L);

        assertThrows(BusinessException.class,
                () -> service.saveReturnOrderFull(order(null), List.of(detail), 9L));

        verify(orderMapper, never()).insert(any(WmsReturnOrder.class));
        verify(detailMapper, never()).insert(any(WmsReturnDetail.class));
    }

    @Test
    void confirmReturnUpdatesOrderBeforeInventoryAndWritesDeterministicMovement() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.SUBMITTED));
        when(detailMapper.selectList(any())).thenReturn(List.of(returnDetail("2.0000")));
        stubValidSource(BigDecimal.ZERO);
        when(inventoryMapper.selectOne(any())).thenReturn(inventory("5.0000"));
        when(orderMapper.updateById(any(WmsReturnOrder.class))).thenReturn(1);
        when(inventoryMapper.updateById(any(WmsInventory.class))).thenReturn(1);
        when(movementMapper.insert(any(WmsStockMovement.class))).thenReturn(1);

        assertTrue(service.confirmReturnOrder(1L, 9L, "剩余良品退料"));

        ArgumentCaptor<WmsInventory> inventoryUpdate = ArgumentCaptor.forClass(WmsInventory.class);
        verify(inventoryMapper).updateById(inventoryUpdate.capture());
        assertEquals(new BigDecimal("7.0000"), inventoryUpdate.getValue().getAvailableQuantity());
        ArgumentCaptor<WmsStockMovement> movement = ArgumentCaptor.forClass(WmsStockMovement.class);
        verify(movementMapper).insert(movement.capture());
        assertEquals("WMS-RETURN-1-6", movement.getValue().getMovementNo());
        assertEquals("IN", movement.getValue().getDirection());
    }

    @Test
    void cumulativeReturnAboveIssuedQuantityIsRejectedBeforeMutation() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.SUBMITTED));
        when(detailMapper.selectList(any())).thenReturn(List.of(returnDetail("2.0000")));
        stubValidSource(new BigDecimal("9.0000"));

        assertThrows(BusinessException.class, () -> service.confirmReturnOrder(1L, 9L, null));

        verify(orderMapper, never()).updateById(any(WmsReturnOrder.class));
        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
    }

    @Test
    void materialAlreadyConsumedByReportsCannotBeReturned() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.SUBMITTED));
        when(detailMapper.selectList(any())).thenReturn(List.of(returnDetail("2.0000")));
        stubValidSource(BigDecimal.ZERO);
        MfgReportMaterial consumed = new MfgReportMaterial();
        consumed.setUsedQuantity(new BigDecimal("9.0000"));
        consumed.setScrapQuantity(BigDecimal.ZERO);
        when(reportMaterialMapper.selectList(any())).thenReturn(List.of(consumed));

        assertThrows(BusinessException.class, () -> service.confirmReturnOrder(1L, 9L, null));

        verify(orderMapper, never()).updateById(any(WmsReturnOrder.class));
        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
    }

    @Test
    void returnOrderConflictStopsBeforeInventoryMutation() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.SUBMITTED));
        when(detailMapper.selectList(any())).thenReturn(List.of(returnDetail("2.0000")));
        stubValidSource(BigDecimal.ZERO);
        when(inventoryMapper.selectOne(any())).thenReturn(inventory("5.0000"));
        when(orderMapper.updateById(any(WmsReturnOrder.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.confirmReturnOrder(1L, 9L, null));

        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
    }

    @Test
    void inventoryConflictRaisesExceptionBeforeMovement() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.SUBMITTED));
        when(detailMapper.selectList(any())).thenReturn(List.of(returnDetail("2.0000")));
        stubValidSource(BigDecimal.ZERO);
        when(inventoryMapper.selectOne(any())).thenReturn(inventory("5.0000"));
        when(orderMapper.updateById(any(WmsReturnOrder.class))).thenReturn(1);
        when(inventoryMapper.updateById(any(WmsInventory.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.confirmReturnOrder(1L, 9L, null));

        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
    }

    @Test
    void repeatedConfirmIsIdempotentWhenMovementIsComplete() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.APPROVED));
        when(detailMapper.selectList(any())).thenReturn(List.of(returnDetail("2.0000")));
        when(movementMapper.selectCount(any())).thenReturn(1L);

        assertTrue(service.confirmReturnOrder(1L, 9L, null));

        verify(orderMapper, never()).updateById(any(WmsReturnOrder.class));
        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
    }

    @Test
    void withdrawSubmittedReturnClosesWithoutInventoryMovement() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.SUBMITTED));
        when(orderMapper.updateById(any(WmsReturnOrder.class))).thenReturn(1);

        service.withdrawReturnOrder(1L, 9L, null);

        ArgumentCaptor<WmsReturnOrder> update = ArgumentCaptor.forClass(WmsReturnOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.CLOSED, update.getValue().getAuditStatus());
        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
        verify(notificationService).cancelReturnPending(1L, 9L);
    }

    private void stubValidSource(BigDecimal alreadyReturned) {
        when(outboundDetailMapper.selectOne(any())).thenReturn(sourceDetail());
        when(outboundDetailMapper.selectList(any())).thenReturn(List.of(sourceDetail()));
        when(outboundOrderMapper.selectOne(any())).thenReturn(sourceOrder());
        when(detailMapper.sumActiveReturnedQuantity(4L, null)).thenReturn(alreadyReturned);
        when(detailMapper.sumActiveReturnedQuantity(4L, 1L)).thenReturn(alreadyReturned);
        when(detailMapper.sumActiveReturnedQuantityByBatch(3L, 11L, 7L, null)).thenReturn(alreadyReturned);
        when(detailMapper.sumActiveReturnedQuantityByBatch(3L, 11L, 7L, 1L)).thenReturn(alreadyReturned);
    }

    private WmsReturnOrder order(String status) {
        WmsReturnOrder order = new WmsReturnOrder();
        order.setReturnId(1L);
        order.setReturnType("GOOD");
        order.setWorkOrderId(3L);
        order.setApplicantId(9L);
        order.setAuditStatus(status);
        order.setVersion(2);
        return order;
    }

    private WmsReturnDetail returnDetail(String quantity) {
        WmsReturnDetail detail = new WmsReturnDetail();
        detail.setDetailId(6L);
        detail.setReturnId(1L);
        detail.setOutboundDetailId(4L);
        detail.setMaterialId(11L);
        detail.setBatchId(7L);
        detail.setLocationId(8L);
        detail.setReturnQuantity(new BigDecimal(quantity));
        detail.setReturnReason("生产剩余物料退回");
        detail.setDeletedFlag(0);
        return detail;
    }

    private WmsOutboundDetail sourceDetail() {
        WmsOutboundDetail detail = new WmsOutboundDetail();
        detail.setDetailId(4L);
        detail.setOutboundId(2L);
        detail.setMaterialId(11L);
        detail.setBatchId(7L);
        detail.setLocationId(8L);
        detail.setOutboundQuantity(new BigDecimal("10.0000"));
        detail.setDeletedFlag(0);
        return detail;
    }

    private WmsOutboundOrder sourceOrder() {
        WmsOutboundOrder order = new WmsOutboundOrder();
        order.setOutboundId(2L);
        order.setOutboundType("MATERIAL_ISSUE");
        order.setWarehouseId(2L);
        order.setWorkOrderId(3L);
        order.setAuditStatus(BusinessStatus.Document.APPROVED);
        order.setDeletedFlag(0);
        return order;
    }

    private WmsInventory inventory(String available) {
        WmsInventory inventory = new WmsInventory();
        inventory.setInventoryId(3L);
        inventory.setWarehouseId(2L);
        inventory.setLocationId(8L);
        inventory.setMaterialId(11L);
        inventory.setBatchId(7L);
        inventory.setAvailableQuantity(new BigDecimal(available));
        inventory.setVersion(2);
        inventory.setDeletedFlag(0);
        return inventory;
    }
}
