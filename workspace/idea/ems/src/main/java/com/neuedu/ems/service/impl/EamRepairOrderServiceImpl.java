package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.EamEquipment;
import com.neuedu.ems.entity.EamEquipmentStatusLog;
import com.neuedu.ems.entity.EamRepairOrder;
import com.neuedu.ems.entity.SysOperationLog;
import com.neuedu.ems.entity.SysStatusChangeLog;
import com.neuedu.ems.mapper.EamEquipmentMapper;
import com.neuedu.ems.mapper.EamEquipmentStatusLogMapper;
import com.neuedu.ems.mapper.EamRepairOrderMapper;
import com.neuedu.ems.mapper.SysOperationLogMapper;
import com.neuedu.ems.mapper.SysStatusChangeLogMapper;
import com.neuedu.ems.service.EamRepairOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备维修单 Service 实现类。
 */
@Service
public class EamRepairOrderServiceImpl extends ServiceImpl<EamRepairOrderMapper, EamRepairOrder>
        implements EamRepairOrderService {

    private static final String STATUS_REPAIRING = "REPAIRING";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String EQUIPMENT_STATUS_RUNNING = "RUNNING";
    private static final String BIZ_TYPE = "EAM_REPAIR_ORDER";
    private static final String MODULE_CODE = "eam_repair_order";

    @Autowired
    private EamEquipmentMapper eamEquipmentMapper;

    @Autowired
    private EamEquipmentStatusLogMapper eamEquipmentStatusLogMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 分页查询维修单，默认过滤已删除记录。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配维修单号或故障描述
     * @param equipmentId 设备ID
     * @param exceptionId 生产异常ID
     * @param repairerId 维修人ID
     * @param status 状态
     * @return 分页结果
     */
    @Override
    public Page<EamRepairOrder> pageRepairOrders(Integer current, Integer size, String keyword, Long equipmentId,
                                                 Long exceptionId, Long repairerId, String status) {
        Page<EamRepairOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildRepairOrderQuery(keyword, equipmentId, exceptionId, repairerId, status));
    }

    /**
     * 查询维修单列表，默认过滤已删除记录。
     * @param keyword 关键字，匹配维修单号或故障描述
     * @param equipmentId 设备ID
     * @param exceptionId 生产异常ID
     * @param repairerId 维修人ID
     * @param status 状态
     * @return 维修单列表
     */
    @Override
    public List<EamRepairOrder> listRepairOrders(String keyword, Long equipmentId, Long exceptionId,
                                                 Long repairerId, String status) {
        return baseMapper.selectList(buildRepairOrderQuery(keyword, equipmentId, exceptionId, repairerId, status));
    }

    /**
     * 根据维修单ID查询未删除的维修单详情。
     * @param repairId 维修单ID
     * @return 维修单实体
     */
    @Override
    public EamRepairOrder getRepairOrderById(Long repairId) {
        LambdaQueryWrapper<EamRepairOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamRepairOrder::getRepairId, repairId)
                .eq(EamRepairOrder::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增维修单，自动设置删除标志为0。
     * @param repairOrder 维修单实体
     * @return 是否成功
     */
    @Override
    public boolean saveRepairOrder(EamRepairOrder repairOrder) {
        if (repairOrder.getDeletedFlag() == null) {
            repairOrder.setDeletedFlag(0);
        }
        return baseMapper.insert(repairOrder) > 0;
    }

    /**
     * 更新维修单，不允许通过普通更新修改删除标志。
     * @param repairOrder 维修单实体
     * @return 是否成功
     */
    @Override
    public boolean updateRepairOrder(EamRepairOrder repairOrder) {
        if (repairOrder.getRepairId() == null) {
            return false;
        }
        repairOrder.setDeletedFlag(null);
        return baseMapper.updateById(repairOrder) > 0;
    }

    /**
     * 根据ID软删除维修单。
     * @param repairId 维修单ID
     * @return 是否成功
     */
    @Override
    public boolean removeRepairOrderById(Long repairId) {
        EamRepairOrder repairOrder = new EamRepairOrder();
        repairOrder.setRepairId(repairId);
        repairOrder.setDeletedFlag(1);
        return baseMapper.updateById(repairOrder) > 0;
    }

    /**
     * 派工维修单并同步设备状态、设备状态日志、系统状态日志和操作日志。
     * @param repairId 维修单ID
     * @param repairerId 维修人ID
     * @param remark 派工备注
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean assignRepairOrder(Long repairId, Long repairerId, String remark) {
        EamRepairOrder existing = getRepairOrderById(repairId);
        if (existing == null || repairerId == null || STATUS_CLOSED.equals(existing.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        EamRepairOrder update = new EamRepairOrder();
        update.setRepairId(repairId);
        update.setRepairerId(repairerId);
        update.setStatus(STATUS_REPAIRING);
        update.setRemark(StringUtils.hasText(remark) ? remark : existing.getRemark());
        boolean updated = baseMapper.updateById(update) > 0;
        if (updated) {
            EamEquipment equipment = updateEquipmentStatus(existing.getEquipmentId(), STATUS_REPAIRING);
            insertEquipmentStatusLog(existing.getEquipmentId(), equipment, STATUS_REPAIRING, remark, repairerId, now);
            insertStatusChangeLog(existing, STATUS_REPAIRING, "ASSIGN_REPAIR", repairerId, remark, now);
            insertOperationLog(existing.getRepairId(), "ASSIGN_REPAIR", repairerId, remark, now);
        }
        return updated;
    }

    /**
     * 关闭维修单并同步设备状态、设备状态日志、系统状态日志和操作日志。
     * @param repairId 维修单ID
     * @param repairResult 维修结果
     * @param repairCost 维修费用
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean closeRepairOrder(Long repairId, String repairResult, BigDecimal repairCost) {
        EamRepairOrder existing = getRepairOrderById(repairId);
        if (existing == null || STATUS_CLOSED.equals(existing.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        EamRepairOrder update = new EamRepairOrder();
        update.setRepairId(repairId);
        update.setRepairResult(repairResult);
        update.setRepairCost(repairCost);
        update.setStatus(STATUS_CLOSED);
        boolean updated = baseMapper.updateById(update) > 0;
        if (updated) {
            Long operatorId = existing.getRepairerId();
            EamEquipment equipment = updateEquipmentStatus(existing.getEquipmentId(), EQUIPMENT_STATUS_RUNNING);
            insertEquipmentStatusLog(existing.getEquipmentId(), equipment, EQUIPMENT_STATUS_RUNNING, repairResult, operatorId, now);
            insertStatusChangeLog(existing, STATUS_CLOSED, "CLOSE_REPAIR", operatorId, repairResult, now);
            insertOperationLog(existing.getRepairId(), "CLOSE_REPAIR", operatorId, repairResult, now);
        }
        return updated;
    }

    /**
     * 构建维修单查询条件。
     * @param keyword 关键字，匹配维修单号或故障描述
     * @param equipmentId 设备ID
     * @param exceptionId 生产异常ID
     * @param repairerId 维修人ID
     * @param status 状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<EamRepairOrder> buildRepairOrderQuery(String keyword, Long equipmentId,
                                                                     Long exceptionId, Long repairerId, String status) {
        LambdaQueryWrapper<EamRepairOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamRepairOrder::getDeletedFlag, 0)
                .and(StringUtils.hasText(keyword), item -> item.like(EamRepairOrder::getRepairNo, keyword)
                        .or().like(EamRepairOrder::getFaultDescription, keyword))
                .eq(equipmentId != null, EamRepairOrder::getEquipmentId, equipmentId)
                .eq(exceptionId != null, EamRepairOrder::getExceptionId, exceptionId)
                .eq(repairerId != null, EamRepairOrder::getRepairerId, repairerId)
                .eq(StringUtils.hasText(status), EamRepairOrder::getStatus, status)
                .orderByDesc(EamRepairOrder::getReportedTime)
                .orderByDesc(EamRepairOrder::getRepairId);
        return queryWrapper;
    }

    /**
     * 更新设备台账状态并返回原设备信息。
     * @param equipmentId 设备ID
     * @param targetStatus 目标设备状态
     * @return 更新前的设备实体，设备不存在时返回null
     */
    private EamEquipment updateEquipmentStatus(Long equipmentId, String targetStatus) {
        if (equipmentId == null) {
            return null;
        }
        EamEquipment equipment = eamEquipmentMapper.selectById(equipmentId);
        if (equipment == null || Integer.valueOf(1).equals(equipment.getDeletedFlag())) {
            return null;
        }
        EamEquipment update = new EamEquipment();
        update.setEquipmentId(equipmentId);
        update.setStatus(targetStatus);
        eamEquipmentMapper.updateById(update);
        return equipment;
    }

    /**
     * 写入设备状态日志。
     * @param equipmentId 设备ID
     * @param equipment 更新前的设备实体
     * @param runningStatus 新运行状态
     * @param description 状态说明
     * @param operatorId 操作人ID
     * @param collectedTime 采集时间
     */
    private void insertEquipmentStatusLog(Long equipmentId, EamEquipment equipment, String runningStatus,
                                          String description, Long operatorId, LocalDateTime collectedTime) {
        EamEquipmentStatusLog statusLog = new EamEquipmentStatusLog();
        statusLog.setEquipmentId(equipmentId);
        statusLog.setRunningStatus(runningStatus);
        statusLog.setStationId(equipment == null ? null : equipment.getStationId());
        statusLog.setCollectedTime(collectedTime);
        statusLog.setDescription(description);
        statusLog.setCreatedBy(operatorId);
        statusLog.setCreatedTime(collectedTime);
        statusLog.setDeletedFlag(0);
        eamEquipmentStatusLogMapper.insert(statusLog);
    }

    /**
     * 写入维修单状态变更日志。
     * @param repairOrder 更新前的维修单实体
     * @param newStatus 新状态
     * @param changeReason 变更原因
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param changedTime 变更时间
     */
    private void insertStatusChangeLog(EamRepairOrder repairOrder, String newStatus, String changeReason,
                                       Long operatorId, String remark, LocalDateTime changedTime) {
        SysStatusChangeLog statusLog = new SysStatusChangeLog();
        statusLog.setBizType(BIZ_TYPE);
        statusLog.setBizId(repairOrder.getRepairId());
        statusLog.setOldStatus(repairOrder.getStatus());
        statusLog.setNewStatus(newStatus);
        statusLog.setChangeReason(changeReason);
        statusLog.setChangedBy(operatorId);
        statusLog.setChangedTime(changedTime);
        statusLog.setCreatedBy(operatorId);
        statusLog.setCreatedTime(changedTime);
        statusLog.setRemark(remark);
        statusLog.setDeletedFlag(0);
        sysStatusChangeLogMapper.insert(statusLog);
    }

    /**
     * 写入维修单操作日志。
     * @param repairId 维修单ID
     * @param operationType 操作类型
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param operationTime 操作时间
     */
    private void insertOperationLog(Long repairId, String operationType, Long operatorId, String remark,
                                    LocalDateTime operationTime) {
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setUserId(operatorId);
        operationLog.setOperationType(operationType);
        operationLog.setModuleCode(MODULE_CODE);
        operationLog.setBizId(repairId);
        operationLog.setOperationTime(operationTime);
        operationLog.setCreatedBy(operatorId);
        operationLog.setCreatedTime(operationTime);
        operationLog.setRemark(remark);
        operationLog.setDeletedFlag(0);
        sysOperationLogMapper.insert(operationLog);
    }
}
