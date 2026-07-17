package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkOrderMaterial;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMaterialMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.trace.TraceMaterialUsageMapper;
import com.neuedu.ems.mapper.trace.TraceProductLotMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundOrderMapper;
import com.neuedu.ems.mapper.wms.WmsReturnOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgProductionOrderServiceImplTest {

    private MfgProductionOrderServiceImpl service;
    private MfgProductionOrderMapper orderMapper;
    private SysStatusChangeLogMapper statusLogMapper;
    private SysOperationLogMapper operationLogMapper;
    private MfgProductionPlanMapper planMapper;
    private MfgWorkOrderMapper workOrderMapper;
    private MfgWorkOrderMaterialMapper materialMapper;
    private MfgDispatchOrderMapper dispatchMapper;
    private MfgTaskMapper taskMapper;
    private MfgWorkReportMapper workReportMapper;
    private WmsOutboundOrderMapper outboundOrderMapper;
    private WmsReturnOrderMapper returnOrderMapper;
    private QmsInspectionOrderMapper inspectionOrderMapper;
    private TraceProductLotMapper productLotMapper;
    private TraceMaterialUsageMapper materialUsageMapper;
    private MfgResponsibilityNotificationService responsibilityNotificationService;

    @BeforeEach
    void setUp() {
        service = new MfgProductionOrderServiceImpl();
        orderMapper = mock(MfgProductionOrderMapper.class);
        statusLogMapper = mock(SysStatusChangeLogMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        materialMapper = mock(MfgWorkOrderMaterialMapper.class);
        dispatchMapper = mock(MfgDispatchOrderMapper.class);
        taskMapper = mock(MfgTaskMapper.class);
        workReportMapper = mock(MfgWorkReportMapper.class);
        outboundOrderMapper = mock(WmsOutboundOrderMapper.class);
        returnOrderMapper = mock(WmsReturnOrderMapper.class);
        inspectionOrderMapper = mock(QmsInspectionOrderMapper.class);
        productLotMapper = mock(TraceProductLotMapper.class);
        materialUsageMapper = mock(TraceMaterialUsageMapper.class);
        responsibilityNotificationService = mock(MfgResponsibilityNotificationService.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "productionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "workOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "workOrderMaterialMapper", materialMapper);
        ReflectionTestUtils.setField(service, "dispatchOrderMapper", dispatchMapper);
        ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "workReportMapper", workReportMapper);
        ReflectionTestUtils.setField(service, "outboundOrderMapper", outboundOrderMapper);
        ReflectionTestUtils.setField(service, "returnOrderMapper", returnOrderMapper);
        ReflectionTestUtils.setField(service, "inspectionOrderMapper", inspectionOrderMapper);
        ReflectionTestUtils.setField(service, "productLotMapper", productLotMapper);
        ReflectionTestUtils.setField(service, "materialUsageMapper", materialUsageMapper);
        ReflectionTestUtils.setField(service, "responsibilityNotificationService", responsibilityNotificationService);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
        when(dispatchMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(workReportMapper.selectCount(any())).thenReturn(0L);
        when(outboundOrderMapper.selectCount(any())).thenReturn(0L);
        when(returnOrderMapper.selectCount(any())).thenReturn(0L);
        when(inspectionOrderMapper.selectCount(any())).thenReturn(0L);
        when(productLotMapper.selectCount(any())).thenReturn(0L);
        when(materialUsageMapper.selectCount(any())).thenReturn(0L);
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

    @Test
    void withdrawUnexecutedOrderChainClosesDownstreamAndRestoresEditableOrder() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.RELEASED));
        when(planMapper.selectList(any())).thenReturn(List.of(plan(BusinessStatus.Document.RELEASED)));
        when(workOrderMapper.selectList(any())).thenReturn(List.of(workOrder(BusinessStatus.Document.RELEASED)));
        when(materialMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.updateById(any(MfgProductionOrder.class))).thenReturn(1);
        when(planMapper.updateById(any(MfgProductionPlan.class))).thenReturn(1);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);

        service.withdrawUnexecutedOrderChain(1L, 9L, "调整产品配置");

        ArgumentCaptor<MfgProductionOrder> orderUpdate = ArgumentCaptor.forClass(MfgProductionOrder.class);
        verify(orderMapper).updateById(orderUpdate.capture());
        assertEquals(BusinessStatus.Document.DRAFT, orderUpdate.getValue().getStatus());
        assertEquals(null, orderUpdate.getValue().getDeletedFlag());

        ArgumentCaptor<MfgProductionPlan> planUpdate = ArgumentCaptor.forClass(MfgProductionPlan.class);
        verify(planMapper).updateById(planUpdate.capture());
        assertEquals(BusinessStatus.Document.CLOSED, planUpdate.getValue().getStatus());
        assertEquals(null, planUpdate.getValue().getDeletedFlag());

        ArgumentCaptor<MfgWorkOrder> workOrderUpdate = ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(workOrderUpdate.capture());
        assertEquals(BusinessStatus.Document.CLOSED, workOrderUpdate.getValue().getStatus());
        assertEquals(null, workOrderUpdate.getValue().getDeletedFlag());
        verify(statusLogMapper, times(3)).insert(any(SysStatusChangeLog.class));
        verify(operationLogMapper, times(3)).insert(any(SysOperationLog.class));
    }

    @Test
    void withdrawUnexecutedOrderChainClosesWaitStartTaskAndRejectsDispatch() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.RELEASED));
        when(planMapper.selectList(any())).thenReturn(List.of(plan(BusinessStatus.Document.RELEASED)));
        when(workOrderMapper.selectList(any())).thenReturn(List.of(workOrder(BusinessStatus.Document.RELEASED)));
        MfgDispatchOrder dispatch = dispatch(BusinessStatus.Document.RELEASED);
        MfgTask task = task(BusinessStatus.Production.WAIT_START);
        when(dispatchMapper.selectList(any())).thenReturn(List.of(dispatch));
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(materialMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.updateById(any(MfgTask.class))).thenReturn(1);
        when(dispatchMapper.updateById(any(MfgDispatchOrder.class))).thenReturn(1);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);
        when(planMapper.updateById(any(MfgProductionPlan.class))).thenReturn(1);
        when(orderMapper.updateById(any(MfgProductionOrder.class))).thenReturn(1);

        service.withdrawUnexecutedOrderChain(1L, 9L, "调整生产安排");

        ArgumentCaptor<MfgTask> taskUpdate = ArgumentCaptor.forClass(MfgTask.class);
        verify(taskMapper).updateById(taskUpdate.capture());
        assertEquals(BusinessStatus.Document.CLOSED, taskUpdate.getValue().getStatus());
        assertEquals(null, taskUpdate.getValue().getDeletedFlag());

        ArgumentCaptor<MfgDispatchOrder> dispatchUpdate = ArgumentCaptor.forClass(MfgDispatchOrder.class);
        verify(dispatchMapper).updateById(dispatchUpdate.capture());
        assertEquals(BusinessStatus.Document.REJECTED, dispatchUpdate.getValue().getStatus());
        assertEquals(null, dispatchUpdate.getValue().getDeletedFlag());
        verify(responsibilityNotificationService).cancelOpenTaskNotifications(5L, 9L);
    }

    @Test
    void withdrawUnexecutedOrderChainRejectsRunningTask() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.RELEASED));
        when(planMapper.selectList(any())).thenReturn(List.of(plan(BusinessStatus.Document.RELEASED)));
        when(workOrderMapper.selectList(any())).thenReturn(List.of(workOrder(BusinessStatus.Document.RELEASED)));
        when(taskMapper.selectList(any())).thenReturn(List.of(task(BusinessStatus.Production.RUNNING)));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.withdrawUnexecutedOrderChain(1L, 9L, "调整生产安排"));

        assertEquals("无法撤回：生产任务已经开工或产生完成/不良数量", exception.getMessage());
        verify(orderMapper, never()).updateById(any(MfgProductionOrder.class));
        verify(taskMapper, never()).updateById(any(MfgTask.class));
    }

    @Test
    void withdrawUnexecutedOrderChainRejectsExistingWorkReport() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.RELEASED));
        when(planMapper.selectList(any())).thenReturn(List.of(plan(BusinessStatus.Document.RELEASED)));
        when(workOrderMapper.selectList(any())).thenReturn(List.of(workOrder(BusinessStatus.Document.RELEASED)));
        when(taskMapper.selectList(any())).thenReturn(List.of(task(BusinessStatus.Production.WAIT_START)));
        when(workReportMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class,
                () -> service.withdrawUnexecutedOrderChain(1L, 9L, "调整生产安排"));

        verify(orderMapper, never()).updateById(any(MfgProductionOrder.class));
        verify(taskMapper, never()).updateById(any(MfgTask.class));
    }

    @Test
    void withdrawUnexecutedOrderChainKeepsUnusedMaterialRequirement() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.RELEASED));
        when(planMapper.selectList(any())).thenReturn(List.of(plan(BusinessStatus.Document.RELEASED)));
        when(workOrderMapper.selectList(any())).thenReturn(List.of(workOrder(BusinessStatus.Document.RELEASED)));
        MfgWorkOrderMaterial requirement = new MfgWorkOrderMaterial();
        requirement.setRequirementId(4L);
        requirement.setIssuedQuantity(BigDecimal.ZERO);
        when(materialMapper.selectList(any())).thenReturn(List.of(requirement));
        when(orderMapper.updateById(any(MfgProductionOrder.class))).thenReturn(1);
        when(planMapper.updateById(any(MfgProductionPlan.class))).thenReturn(1);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);

        service.withdrawUnexecutedOrderChain(1L, 9L, "调整生产安排");

        verify(materialMapper, never()).updateById(any(MfgWorkOrderMaterial.class));
    }

    @Test
    void withdrawUnexecutedOrderChainRejectsIssuedMaterialRequirement() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.RELEASED));
        when(planMapper.selectList(any())).thenReturn(List.of(plan(BusinessStatus.Document.RELEASED)));
        when(workOrderMapper.selectList(any())).thenReturn(List.of(workOrder(BusinessStatus.Document.RELEASED)));
        MfgWorkOrderMaterial requirement = new MfgWorkOrderMaterial();
        requirement.setRequirementId(4L);
        requirement.setIssuedQuantity(BigDecimal.ONE);
        when(materialMapper.selectList(any())).thenReturn(List.of(requirement));

        assertThrows(BusinessException.class,
                () -> service.withdrawUnexecutedOrderChain(1L, 9L, "调整生产安排"));

        verify(materialMapper, never()).updateById(any(MfgWorkOrderMaterial.class));
        verify(orderMapper, never()).updateById(any(MfgProductionOrder.class));
        verify(planMapper, never()).updateById(any(MfgProductionPlan.class));
        verify(workOrderMapper, never()).updateById(any(MfgWorkOrder.class));
    }

    @Test
    void withdrawUnexecutedOrderChainUsesStandardAuditWhenReasonIsBlank() {
        when(orderMapper.selectOne(any())).thenReturn(order(BusinessStatus.Document.RELEASED));
        when(planMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.updateById(any(MfgProductionOrder.class))).thenReturn(1);

        service.withdrawUnexecutedOrderChain(1L, 9L, " ");

        ArgumentCaptor<SysOperationLog> operationLog = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(operationLogMapper).insert(operationLog.capture());
        assertEquals("用户主动撤回，未填写补充原因", operationLog.getValue().getRemark());
    }

    @Test
    void withdrawPlanClosesUnexecutedWorkOrdersAndRestoresPlanDraft() {
        when(planMapper.selectOne(any())).thenReturn(plan(BusinessStatus.Document.RELEASED));
        when(workOrderMapper.selectList(any())).thenReturn(List.of());
        when(planMapper.updateById(any(MfgProductionPlan.class))).thenReturn(1);

        service.withdrawUnexecutedPlanChain(2L, 9L, "调整计划");

        ArgumentCaptor<MfgProductionPlan> update = ArgumentCaptor.forClass(MfgProductionPlan.class);
        verify(planMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.DRAFT, update.getValue().getStatus());
        assertEquals(null, update.getValue().getDeletedFlag());
    }

    @Test
    void withdrawWorkOrderRestoresDraftWhenExecutionHasNotStarted() {
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder(BusinessStatus.Document.RELEASED));
        when(materialMapper.selectList(any())).thenReturn(List.of());
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);

        service.withdrawUnexecutedWorkOrderChain(3L, 9L, "调整工单");

        ArgumentCaptor<MfgWorkOrder> update = ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.DRAFT, update.getValue().getStatus());
        assertEquals(null, update.getValue().getDeletedFlag());
        ArgumentCaptor<SysOperationLog> operationLog = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(operationLogMapper).insert(operationLog.capture());
        assertEquals("WITHDRAW_WORK_ORDER", operationLog.getValue().getOperationType());
    }

    private MfgProductionOrder order(String status) {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(1L);
        order.setCustomerId(10L);
        order.setProductId(11L);
        order.setOrderQuantity(new BigDecimal("100"));
        order.setDeliveryDate(LocalDate.now().plusDays(7));
        order.setSourceType("MANUAL");
        order.setStatus(status);
        order.setVersion(2);
        return order;
    }

    private MfgProductionPlan plan(String status) {
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(2L);
        plan.setProductionOrderId(1L);
        plan.setStatus(status);
        plan.setVersion(3);
        return plan;
    }

    private MfgWorkOrder workOrder(String status) {
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(3L);
        workOrder.setPlanId(2L);
        workOrder.setStatus(status);
        workOrder.setCompletedQuantity(BigDecimal.ZERO);
        workOrder.setVersion(4);
        return workOrder;
    }

    private MfgDispatchOrder dispatch(String status) {
        MfgDispatchOrder dispatch = new MfgDispatchOrder();
        dispatch.setDispatchId(4L);
        dispatch.setWorkOrderId(3L);
        dispatch.setStatus(status);
        dispatch.setVersion(5);
        return dispatch;
    }

    private MfgTask task(String status) {
        MfgTask task = new MfgTask();
        task.setTaskId(5L);
        task.setDispatchId(4L);
        task.setWorkOrderId(3L);
        task.setStatus(status);
        task.setCompletedQuantity(BigDecimal.ZERO);
        task.setDefectQuantity(BigDecimal.ZERO);
        task.setVersion(6);
        return task;
    }
}
