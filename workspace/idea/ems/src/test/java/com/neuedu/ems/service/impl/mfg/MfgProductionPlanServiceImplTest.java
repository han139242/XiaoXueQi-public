package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgProductionPlanServiceImplTest {

    private MfgProductionPlanServiceImpl service;
    private MfgProductionPlanMapper planMapper;
    private MfgProductionOrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        service = new MfgProductionPlanServiceImpl();
        planMapper = mock(MfgProductionPlanMapper.class);
        orderMapper = mock(MfgProductionOrderMapper.class);
        SysStatusChangeLogMapper statusLogMapper = mock(SysStatusChangeLogMapper.class);
        SysOperationLogMapper operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", planMapper);
        ReflectionTestUtils.setField(service, "productionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        when(statusLogMapper.insert(any(SysStatusChangeLog.class))).thenReturn(1);
        when(operationLogMapper.insert(any(SysOperationLog.class))).thenReturn(1);
    }

    @Test
    void generateCreatesReleasedPlanWithinRemainingOrderQuantity() {
        when(orderMapper.selectOne(any())).thenReturn(order("100"));
        MfgProductionPlan total = new MfgProductionPlan();
        total.setPlannedQuantity(new BigDecimal("40"));
        when(planMapper.selectOne(any())).thenReturn(total);
        when(planMapper.insert(any(MfgProductionPlan.class))).thenAnswer(invocation -> {
            invocation.<MfgProductionPlan>getArgument(0).setPlanId(3L);
            return 1;
        });

        MfgProductionPlan result = service.generatePlan(1L, request("60"), 9L);

        assertEquals(3L, result.getPlanId());
        assertEquals(BusinessStatus.Document.RELEASED, result.getStatus());
        assertTrue(result.getPlanNo().startsWith("PLN-1-"));
        verify(planMapper).insert(any(MfgProductionPlan.class));
    }

    @Test
    void generateRejectsQuantityAboveOrderRemainder() {
        when(orderMapper.selectOne(any())).thenReturn(order("100"));
        MfgProductionPlan total = new MfgProductionPlan();
        total.setPlannedQuantity(new BigDecimal("80"));
        when(planMapper.selectOne(any())).thenReturn(total);

        assertThrows(BusinessException.class, () -> service.generatePlan(1L, request("21"), 9L));
        verify(planMapper, never()).insert(any(MfgProductionPlan.class));
    }

    @Test
    void detachedPlanCreationIsRejected() {
        assertThrows(BusinessException.class, () -> service.savePlan(new MfgProductionPlan()));
        verify(planMapper, never()).insert(any(MfgProductionPlan.class));
    }

    @Test
    void releaseMovesWithdrawnDraftPlanBackToReleased() {
        MfgProductionPlan draft = request("60");
        draft.setPlanId(3L);
        draft.setProductionOrderId(1L);
        draft.setStatus(BusinessStatus.Document.DRAFT);
        draft.setVersion(2);
        MfgProductionPlan total = new MfgProductionPlan();
        total.setPlannedQuantity(new BigDecimal("60"));
        when(planMapper.selectOne(any())).thenReturn(draft, total, draft);
        when(orderMapper.selectOne(any())).thenReturn(order("100"));
        when(planMapper.updateById(any(MfgProductionPlan.class))).thenReturn(1);

        service.releasePlan(3L, 9L, "修改后重新生效");

        org.mockito.ArgumentCaptor<MfgProductionPlan> update =
                org.mockito.ArgumentCaptor.forClass(MfgProductionPlan.class);
        verify(planMapper).updateById(update.capture());
        assertEquals(BusinessStatus.Document.RELEASED, update.getValue().getStatus());
        assertEquals(2, update.getValue().getVersion());
    }

    private MfgProductionOrder order(String quantity) {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(1L);
        order.setOrderQuantity(new BigDecimal(quantity));
        order.setStatus(BusinessStatus.Document.RELEASED);
        return order;
    }

    private MfgProductionPlan request(String quantity) {
        MfgProductionPlan request = new MfgProductionPlan();
        request.setLineId(2L);
        request.setPlannedStartTime(LocalDateTime.of(2026, 7, 14, 8, 0));
        request.setPlannedEndTime(LocalDateTime.of(2026, 7, 15, 18, 0));
        request.setPlannedQuantity(new BigDecimal(quantity));
        return request;
    }
}
