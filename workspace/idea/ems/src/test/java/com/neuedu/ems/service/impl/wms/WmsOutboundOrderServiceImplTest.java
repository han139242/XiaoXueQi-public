package com.neuedu.ems.service.impl.wms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.entity.wms.WmsMaterialBatch;
import com.neuedu.ems.entity.wms.WmsOutboundDetail;
import com.neuedu.ems.entity.wms.WmsOutboundOrder;
import com.neuedu.ems.entity.wms.WmsStockMovement;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.mapper.wms.WmsMaterialBatchMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundDetailMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundOrderMapper;
import com.neuedu.ems.mapper.wms.WmsStockMovementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsOutboundOrderServiceImplTest {

    private WmsOutboundOrderServiceImpl service;
    private WmsOutboundOrderMapper orderMapper;
    private WmsOutboundDetailMapper detailMapper;
    private WmsInventoryMapper inventoryMapper;
    private WmsMaterialBatchMapper batchMapper;
    private WmsStockMovementMapper movementMapper;
    private WmsResponsibilityNotificationService notificationService;

    @BeforeEach
    void setUp() {
        service = new WmsOutboundOrderServiceImpl();
        orderMapper = mock(WmsOutboundOrderMapper.class);
        detailMapper = mock(WmsOutboundDetailMapper.class);
        inventoryMapper = mock(WmsInventoryMapper.class);
        batchMapper = mock(WmsMaterialBatchMapper.class);
        movementMapper = mock(WmsStockMovementMapper.class);
        SysStatusChangeLogMapper statusLogMapper = mock(SysStatusChangeLogMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);
        notificationService = mock(WmsResponsibilityNotificationService.class);

        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "wmsOutboundDetailMapper", detailMapper);
        ReflectionTestUtils.setField(service, "wmsInventoryMapper", inventoryMapper);
        ReflectionTestUtils.setField(service, "wmsMaterialBatchMapper", batchMapper);
        ReflectionTestUtils.setField(service, "wmsStockMovementMapper", movementMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "responsibilityNotificationService", notificationService);

        when(orderMapper.selectOne(any())).thenReturn(order());
        when(batchMapper.selectOne(any())).thenReturn(batch(BusinessStatus.Quality.PASS, "AVAILABLE"));
        when(orderMapper.updateById(any(WmsOutboundOrder.class))).thenReturn(1);
        when(inventoryMapper.updateById(any(WmsInventory.class))).thenReturn(1);
        when(movementMapper.insert(any(WmsStockMovement.class))).thenReturn(1);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void createForcesSubmittedStatusAndClearsClientControlledFields() {
        WmsOutboundOrder request = order();
        request.setAuditStatus(BusinessStatus.Document.APPROVED);
        request.setOutboundTime(LocalDateTime.now());
        request.setDeletedFlag(1);
        when(orderMapper.insert(any(WmsOutboundOrder.class))).thenReturn(1);

        assertTrue(service.saveOutboundOrder(request));

        ArgumentCaptor<WmsOutboundOrder> saved = ArgumentCaptor.forClass(WmsOutboundOrder.class);
        verify(orderMapper).insert(saved.capture());
        assertNull(saved.getValue().getOutboundId());
        assertEquals(BusinessStatus.Document.SUBMITTED, saved.getValue().getAuditStatus());
        assertNull(saved.getValue().getOutboundTime());
        assertEquals(0, saved.getValue().getDeletedFlag());
        verify(notificationService).publishOutboundPending(saved.getValue(), saved.getValue().getCreatedBy());
    }

    @Test
    void fullCreateRequiresAtLeastOneDetail() {
        assertThrows(BusinessException.class, () -> service.saveOutboundOrderFull(order(), List.of()));

        verify(orderMapper, never()).insert(any(WmsOutboundOrder.class));
        verify(detailMapper, never()).insert(any(WmsOutboundDetail.class));
    }

    @Test
    void confirmOutboundAggregatesSameInventoryAndWritesOneDeductionWithDetailMovements() {
        when(detailMapper.selectList(any())).thenReturn(List.of(detail(4L, "6.0000"), detail(5L, "5.0000")));
        when(inventoryMapper.selectOne(any())).thenReturn(inventory("12.0000"));

        assertTrue(service.confirmOutboundOrder(1L, 9L, "确认出库"));

        ArgumentCaptor<WmsInventory> inventoryUpdate = ArgumentCaptor.forClass(WmsInventory.class);
        verify(inventoryMapper).updateById(inventoryUpdate.capture());
        assertEquals(new BigDecimal("1.0000"), inventoryUpdate.getValue().getAvailableQuantity());

        ArgumentCaptor<WmsStockMovement> movements = ArgumentCaptor.forClass(WmsStockMovement.class);
        verify(movementMapper, times(2)).insert(movements.capture());
        assertEquals(List.of("WMS-OUT-1-4", "WMS-OUT-1-5"),
                movements.getAllValues().stream().map(WmsStockMovement::getMovementNo).toList());
        verify(inventoryMapper).selectOne(any());
    }

    @Test
    void combinedQuantityAboveAvailableInventoryIsRejectedBeforeMutation() {
        when(detailMapper.selectList(any())).thenReturn(List.of(detail(4L, "6.0000"), detail(5L, "5.0000")));
        when(inventoryMapper.selectOne(any())).thenReturn(inventory("10.0000"));

        assertThrows(BusinessException.class, () -> service.confirmOutboundOrder(1L, 9L, null));

        verify(orderMapper, never()).updateById(any(WmsOutboundOrder.class));
        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
    }

    @Test
    void outboundOrderConflictStopsBeforeInventoryMutation() {
        when(detailMapper.selectList(any())).thenReturn(List.of(detail(4L, "6.0000")));
        when(inventoryMapper.selectOne(any())).thenReturn(inventory("10.0000"));
        when(orderMapper.updateById(any(WmsOutboundOrder.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.confirmOutboundOrder(1L, 9L, null));

        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
    }

    @Test
    void inventoryConflictRaisesExceptionForWholeTransactionRollback() {
        when(detailMapper.selectList(any())).thenReturn(List.of(detail(4L, "6.0000")));
        when(inventoryMapper.selectOne(any())).thenReturn(inventory("10.0000"));
        when(inventoryMapper.updateById(any(WmsInventory.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.confirmOutboundOrder(1L, 9L, null));

        verify(orderMapper).updateById(any(WmsOutboundOrder.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
    }

    @Test
    void frozenBatchIsRejectedBeforeAnyMutation() {
        when(detailMapper.selectList(any())).thenReturn(List.of(detail(4L, "6.0000")));
        when(batchMapper.selectOne(any())).thenReturn(batch("WAIT_INSPECTION", "FROZEN"));

        assertThrows(BusinessException.class, () -> service.confirmOutboundOrder(1L, 9L, null));

        verify(inventoryMapper, never()).selectOne(any());
        verify(orderMapper, never()).updateById(any(WmsOutboundOrder.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
    }

    @Test
    void deleteOrderConflictDoesNotSoftDeleteDetails() {
        when(orderMapper.updateById(any(WmsOutboundOrder.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.removeOutboundOrderById(1L));

        verify(detailMapper, never()).selectList(any());
        verify(detailMapper, never()).updateById(any(WmsOutboundDetail.class));
    }

    private WmsOutboundOrder order() {
        WmsOutboundOrder order = new WmsOutboundOrder();
        order.setOutboundId(1L);
        order.setWarehouseId(2L);
        order.setAuditStatus(BusinessStatus.Document.SUBMITTED);
        order.setVersion(3);
        return order;
    }

    private WmsOutboundDetail detail(Long detailId, String quantity) {
        WmsOutboundDetail detail = new WmsOutboundDetail();
        detail.setDetailId(detailId);
        detail.setOutboundId(1L);
        detail.setMaterialId(11L);
        detail.setBatchId(7L);
        detail.setLocationId(8L);
        detail.setOutboundQuantity(new BigDecimal(quantity));
        return detail;
    }

    private WmsInventory inventory(String availableQuantity) {
        WmsInventory inventory = new WmsInventory();
        inventory.setInventoryId(6L);
        inventory.setWarehouseId(2L);
        inventory.setLocationId(8L);
        inventory.setMaterialId(11L);
        inventory.setBatchId(7L);
        inventory.setAvailableQuantity(new BigDecimal(availableQuantity));
        inventory.setVersion(2);
        return inventory;
    }

    private WmsMaterialBatch batch(String qualityStatus, String status) {
        WmsMaterialBatch batch = new WmsMaterialBatch();
        batch.setBatchId(7L);
        batch.setMaterialId(11L);
        batch.setQualityStatus(qualityStatus);
        batch.setStatus(status);
        return batch;
    }
}
