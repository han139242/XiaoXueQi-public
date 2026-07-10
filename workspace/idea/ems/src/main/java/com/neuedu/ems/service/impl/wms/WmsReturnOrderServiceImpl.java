package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.entity.wms.WmsReturnDetail;
import com.neuedu.ems.entity.wms.WmsReturnOrder;
import com.neuedu.ems.entity.wms.WmsStockMovement;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.mapper.wms.WmsReturnDetailMapper;
import com.neuedu.ems.mapper.wms.WmsReturnOrderMapper;
import com.neuedu.ems.mapper.wms.WmsStockMovementMapper;
import com.neuedu.ems.service.wms.WmsReturnOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 退料单管理 Service 实现类。
 */
@Service
public class WmsReturnOrderServiceImpl extends ServiceImpl<WmsReturnOrderMapper, WmsReturnOrder> implements WmsReturnOrderService {

    private static final String APPROVED_STATUS = "APPROVED";

    @Autowired
    private WmsReturnDetailMapper wmsReturnDetailMapper;

    @Autowired
    private WmsInventoryMapper wmsInventoryMapper;

    @Autowired
    private WmsStockMovementMapper wmsStockMovementMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 分页查询退料单，支持单号、类型、工单、申请人和审核状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配退料单号
     * @param returnType 退料类型
     * @param workOrderId 工单ID
     * @param applicantId 申请人ID
     * @param auditStatus 审核状态
     * @return 退料单分页结果
     */
    @Override
    public Page<WmsReturnOrder> pageReturnOrders(Integer current, Integer size, String keyword, String returnType,
                                                 Long workOrderId, Long applicantId, String auditStatus) {
        Page<WmsReturnOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildReturnOrderQuery(keyword, returnType, workOrderId, applicantId, auditStatus));
    }

    /**
     * 查询退料单列表，支持单号、类型、工单、申请人和审核状态筛选。
     * @param keyword 关键字，匹配退料单号
     * @param returnType 退料类型
     * @param workOrderId 工单ID
     * @param applicantId 申请人ID
     * @param auditStatus 审核状态
     * @return 退料单列表
     */
    @Override
    public List<WmsReturnOrder> listReturnOrders(String keyword, String returnType, Long workOrderId, Long applicantId, String auditStatus) {
        return baseMapper.selectList(buildReturnOrderQuery(keyword, returnType, workOrderId, applicantId, auditStatus));
    }

    /**
     * 根据退料单ID查询未删除的退料单详情。
     * @param returnId 退料单ID
     * @return 退料单实体
     */
    @Override
    public WmsReturnOrder getReturnOrderById(Long returnId) {
        LambdaQueryWrapper<WmsReturnOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsReturnOrder::getReturnId, returnId)
                .eq(WmsReturnOrder::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增退料单，自动设置删除标记为0。
     * @param returnOrder 退料单实体
     * @return 是否成功
     */
    @Override
    public boolean saveReturnOrder(WmsReturnOrder returnOrder) {
        if (returnOrder.getDeletedFlag() == null) {
            returnOrder.setDeletedFlag(0);
        }
        return baseMapper.insert(returnOrder) > 0;
    }

    /**
     * 新增退料单并保存退料明细。
     * @param returnOrder 退料单实体
     * @param details 退料明细列表
     * @return 退料单ID
     */
    @Override
    @Transactional
    public Long saveReturnOrderFull(WmsReturnOrder returnOrder, List<WmsReturnDetail> details) {
        saveReturnOrder(returnOrder);
        if (details != null) {
            for (WmsReturnDetail detail : details) {
                detail.setReturnId(returnOrder.getReturnId());
                if (detail.getDeletedFlag() == null) {
                    detail.setDeletedFlag(0);
                }
                wmsReturnDetailMapper.insert(detail);
            }
        }
        return returnOrder.getReturnId();
    }

    /**
     * 更新退料单，已确认退料单不允许普通修改。
     * @param returnOrder 退料单实体
     * @return 是否成功
     */
    @Override
    public boolean updateReturnOrder(WmsReturnOrder returnOrder) {
        if (returnOrder.getReturnId() == null) {
            return false;
        }
        WmsReturnOrder existing = getReturnOrderById(returnOrder.getReturnId());
        if (existing == null || APPROVED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        returnOrder.setDeletedFlag(null);
        return baseMapper.updateById(returnOrder) > 0;
    }

    /**
     * 更新退料单并替换退料明细，已确认退料单不允许替换明细。
     * @param returnOrder 退料单实体
     * @param details 退料明细列表
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateReturnOrderFull(WmsReturnOrder returnOrder, List<WmsReturnDetail> details) {
        if (!updateReturnOrder(returnOrder)) {
            return false;
        }
        if (details != null) {
            softDeleteDetails(returnOrder.getReturnId());
            for (WmsReturnDetail detail : details) {
                detail.setDetailId(null);
                detail.setReturnId(returnOrder.getReturnId());
                if (detail.getDeletedFlag() == null) {
                    detail.setDeletedFlag(0);
                }
                wmsReturnDetailMapper.insert(detail);
            }
        }
        return true;
    }

    /**
     * 根据ID软删除退料单和明细，已确认退料单不允许删除。
     * @param returnId 退料单ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean removeReturnOrderById(Long returnId) {
        WmsReturnOrder existing = getReturnOrderById(returnId);
        if (existing == null || APPROVED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        WmsReturnOrder returnOrder = new WmsReturnOrder();
        returnOrder.setReturnId(returnId);
        returnOrder.setDeletedFlag(1);
        boolean orderRemoved = baseMapper.updateById(returnOrder) > 0;
        softDeleteDetails(returnId);
        return orderRemoved;
    }

    /**
     * 确认退料入库，回加库存并写入库存流水。
     * @param returnId 退料单ID
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean confirmReturnOrder(Long returnId, Long operatorId, String remark) {
        WmsReturnOrder returnOrder = getReturnOrderById(returnId);
        if (returnOrder == null || APPROVED_STATUS.equals(returnOrder.getAuditStatus())) {
            return false;
        }
        List<WmsReturnDetail> details = listActiveDetails(returnId);
        if (details.isEmpty() || !canIncreaseInventory(details)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        for (WmsReturnDetail detail : details) {
            increaseInventory(detail);
            insertReturnMovement(returnOrder, detail, operatorId, remark, now);
        }
        WmsReturnOrder update = new WmsReturnOrder();
        update.setReturnId(returnId);
        update.setAuditStatus(APPROVED_STATUS);
        update.setReturnTime(now);
        update.setRemark(StringUtils.hasText(remark) ? remark : returnOrder.getRemark());
        boolean updated = baseMapper.updateById(update) > 0;
        if (updated) {
            insertStatusChangeLog(returnOrder.getReturnId(), returnOrder.getAuditStatus(), APPROVED_STATUS,
                    "WMS_RETURN_ORDER", "CONFIRM_RETURN", operatorId, remark, now);
            insertOperationLog(returnOrder.getReturnId(), "CONFIRM_RETURN", "wms_return_order", operatorId, remark, now);
        }
        return updated;
    }

    /**
     * 构建退料单查询条件，默认过滤已删除记录。
     * @param keyword 关键字，匹配退料单号
     * @param returnType 退料类型
     * @param workOrderId 工单ID
     * @param applicantId 申请人ID
     * @param auditStatus 审核状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsReturnOrder> buildReturnOrderQuery(String keyword, String returnType,
                                                                     Long workOrderId, Long applicantId, String auditStatus) {
        LambdaQueryWrapper<WmsReturnOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsReturnOrder::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), WmsReturnOrder::getReturnNo, keyword)
                .eq(StringUtils.hasText(returnType), WmsReturnOrder::getReturnType, returnType)
                .eq(workOrderId != null, WmsReturnOrder::getWorkOrderId, workOrderId)
                .eq(applicantId != null, WmsReturnOrder::getApplicantId, applicantId)
                .eq(StringUtils.hasText(auditStatus), WmsReturnOrder::getAuditStatus, auditStatus)
                .orderByDesc(WmsReturnOrder::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 软删除指定退料单的所有明细。
     * @param returnId 退料单ID
     */
    private void softDeleteDetails(Long returnId) {
        List<WmsReturnDetail> details = listActiveDetails(returnId);
        for (WmsReturnDetail detail : details) {
            WmsReturnDetail update = new WmsReturnDetail();
            update.setDetailId(detail.getDetailId());
            update.setDeletedFlag(1);
            wmsReturnDetailMapper.updateById(update);
        }
    }

    /**
     * 查询指定退料单的未删除明细。
     * @param returnId 退料单ID
     * @return 退料明细列表
     */
    private List<WmsReturnDetail> listActiveDetails(Long returnId) {
        LambdaQueryWrapper<WmsReturnDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsReturnDetail::getReturnId, returnId)
                .eq(WmsReturnDetail::getDeletedFlag, 0);
        return wmsReturnDetailMapper.selectList(queryWrapper);
    }

    /**
     * 校验退料明细是否可定位唯一库存行。
     * @param details 退料明细列表
     * @return 是否可以回加库存
     */
    private boolean canIncreaseInventory(List<WmsReturnDetail> details) {
        for (WmsReturnDetail detail : details) {
            BigDecimal returnQuantity = detail.getReturnQuantity() == null ? BigDecimal.ZERO : detail.getReturnQuantity();
            if (returnQuantity.compareTo(BigDecimal.ZERO) <= 0 || selectInventories(detail).size() != 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据物料和批次查询未删除库存行；退料表无仓库库位字段，因此必须唯一定位。
     * @param detail 退料明细实体
     * @return 库存余额列表
     */
    private List<WmsInventory> selectInventories(WmsReturnDetail detail) {
        LambdaQueryWrapper<WmsInventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInventory::getMaterialId, detail.getMaterialId())
                .eq(WmsInventory::getBatchId, detail.getBatchId())
                .eq(WmsInventory::getDeletedFlag, 0);
        return wmsInventoryMapper.selectList(queryWrapper);
    }

    /**
     * 根据退料明细回加唯一库存行的可用库存。
     * @param detail 退料明细实体
     */
    private void increaseInventory(WmsReturnDetail detail) {
        WmsInventory inventory = selectInventories(detail).get(0);
        BigDecimal availableQuantity = inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
        WmsInventory update = new WmsInventory();
        update.setInventoryId(inventory.getInventoryId());
        update.setAvailableQuantity(availableQuantity.add(detail.getReturnQuantity()));
        wmsInventoryMapper.updateById(update);
    }

    /**
     * 写入退料入库库存流水。
     * @param returnOrder 退料单实体
     * @param detail 退料明细实体
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param operationTime 操作时间
     */
    private void insertReturnMovement(WmsReturnOrder returnOrder, WmsReturnDetail detail,
                                      Long operatorId, String remark, LocalDateTime operationTime) {
        WmsStockMovement movement = new WmsStockMovement();
        movement.setMovementNo("MV" + System.currentTimeMillis() + detail.getDetailId());
        movement.setMovementType("RETURN");
        movement.setSourceOrderType("RETURN_ORDER");
        movement.setSourceOrderId(returnOrder.getReturnId());
        movement.setMaterialId(detail.getMaterialId());
        movement.setBatchId(detail.getBatchId());
        movement.setQuantity(detail.getReturnQuantity());
        movement.setDirection("IN");
        movement.setOperationTime(operationTime);
        movement.setCreatedBy(operatorId);
        movement.setRemark(remark);
        movement.setDeletedFlag(0);
        wmsStockMovementMapper.insert(movement);
    }

    /**
     * 写入状态变更日志。
     * @param bizId 业务ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @param bizType 业务类型
     * @param reason 变更原因
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param changedTime 变更时间
     */
    private void insertStatusChangeLog(Long bizId, String oldStatus, String newStatus, String bizType,
                                       String reason, Long operatorId, String remark, LocalDateTime changedTime) {
        SysStatusChangeLog statusLog = new SysStatusChangeLog();
        statusLog.setBizType(bizType);
        statusLog.setBizId(bizId);
        statusLog.setOldStatus(oldStatus);
        statusLog.setNewStatus(newStatus);
        statusLog.setChangeReason(reason);
        statusLog.setChangedBy(operatorId);
        statusLog.setChangedTime(changedTime);
        statusLog.setCreatedBy(operatorId);
        statusLog.setCreatedTime(changedTime);
        statusLog.setRemark(remark);
        statusLog.setDeletedFlag(0);
        sysStatusChangeLogMapper.insert(statusLog);
    }

    /**
     * 写入操作日志。
     * @param bizId 业务ID
     * @param operationType 操作类型
     * @param moduleCode 模块编码
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param operationTime 操作时间
     */
    private void insertOperationLog(Long bizId, String operationType, String moduleCode,
                                    Long operatorId, String remark, LocalDateTime operationTime) {
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setUserId(operatorId);
        operationLog.setOperationType(operationType);
        operationLog.setModuleCode(moduleCode);
        operationLog.setBizId(bizId);
        operationLog.setOperationTime(operationTime);
        operationLog.setCreatedBy(operatorId);
        operationLog.setCreatedTime(operationTime);
        operationLog.setRemark(remark);
        operationLog.setDeletedFlag(0);
        sysOperationLogMapper.insert(operationLog);
    }
}
