package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkOrderMaterial;
import com.neuedu.ems.entity.mfg.MfgLineProductCapability;
import com.neuedu.ems.entity.mdm.MdmBom;
import com.neuedu.ems.entity.mdm.MdmBomItem;
import com.neuedu.ems.entity.process.ProcessRoute;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgLineProductCapabilityMapper;
import com.neuedu.ems.mapper.mdm.MdmBomMapper;
import com.neuedu.ems.mapper.mdm.MdmBomItemMapper;
import com.neuedu.ems.mapper.process.ProcessRouteMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgWorkOrderServiceImplTest {

    private MfgWorkOrderServiceImpl service;
    private MfgWorkOrderMapper workOrderMapper;
    private MfgProductionPlanMapper planMapper;
    private MfgProductionOrderMapper orderMapper;
    private MdmBomMapper bomMapper;
    private MdmBomItemMapper bomItemMapper;
    private MfgWorkOrderMaterialMapper materialMapper;
    private WmsInventoryMapper inventoryMapper;
    private MfgLineProductCapabilityMapper capabilityMapper;
    private ProcessRouteMapper routeMapper;
    private MfgTaskMapper taskMapper;

    @BeforeEach
    void setUp() {
        service = new MfgWorkOrderServiceImpl();
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        orderMapper = mock(MfgProductionOrderMapper.class);
        bomMapper = mock(MdmBomMapper.class);
        bomItemMapper = mock(MdmBomItemMapper.class);
        materialMapper = mock(MfgWorkOrderMaterialMapper.class);
        inventoryMapper = mock(WmsInventoryMapper.class);
        capabilityMapper = mock(MfgLineProductCapabilityMapper.class);
        routeMapper = mock(ProcessRouteMapper.class);
        taskMapper = mock(MfgTaskMapper.class);
        SysStatusChangeLogMapper statusLogMapper = mock(SysStatusChangeLogMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "productionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "productionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "bomMapper", bomMapper);
        ReflectionTestUtils.setField(service, "bomItemMapper", bomItemMapper);
        ReflectionTestUtils.setField(service, "workOrderMaterialMapper", materialMapper);
        ReflectionTestUtils.setField(service, "inventoryMapper", inventoryMapper);
        ReflectionTestUtils.setField(service, "lineProductCapabilityMapper", capabilityMapper);
        ReflectionTestUtils.setField(service, "routeMapper", routeMapper);
        ReflectionTestUtils.setField(service, "mfgTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
        when(bomItemMapper.selectList(any())).thenReturn(List.of(bomItem()));
        when(inventoryMapper.selectList(any())).thenReturn(List.of());
        when(materialMapper.insert(any(MfgWorkOrderMaterial.class))).thenReturn(1);
        when(materialMapper.selectCount(any())).thenReturn(1L);
        when(materialMapper.selectList(any())).thenReturn(List.of(readyRequirement()));
    }

    @Test
    void generateCreatesOneDraftWorkOrderForFullPlanQuantity() {
        stubProductionDefinition();
        when(planMapper.selectOne(any())).thenReturn(plan());
        when(workOrderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectOne(any())).thenReturn(order());
        WmsInventory inventory = new WmsInventory();
        inventory.setMaterialId(21L);
        inventory.setAvailableQuantity(new BigDecimal("150"));
        when(inventoryMapper.selectList(any())).thenReturn(List.of(inventory));
        when(workOrderMapper.insert(any(MfgWorkOrder.class))).thenAnswer(invocation -> {
            invocation.<MfgWorkOrder>getArgument(0).setWorkOrderId(5L);
            return 1;
        });

        MfgWorkOrder result = service.generateWorkOrder(2L, request("100"), 9L);

        assertEquals(5L, result.getWorkOrderId());
        assertEquals(BusinessStatus.Document.DRAFT, result.getStatus());
        assertEquals(11L, result.getProductId());
        assertEquals(12L, result.getBomId());
        assertEquals(13L, result.getRouteId());
        assertEquals(new BigDecimal("100"), result.getPlannedQuantity());
        assertTrue(result.getWorkOrderNo().startsWith("WO-2-"));
        ArgumentCaptor<MfgWorkOrderMaterial> requirement = ArgumentCaptor.forClass(MfgWorkOrderMaterial.class);
        verify(materialMapper).insert(requirement.capture());
        assertEquals(5L, requirement.getValue().getWorkOrderId());
        assertEquals(21L, requirement.getValue().getMaterialId());
        assertEquals(new BigDecimal("206.0000"), requirement.getValue().getRequiredQuantity());
        assertEquals(new BigDecimal("56.0000"), requirement.getValue().getShortageQuantity());
        assertEquals("PARTIAL_READY", requirement.getValue().getStatus());
    }

    @Test
    void generateIgnoresClientBomRouteAndQuantityOverrides() {
        stubProductionDefinition();
        when(planMapper.selectOne(any())).thenReturn(plan());
        when(workOrderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(workOrderMapper.insert(any(MfgWorkOrder.class))).thenAnswer(invocation -> {
            invocation.<MfgWorkOrder>getArgument(0).setWorkOrderId(5L);
            return 1;
        });
        MfgWorkOrder forged = request("99");
        forged.setBomId(999L);
        forged.setRouteId(998L);

        MfgWorkOrder result = service.generateWorkOrder(2L, forged, 9L);

        assertEquals(12L, result.getBomId());
        assertEquals(13L, result.getRouteId());
        assertEquals(new BigDecimal("100"), result.getPlannedQuantity());
    }

    @Test
    void generateRejectsProductWithoutEffectiveReleasedMaterialList() {
        when(planMapper.selectOne(any())).thenReturn(plan());
        when(workOrderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(bomMapper.selectList(any())).thenReturn(List.of());

        assertThrows(BusinessException.class,
                () -> service.generateWorkOrder(2L, new MfgWorkOrder(), 9L));

        verify(workOrderMapper, never()).insert(any(MfgWorkOrder.class));
    }

    @Test
    void generateRejectsReleasedMaterialListWithoutValidItemsBeforeCreatingWorkOrder() {
        stubProductionDefinition();
        when(bomItemMapper.selectList(any())).thenReturn(List.of());
        when(planMapper.selectOne(any())).thenReturn(plan());
        when(workOrderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectOne(any())).thenReturn(order());

        assertThrows(BusinessException.class,
                () -> service.generateWorkOrder(2L, new MfgWorkOrder(), 9L));

        verify(workOrderMapper, never()).insert(any(MfgWorkOrder.class));
        verify(materialMapper, never()).insert(any(MfgWorkOrderMaterial.class));
    }

    @Test
    void detachedWorkOrderCreationIsRejected() {
        assertThrows(BusinessException.class, () -> service.saveWorkOrder(new MfgWorkOrder()));
        verify(workOrderMapper, never()).insert(any(MfgWorkOrder.class));
    }

    @Test
    void generateFallsBackToOnlyReleasedProductRouteWhenLineCapabilityIsMissing() {
        MdmBom bom = new MdmBom();
        bom.setBomId(12L);
        when(bomMapper.selectList(any())).thenReturn(List.of(bom));
        when(capabilityMapper.selectList(any())).thenReturn(List.of());
        ProcessRoute route = new ProcessRoute();
        route.setRouteId(13L);
        route.setProductId(11L);
        route.setStatus(BusinessStatus.Document.RELEASED);
        when(routeMapper.selectList(any())).thenReturn(List.of(route));
        when(planMapper.selectOne(any())).thenReturn(plan());
        when(workOrderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(workOrderMapper.insert(any(MfgWorkOrder.class))).thenAnswer(invocation -> {
            invocation.<MfgWorkOrder>getArgument(0).setWorkOrderId(5L);
            return 1;
        });

        MfgWorkOrder result = service.generateWorkOrder(2L, new MfgWorkOrder(), 9L);

        assertEquals(13L, result.getRouteId());
    }

    @Test
    void releaseMovesDraftWorkOrderToReleased() {
        MfgWorkOrder existing = request("100");
        existing.setWorkOrderId(5L);
        existing.setStatus(BusinessStatus.Document.DRAFT);
        existing.setVersion(4);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);
        when(planMapper.selectOne(any())).thenReturn(plan());
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);

        service.releaseWorkOrder(5L, 9L, null);

        org.mockito.ArgumentCaptor<MfgWorkOrder> update = org.mockito.ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.RELEASED, update.getValue().getStatus());
        assertEquals(4, update.getValue().getVersion());
    }

    @Test
    void waitStartFilterIncludesAllPreProductionStatuses() {
        QueryWrapper<MfgWorkOrder> query = ReflectionTestUtils.invokeMethod(
                service, "buildWorkOrderQuery", null, BusinessStatus.Production.WAIT_START, null, null);

        assertTrue(query.getSqlSegment().contains("status IN"));
        assertTrue(query.getParamNameValuePairs().containsValue(BusinessStatus.Document.DRAFT));
        assertTrue(query.getParamNameValuePairs().containsValue(BusinessStatus.Document.APPROVED));
        assertTrue(query.getParamNameValuePairs().containsValue(BusinessStatus.Document.RELEASED));
        assertTrue(query.getParamNameValuePairs().containsValue(BusinessStatus.Production.WAIT_START));
    }

    @Test
    void startMovesReleasedWorkOrderToRunningWhenTasksExist() {
        MfgWorkOrder existing = request("100");
        existing.setWorkOrderId(5L);
        existing.setStatus(BusinessStatus.Document.RELEASED);
        existing.setVersion(4);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);
        when(taskMapper.selectCount(any())).thenReturn(7L, 0L);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);

        service.startWorkOrder(5L, 9L);

        org.mockito.ArgumentCaptor<MfgWorkOrder> update = org.mockito.ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Production.RUNNING, update.getValue().getStatus());
        assertEquals(4, update.getValue().getVersion());
    }

    @Test
    void startRejectsWorkOrderWithoutTasks() {
        MfgWorkOrder existing = request("100");
        existing.setWorkOrderId(5L);
        existing.setStatus(BusinessStatus.Document.RELEASED);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);
        when(taskMapper.selectCount(any())).thenReturn(0L);

        assertThrows(BusinessException.class, () -> service.startWorkOrder(5L, 9L));

        verify(workOrderMapper, never()).updateById(any(MfgWorkOrder.class));
    }

    @Test
    void startRejectsWorkOrderWithAlreadyStartedTask() {
        MfgWorkOrder existing = request("100");
        existing.setWorkOrderId(5L);
        existing.setStatus(BusinessStatus.Document.RELEASED);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);
        when(taskMapper.selectCount(any())).thenReturn(7L, 1L);

        assertThrows(BusinessException.class, () -> service.startWorkOrder(5L, 9L));

        verify(workOrderMapper, never()).updateById(any(MfgWorkOrder.class));
    }

    @Test
    void startRejectsWorkOrderWhenMaterialIsNotReady() {
        MfgWorkOrder existing = request("100");
        existing.setWorkOrderId(5L);
        existing.setStatus(BusinessStatus.Document.RELEASED);
        MfgWorkOrderMaterial shortage = readyRequirement();
        shortage.setIssuedQuantity(new BigDecimal("10"));
        when(workOrderMapper.selectOne(any())).thenReturn(existing);
        when(materialMapper.selectList(any())).thenReturn(List.of(shortage));

        assertThrows(BusinessException.class, () -> service.startWorkOrder(5L, 9L));

        verify(workOrderMapper, never()).updateById(any(MfgWorkOrder.class));
    }

    @Test
    void supplementMaterialRequirementUsesBomLossAndExistingIssuedQuantity() {
        MfgWorkOrder workOrder = request("100");
        workOrder.setWorkOrderId(5L);
        workOrder.setStatus(BusinessStatus.Production.RUNNING);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        when(materialMapper.updateById(any(MfgWorkOrderMaterial.class))).thenReturn(1);

        service.addSupplementMaterialRequirements(5L, new BigDecimal("10"), 9L, "报废补产");

        ArgumentCaptor<MfgWorkOrderMaterial> update = ArgumentCaptor.forClass(MfgWorkOrderMaterial.class);
        verify(materialMapper).updateById(update.capture());
        assertEquals(new BigDecimal("120.6000"), update.getValue().getRequiredQuantity());
        assertEquals(new BigDecimal("20.6000"), update.getValue().getShortageQuantity());
        assertEquals("PARTIAL_READY", update.getValue().getStatus());
    }

    @Test
    void supplementMaterialRequirementRejectsNegativeBomLossRate() {
        MfgWorkOrder workOrder = request("100");
        workOrder.setWorkOrderId(5L);
        workOrder.setStatus(BusinessStatus.Production.RUNNING);
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        MdmBomItem invalid = bomItem();
        invalid.setLossRate(new BigDecimal("-0.01"));
        when(bomItemMapper.selectList(any())).thenReturn(List.of(invalid));

        assertThrows(BusinessException.class,
                () -> service.addSupplementMaterialRequirements(5L, BigDecimal.TEN, 9L, "返工补料"));

        verify(materialMapper, never()).updateById(any(MfgWorkOrderMaterial.class));
    }

    private MfgProductionPlan plan() {
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(2L);
        plan.setProductionOrderId(1L);
        plan.setLineId(8L);
        plan.setPlannedStartTime(LocalDateTime.now().plusHours(1));
        plan.setPlannedEndTime(LocalDateTime.now().plusHours(8));
        plan.setPlannedQuantity(new BigDecimal("100"));
        plan.setStatus(BusinessStatus.Document.RELEASED);
        return plan;
    }

    private MfgProductionOrder order() {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(1L);
        order.setProductId(11L);
        return order;
    }

    private MfgWorkOrder request(String quantity) {
        MfgWorkOrder request = new MfgWorkOrder();
        request.setPlanId(2L);
        request.setBomId(12L);
        request.setRouteId(13L);
        request.setPlannedQuantity(new BigDecimal(quantity));
        return request;
    }

    private MfgWorkOrderMaterial readyRequirement() {
        MfgWorkOrderMaterial requirement = new MfgWorkOrderMaterial();
        requirement.setRequirementId(31L);
        requirement.setWorkOrderId(5L);
        requirement.setMaterialId(21L);
        requirement.setRequiredQuantity(new BigDecimal("100"));
        requirement.setIssuedQuantity(new BigDecimal("100"));
        requirement.setStatus("READY");
        return requirement;
    }

    private MdmBomItem bomItem() {
        MdmBomItem item = new MdmBomItem();
        item.setBomItemId(31L);
        item.setBomId(12L);
        item.setMaterialId(21L);
        item.setQuantity(new BigDecimal("2"));
        item.setLossRate(new BigDecimal("0.03"));
        item.setSequenceNo(1);
        return item;
    }

    private void stubProductionDefinition() {
        MdmBom bom = new MdmBom();
        bom.setBomId(12L);
        bom.setProductId(11L);
        bom.setEffectiveDate(LocalDate.of(2026, 1, 1));
        bom.setStatus(BusinessStatus.Document.RELEASED);
        when(bomMapper.selectList(any())).thenReturn(List.of(bom));

        MfgLineProductCapability capability = new MfgLineProductCapability();
        capability.setCapabilityId(1L);
        capability.setLineId(8L);
        capability.setProductId(11L);
        capability.setRouteId(13L);
        capability.setPriorityLevel(1);
        when(capabilityMapper.selectList(any())).thenReturn(List.of(capability));

        ProcessRoute route = new ProcessRoute();
        route.setRouteId(13L);
        route.setProductId(11L);
        route.setStatus(BusinessStatus.Document.RELEASED);
        when(routeMapper.selectOne(any())).thenReturn(route);
    }
}
