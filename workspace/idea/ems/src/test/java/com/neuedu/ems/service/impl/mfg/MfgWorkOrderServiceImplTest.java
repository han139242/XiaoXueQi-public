package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

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

    @BeforeEach
    void setUp() {
        service = new MfgWorkOrderServiceImpl();
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        planMapper = mock(MfgProductionPlanMapper.class);
        orderMapper = mock(MfgProductionOrderMapper.class);
        SysStatusChangeLogMapper statusLogMapper = mock(SysStatusChangeLogMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "productionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "productionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void generateCreatesOneDraftWorkOrderForFullPlanQuantity() {
        when(planMapper.selectOne(any())).thenReturn(plan());
        when(workOrderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectOne(any())).thenReturn(order());
        when(workOrderMapper.insert(any(MfgWorkOrder.class))).thenAnswer(invocation -> {
            invocation.<MfgWorkOrder>getArgument(0).setWorkOrderId(5L);
            return 1;
        });

        MfgWorkOrder result = service.generateWorkOrder(2L, request("100"), 9L);

        assertEquals(5L, result.getWorkOrderId());
        assertEquals(BusinessStatus.Document.DRAFT, result.getStatus());
        assertEquals(11L, result.getProductId());
        assertTrue(result.getWorkOrderNo().startsWith("WO-2-"));
    }

    @Test
    void generateRejectsPartialQuantityInSingleWorkOrderMode() {
        when(planMapper.selectOne(any())).thenReturn(plan());
        when(workOrderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectOne(any())).thenReturn(order());

        assertThrows(BusinessException.class, () -> service.generateWorkOrder(2L, request("99"), 9L));
        verify(workOrderMapper, never()).insert(any(MfgWorkOrder.class));
    }

    @Test
    void releaseMovesDraftWorkOrderToReleased() {
        MfgWorkOrder existing = request("100");
        existing.setWorkOrderId(5L);
        existing.setStatus(BusinessStatus.Document.DRAFT);
        existing.setVersion(4);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);
        when(workOrderMapper.updateById(any(MfgWorkOrder.class))).thenReturn(1);

        service.releaseWorkOrder(5L, 9L, null);

        org.mockito.ArgumentCaptor<MfgWorkOrder> update = org.mockito.ArgumentCaptor.forClass(MfgWorkOrder.class);
        verify(workOrderMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.RELEASED, update.getValue().getStatus());
        assertEquals(4, update.getValue().getVersion());
    }

    private MfgProductionPlan plan() {
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(2L);
        plan.setProductionOrderId(1L);
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
        request.setBomId(12L);
        request.setRouteId(13L);
        request.setPlannedQuantity(new BigDecimal(quantity));
        return request;
    }
}
