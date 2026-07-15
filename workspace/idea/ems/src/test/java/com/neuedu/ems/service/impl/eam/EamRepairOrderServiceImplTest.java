package com.neuedu.ems.service.impl.eam;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.eam.EamEquipment;
import com.neuedu.ems.entity.eam.EamEquipmentStatusLog;
import com.neuedu.ems.entity.eam.EamRepairOrder;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.mapper.eam.EamEquipmentMapper;
import com.neuedu.ems.mapper.eam.EamEquipmentStatusLogMapper;
import com.neuedu.ems.mapper.eam.EamRepairOrderMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EamRepairOrderServiceImplTest {

    private EamRepairOrderServiceImpl service;
    private EamRepairOrderMapper repairMapper;
    private EamEquipmentMapper equipmentMapper;
    private EamEquipmentStatusLogMapper equipmentStatusLogMapper;
    private SysStatusChangeLogMapper statusChangeLogMapper;
    private SysOperationLogMapper operationLogMapper;

    @BeforeEach
    void setUp() {
        service = new EamRepairOrderServiceImpl();
        repairMapper = mock(EamRepairOrderMapper.class);
        equipmentMapper = mock(EamEquipmentMapper.class);
        equipmentStatusLogMapper = mock(EamEquipmentStatusLogMapper.class);
        statusChangeLogMapper = mock(SysStatusChangeLogMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", repairMapper);
        ReflectionTestUtils.setField(service, "eamEquipmentMapper", equipmentMapper);
        ReflectionTestUtils.setField(service, "eamEquipmentStatusLogMapper", equipmentStatusLogMapper);
        ReflectionTestUtils.setField(service, "sysStatusChangeLogMapper", statusChangeLogMapper);
        ReflectionTestUtils.setField(service, "sysOperationLogMapper", operationLogMapper);
        ReflectionTestUtils.setField(service, "responsibilityNotificationService",
                mock(EamResponsibilityNotificationService.class));
    }

    @Test
    void saveForcesSubmittedStateAndClearsExecutionFields() {
        when(repairMapper.insert(any(EamRepairOrder.class))).thenReturn(1);
        EamRepairOrder request = repair(BusinessStatus.Repair.CLOSED);
        request.setRepairerId(8L);
        request.setRepairResult("绕过流程");
        request.setRepairCost(BigDecimal.TEN);

        assertTrue(service.saveRepairOrder(request));
        assertEquals(BusinessStatus.Repair.SUBMITTED, request.getStatus());
        assertNull(request.getRepairerId());
        assertNull(request.getRepairResult());
        assertNull(request.getRepairCost());
    }

    @Test
    void assignOnlyAcceptsSubmittedOrder() {
        when(repairMapper.selectOne(any())).thenReturn(repair(BusinessStatus.Repair.REPAIRING));

        assertFalse(service.assignRepairOrder(1L, 8L, null, 9L));
        verify(repairMapper, never()).update(any(EamRepairOrder.class), any());
    }

    @Test
    void assignUsesEquipmentRepairingStateForEquipmentAndRunningLog() {
        EamRepairOrder existing = repair(BusinessStatus.Repair.SUBMITTED);
        existing.setEquipmentId(12L);
        EamEquipment equipment = new EamEquipment();
        equipment.setEquipmentId(12L);
        equipment.setStatus(BusinessStatus.Equipment.IDLE);
        equipment.setDeletedFlag(0);
        when(repairMapper.selectOne(any())).thenReturn(existing);
        when(repairMapper.update(any(EamRepairOrder.class), any())).thenReturn(1);
        when(equipmentMapper.selectById(12L)).thenReturn(equipment);

        assertTrue(service.assignRepairOrder(1L, 8L, "派工", 9L));

        ArgumentCaptor<EamEquipment> equipmentUpdate = ArgumentCaptor.forClass(EamEquipment.class);
        ArgumentCaptor<EamEquipmentStatusLog> runningLog = ArgumentCaptor.forClass(EamEquipmentStatusLog.class);
        verify(equipmentMapper).updateById(equipmentUpdate.capture());
        verify(equipmentStatusLogMapper).insert(runningLog.capture());
        assertEquals(BusinessStatus.Equipment.REPAIRING, equipmentUpdate.getValue().getStatus());
        assertEquals(BusinessStatus.Equipment.REPAIRING, runningLog.getValue().getRunningStatus());
    }

    @Test
    void closeAcceptsRepairingOrderAndRejectsMissingResult() {
        when(repairMapper.selectOne(any())).thenReturn(repair(BusinessStatus.Repair.REPAIRING));
        when(repairMapper.update(any(EamRepairOrder.class), any())).thenReturn(1);

        assertTrue(service.closeRepairOrder(1L, "试运行正常", BigDecimal.TEN, 9L));

        ArgumentCaptor<EamRepairOrder> update = ArgumentCaptor.forClass(EamRepairOrder.class);
        verify(repairMapper).update(update.capture(), any());
        assertEquals(BusinessStatus.Repair.CLOSED, update.getValue().getStatus());
        assertEquals("试运行正常", update.getValue().getRepairResult());
    }

    @Test
    void ordinaryUpdateCannotBypassRepairingState() {
        when(repairMapper.selectOne(any())).thenReturn(repair(BusinessStatus.Repair.REPAIRING));
        EamRepairOrder request = repair(BusinessStatus.Repair.CLOSED);

        assertFalse(service.updateRepairOrder(request));
        verify(repairMapper, never()).update(any(EamRepairOrder.class), any());
    }

    @Test
    void assignDoesNotWriteEquipmentOrLogsWhenConditionalUpdateLosesRace() {
        when(repairMapper.selectOne(any())).thenReturn(repair(BusinessStatus.Repair.SUBMITTED));
        when(repairMapper.update(any(EamRepairOrder.class), any())).thenReturn(0);

        assertFalse(service.assignRepairOrder(1L, 8L, "派工", 9L));
        verify(equipmentMapper, never()).updateById(any(EamEquipment.class));
        verify(equipmentStatusLogMapper, never()).insert(any(EamEquipmentStatusLog.class));
        verify(statusChangeLogMapper, never()).insert(any(SysStatusChangeLog.class));
        verify(operationLogMapper, never()).insert(any(SysOperationLog.class));
    }

    private EamRepairOrder repair(String status) {
        EamRepairOrder order = new EamRepairOrder();
        order.setRepairId(1L);
        order.setStatus(status);
        return order;
    }
}
