package com.neuedu.ems.service.impl.wms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mdm.MdmProduct;
import com.neuedu.ems.entity.trace.TraceProductLot;
import com.neuedu.ems.entity.wms.WmsInboundDetail;
import com.neuedu.ems.entity.wms.WmsInboundOrder;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.entity.wms.WmsMaterialBatch;
import com.neuedu.ems.entity.wms.WmsStockMovement;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.mdm.MdmProductMapper;
import com.neuedu.ems.mapper.trace.TraceProductLotMapper;
import com.neuedu.ems.mapper.wms.WmsInboundDetailMapper;
import com.neuedu.ems.mapper.wms.WmsInboundOrderMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.mapper.wms.WmsMaterialBatchMapper;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsInboundOrderServiceImplTest {

    private WmsInboundOrderServiceImpl service;
    private WmsInboundOrderMapper orderMapper;
    private WmsInboundDetailMapper detailMapper;
    private WmsInventoryMapper inventoryMapper;
    private WmsStockMovementMapper movementMapper;
    private WmsMaterialBatchMapper batchMapper;
    private WmsResponsibilityNotificationService notificationService;
    private TraceProductLotMapper productLotMapper;
    private MdmProductMapper productMapper;

    @BeforeEach
    void setUp() {
        service = new WmsInboundOrderServiceImpl();
        orderMapper = mock(WmsInboundOrderMapper.class);
        detailMapper = mock(WmsInboundDetailMapper.class);
        inventoryMapper = mock(WmsInventoryMapper.class);
        movementMapper = mock(WmsStockMovementMapper.class);
        batchMapper = mock(WmsMaterialBatchMapper.class);
        productLotMapper = mock(TraceProductLotMapper.class);
        productMapper = mock(MdmProductMapper.class);
        SysStatusChangeLogMapper statusLogMapper = mock(SysStatusChangeLogMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);

        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "wmsInboundDetailMapper", detailMapper);
        ReflectionTestUtils.setField(service, "wmsInventoryMapper", inventoryMapper);
        ReflectionTestUtils.setField(service, "wmsStockMovementMapper", movementMapper);
        ReflectionTestUtils.setField(service, "wmsMaterialBatchMapper", batchMapper);
        ReflectionTestUtils.setField(service, "traceProductLotMapper", productLotMapper);
        ReflectionTestUtils.setField(service, "mdmProductMapper", productMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        notificationService = mock(WmsResponsibilityNotificationService.class);
        ReflectionTestUtils.setField(service, "responsibilityNotificationService", notificationService);

        when(detailMapper.selectList(any())).thenReturn(List.of(detail()));
        when(inventoryMapper.insert(any(WmsInventory.class))).thenReturn(1);
        when(movementMapper.insert(any(WmsStockMovement.class))).thenReturn(1);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void confirmInboundAcceptsReleasedBatchAndWritesInventoryMovementAndAudit() {
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(batchMapper.selectOne(any())).thenReturn(batch(BusinessStatus.Quality.PASS, "AVAILABLE"));
        when(inventoryMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.updateById(any(WmsInboundOrder.class))).thenReturn(1);

        assertTrue(service.confirmInboundOrder(1L, 9L, "确认入库"));

        ArgumentCaptor<WmsInventory> inventory = ArgumentCaptor.forClass(WmsInventory.class);
        verify(inventoryMapper).insert(inventory.capture());
        assertEquals(new BigDecimal("12.0000"), inventory.getValue().getAvailableQuantity());

        ArgumentCaptor<WmsStockMovement> movement = ArgumentCaptor.forClass(WmsStockMovement.class);
        verify(movementMapper).insert(movement.capture());
        assertEquals("INBOUND", movement.getValue().getMovementType());
        assertEquals("IN", movement.getValue().getDirection());
        assertEquals(9L, movement.getValue().getCreatedBy());

        ArgumentCaptor<WmsInboundOrder> update = ArgumentCaptor.forClass(WmsInboundOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.APPROVED, update.getValue().getAuditStatus());
        assertEquals(3, update.getValue().getVersion());
    }

    @Test
    void concessionBatchCanEnterAvailableInventory() {
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(batchMapper.selectOne(any())).thenReturn(batch(BusinessStatus.Quality.CONCESSION, "AVAILABLE"));
        when(inventoryMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.updateById(any(WmsInboundOrder.class))).thenReturn(1);

        assertTrue(service.confirmInboundOrder(1L, 9L, null));
        verify(inventoryMapper).insert(any(WmsInventory.class));
    }

    @Test
    void pendingInspectionBatchIsRejectedBeforeAnyInventoryMutation() {
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(batchMapper.selectOne(any())).thenReturn(batch("WAIT_INSPECTION", "FROZEN"));

        assertThrows(BusinessException.class, () -> service.confirmInboundOrder(1L, 9L, null));

        verify(inventoryMapper, never()).insert(any(WmsInventory.class));
        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
        verify(orderMapper, never()).updateById(any(WmsInboundOrder.class));
    }

    @Test
    void withdrawSubmittedInboundClosesWithoutInventoryMovement() {
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(orderMapper.updateById(any(WmsInboundOrder.class))).thenReturn(1);

        service.withdrawInboundOrder(1L, 9L, null);

        ArgumentCaptor<WmsInboundOrder> update = ArgumentCaptor.forClass(WmsInboundOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.CLOSED, update.getValue().getAuditStatus());
        verify(inventoryMapper, never()).insert(any(WmsInventory.class));
        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
        verify(notificationService).cancelInboundPending(1L, 9L);
    }

    @Test
    void inboundOrderOptimisticConflictRaisesExceptionForTransactionRollback() {
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(batchMapper.selectOne(any())).thenReturn(batch(BusinessStatus.Quality.PASS, "AVAILABLE"));
        when(inventoryMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.updateById(any(WmsInboundOrder.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.confirmInboundOrder(1L, 9L, null));
        verify(inventoryMapper).insert(any(WmsInventory.class));
        verify(movementMapper).insert(any(WmsStockMovement.class));
    }

    @Test
    void generateProductionInboundDerivesFinishedMaterialBatchAndRemainingQuantity() {
        TraceProductLot lot = productLot("20", BusinessStatus.Quality.PASS);
        MdmProduct product = product();
        when(productLotMapper.selectOne(any())).thenReturn(lot);
        when(orderMapper.selectList(any())).thenReturn(List.of());
        when(productMapper.selectOne(any())).thenReturn(product);
        when(detailMapper.selectList(any())).thenReturn(List.of());
        when(batchMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            WmsMaterialBatch batch = invocation.getArgument(0);
            batch.setBatchId(7L);
            return 1;
        }).when(batchMapper).insert(any(WmsMaterialBatch.class));
        doAnswer(invocation -> {
            WmsInboundOrder inbound = invocation.getArgument(0);
            inbound.setInboundId(1L);
            return 1;
        }).when(orderMapper).insert(any(WmsInboundOrder.class));
        when(detailMapper.insert(any(WmsInboundDetail.class))).thenReturn(1);

        WmsInboundOrder result = service.generateProductionInbound(31L, 2L, 8L, 9L, null);

        assertEquals(1L, result.getInboundId());
        assertEquals("PRODUCTION", result.getInboundType());
        assertEquals("PRODUCT_LOT", result.getSourceOrderType());
        ArgumentCaptor<WmsMaterialBatch> batch = ArgumentCaptor.forClass(WmsMaterialBatch.class);
        verify(batchMapper).insert(batch.capture());
        assertEquals(31L, batch.getValue().getSourceProductLotId());
        assertEquals(11L, batch.getValue().getMaterialId());
        ArgumentCaptor<WmsInboundDetail> detail = ArgumentCaptor.forClass(WmsInboundDetail.class);
        verify(detailMapper).insert(detail.capture());
        assertEquals(new BigDecimal("20"), detail.getValue().getInboundQuantity());
        assertEquals(8L, detail.getValue().getLocationId());
    }

    @Test
    void generateProductionInboundRejectsUnreleasedProductLot() {
        when(productLotMapper.selectOne(any())).thenReturn(productLot("20", BusinessStatus.Quality.FAIL));

        assertThrows(BusinessException.class,
                () -> service.generateProductionInbound(31L, 2L, 8L, 9L, null));

        verify(orderMapper, never()).insert(any(WmsInboundOrder.class));
        verify(detailMapper, never()).insert(any(WmsInboundDetail.class));
        verify(batchMapper, never()).insert(any(WmsMaterialBatch.class));
    }

    @Test
    void repeatedProductionInboundGenerationReturnsExistingSubmittedOrder() {
        WmsInboundOrder existing = order();
        existing.setInboundType("PRODUCTION");
        existing.setSourceOrderType("PRODUCT_LOT");
        existing.setSourceOrderId(31L);
        when(productLotMapper.selectOne(any())).thenReturn(productLot("20", BusinessStatus.Quality.PASS));
        when(orderMapper.selectList(any())).thenReturn(List.of(existing));

        WmsInboundOrder result = service.generateProductionInbound(31L, 2L, 8L, 9L, null);

        assertEquals(existing, result);
        verify(orderMapper, never()).insert(any(WmsInboundOrder.class));
        verify(detailMapper, never()).insert(any(WmsInboundDetail.class));
        verify(batchMapper, never()).insert(any(WmsMaterialBatch.class));
    }

    @Test
    void confirmProductionInboundRejectsCumulativeQuantityAboveReleasedQuantity() {
        WmsInboundOrder order = order();
        order.setInboundType("PRODUCTION");
        order.setSourceOrderType("PRODUCT_LOT");
        order.setSourceOrderId(31L);
        WmsMaterialBatch batch = batch(BusinessStatus.Quality.PASS, "AVAILABLE");
        batch.setSourceProductLotId(31L);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(detailMapper.selectList(any())).thenReturn(List.of(detail()), List.of());
        when(batchMapper.selectOne(any())).thenReturn(batch);
        when(productLotMapper.selectOne(any())).thenReturn(productLot("10", BusinessStatus.Quality.PASS));
        when(productMapper.selectOne(any())).thenReturn(product());

        assertThrows(BusinessException.class, () -> service.confirmInboundOrder(1L, 9L, null));

        verify(inventoryMapper, never()).insert(any(WmsInventory.class));
        verify(inventoryMapper, never()).updateById(any(WmsInventory.class));
        verify(movementMapper, never()).insert(any(WmsStockMovement.class));
        verify(orderMapper, never()).updateById(any(WmsInboundOrder.class));
    }

    @Test
    void confirmProductionInboundMarksFullyStoredProductLot() {
        WmsInboundOrder order = order();
        order.setInboundType("PRODUCTION");
        order.setSourceOrderType("PRODUCT_LOT");
        order.setSourceOrderId(31L);
        WmsMaterialBatch batch = batch(BusinessStatus.Quality.PASS, "AVAILABLE");
        batch.setSourceProductLotId(31L);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(detailMapper.selectList(any())).thenReturn(List.of(detail()), List.of());
        when(batchMapper.selectOne(any())).thenReturn(batch);
        when(productLotMapper.selectOne(any())).thenReturn(productLot("12.0000", BusinessStatus.Quality.PASS));
        when(productMapper.selectOne(any())).thenReturn(product());
        when(inventoryMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.updateById(any(WmsInboundOrder.class))).thenReturn(1);
        when(productLotMapper.updateById(any(TraceProductLot.class))).thenReturn(1);

        assertTrue(service.confirmInboundOrder(1L, 9L, null));

        ArgumentCaptor<TraceProductLot> update = ArgumentCaptor.forClass(TraceProductLot.class);
        verify(productLotMapper).updateById(update.capture());
        assertEquals("STORED", update.getValue().getStatus());
    }

    private WmsInboundOrder order() {
        WmsInboundOrder order = new WmsInboundOrder();
        order.setInboundId(1L);
        order.setWarehouseId(2L);
        order.setAuditStatus(BusinessStatus.Document.SUBMITTED);
        order.setVersion(3);
        return order;
    }

    private WmsInboundDetail detail() {
        WmsInboundDetail detail = new WmsInboundDetail();
        detail.setDetailId(4L);
        detail.setInboundId(1L);
        detail.setMaterialId(11L);
        detail.setBatchId(7L);
        detail.setLocationId(8L);
        detail.setInboundQuantity(new BigDecimal("12.0000"));
        return detail;
    }

    private WmsMaterialBatch batch(String qualityStatus, String status) {
        WmsMaterialBatch batch = new WmsMaterialBatch();
        batch.setBatchId(7L);
        batch.setMaterialId(11L);
        batch.setQualityStatus(qualityStatus);
        batch.setStatus(status);
        batch.setVersion(1);
        return batch;
    }

    private TraceProductLot productLot(String qualifiedQuantity, String status) {
        TraceProductLot lot = new TraceProductLot();
        lot.setProductLotId(31L);
        lot.setLotNo("PL-3-1");
        lot.setProductId(8L);
        lot.setQualifiedQuantity(new BigDecimal(qualifiedQuantity));
        lot.setStatus(status);
        lot.setVersion(2);
        return lot;
    }

    private MdmProduct product() {
        MdmProduct product = new MdmProduct();
        product.setProductId(8L);
        product.setMaterialId(11L);
        product.setStatus(BusinessStatus.Common.ENABLE);
        return product;
    }
}
