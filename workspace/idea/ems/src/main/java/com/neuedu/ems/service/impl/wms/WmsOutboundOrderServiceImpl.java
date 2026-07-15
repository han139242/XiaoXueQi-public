package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.entity.wms.WmsOutboundDetail;
import com.neuedu.ems.entity.wms.WmsOutboundOrder;
import com.neuedu.ems.entity.wms.WmsStockMovement;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundDetailMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundOrderMapper;
import com.neuedu.ems.mapper.wms.WmsStockMovementMapper;
import com.neuedu.ems.service.wms.WmsOutboundOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 出库单管理 Service 实现类。
 */
@Service
public class WmsOutboundOrderServiceImpl extends ServiceImpl<WmsOutboundOrderMapper, WmsOutboundOrder> implements WmsOutboundOrderService {

    private static final String SUBMITTED_STATUS = BusinessStatus.Document.SUBMITTED;
    private static final String APPROVED_STATUS = BusinessStatus.Document.APPROVED;

    @Autowired
    private WmsOutboundDetailMapper wmsOutboundDetailMapper;

    @Autowired
    private WmsInventoryMapper wmsInventoryMapper;

    @Autowired
    private WmsStockMovementMapper wmsStockMovementMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private WmsResponsibilityNotificationService responsibilityNotificationService;

    /**
     * 分页查询出库单，支持单号、类型、仓库、工单、申请人和审核状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配出库单号
     * @param outboundType 出库类型
     * @param warehouseId 仓库ID
     * @param workOrderId 工单ID
     * @param applicantId 申请人ID
     * @param auditStatus 审核状态
     * @return 出库单分页结果
     */
    @Override
    public Page<WmsOutboundOrder> pageOutboundOrders(Integer current, Integer size, String keyword, String outboundType,
                                                     Long warehouseId, Long workOrderId, Long applicantId, String auditStatus) {
        Page<WmsOutboundOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildOutboundOrderQuery(keyword, outboundType, warehouseId, workOrderId, applicantId, auditStatus));
    }

    /**
     * 查询出库单列表，支持单号、类型、仓库、工单、申请人和审核状态筛选。
     * @param keyword 关键字，匹配出库单号
     * @param outboundType 出库类型
     * @param warehouseId 仓库ID
     * @param workOrderId 工单ID
     * @param applicantId 申请人ID
     * @param auditStatus 审核状态
     * @return 出库单列表
     */
    @Override
    public List<WmsOutboundOrder> listOutboundOrders(String keyword, String outboundType, Long warehouseId,
                                                     Long workOrderId, Long applicantId, String auditStatus) {
        return baseMapper.selectList(buildOutboundOrderQuery(keyword, outboundType, warehouseId, workOrderId, applicantId, auditStatus));
    }

    /**
     * 根据出库单ID查询未删除的出库单详情。
     * @param outboundId 出库单ID
     * @return 出库单实体
     */
    @Override
    public WmsOutboundOrder getOutboundOrderById(Long outboundId) {
        LambdaQueryWrapper<WmsOutboundOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsOutboundOrder::getOutboundId, outboundId)
                .eq(WmsOutboundOrder::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增出库单，自动设置删除标记为0。
     * @param outboundOrder 出库单实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean saveOutboundOrder(WmsOutboundOrder outboundOrder) {
        if (!StringUtils.hasText(outboundOrder.getAuditStatus())) {
            outboundOrder.setAuditStatus(SUBMITTED_STATUS);
        }
        if (outboundOrder.getDeletedFlag() == null) {
            outboundOrder.setDeletedFlag(0);
        }
        boolean saved = baseMapper.insert(outboundOrder) > 0;
        if (saved) {
            responsibilityNotificationService.publishOutboundPending(outboundOrder, outboundOrder.getCreatedBy());
        }
        return saved;
    }

    /**
     * 新增出库单并保存出库明细。
     * @param outboundOrder 出库单实体
     * @param details 出库明细列表
     * @return 出库单ID
     */
    @Override
    @Transactional
    public Long saveOutboundOrderFull(WmsOutboundOrder outboundOrder, List<WmsOutboundDetail> details) {
        saveOutboundOrder(outboundOrder);
        if (details != null) {
            for (WmsOutboundDetail detail : details) {
                detail.setOutboundId(outboundOrder.getOutboundId());
                if (detail.getDeletedFlag() == null) {
                    detail.setDeletedFlag(0);
                }
                wmsOutboundDetailMapper.insert(detail);
            }
        }
        return outboundOrder.getOutboundId();
    }

    /**
     * 更新出库单，已确认出库单不允许普通修改。
     * @param outboundOrder 出库单实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateOutboundOrder(WmsOutboundOrder outboundOrder) {
        if (outboundOrder.getOutboundId() == null) {
            return false;
        }
        WmsOutboundOrder existing = getOutboundOrderById(outboundOrder.getOutboundId());
        if (existing == null || APPROVED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        outboundOrder.setDeletedFlag(null);
        outboundOrder.setAuditStatus(null);
        outboundOrder.setOutboundTime(null);
        if (outboundOrder.getVersion() == null) {
            outboundOrder.setVersion(existing.getVersion());
        }
        boolean warehouseChanged = outboundOrder.getWarehouseId() != null
                && !Objects.equals(existing.getWarehouseId(), outboundOrder.getWarehouseId());
        boolean updated = baseMapper.updateById(outboundOrder) > 0;
        if (updated && warehouseChanged) {
            responsibilityNotificationService.cancelOutboundPending(existing.getOutboundId(), outboundOrder.getUpdatedBy());
            responsibilityNotificationService.publishOutboundPending(
                    getOutboundOrderById(existing.getOutboundId()), outboundOrder.getUpdatedBy());
        }
        return updated;
    }

    /**
     * 更新出库单并替换出库明细，已确认出库单不允许替换明细。
     * @param outboundOrder 出库单实体
     * @param details 出库明细列表
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateOutboundOrderFull(WmsOutboundOrder outboundOrder, List<WmsOutboundDetail> details) {
        if (!updateOutboundOrder(outboundOrder)) {
            return false;
        }
        if (details != null) {
            softDeleteDetails(outboundOrder.getOutboundId());
            for (WmsOutboundDetail detail : details) {
                detail.setDetailId(null);
                detail.setOutboundId(outboundOrder.getOutboundId());
                if (detail.getDeletedFlag() == null) {
                    detail.setDeletedFlag(0);
                }
                wmsOutboundDetailMapper.insert(detail);
            }
        }
        return true;
    }

    /**
     * 根据ID软删除出库单和明细，已确认出库单不允许删除。
     * @param outboundId 出库单ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean removeOutboundOrderById(Long outboundId) {
        WmsOutboundOrder existing = getOutboundOrderById(outboundId);
        if (existing == null || APPROVED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        WmsOutboundOrder outboundOrder = new WmsOutboundOrder();
        outboundOrder.setOutboundId(outboundId);
        outboundOrder.setDeletedFlag(1);
        outboundOrder.setVersion(existing.getVersion());
        boolean orderRemoved = baseMapper.updateById(outboundOrder) > 0;
        softDeleteDetails(outboundId);
        if (orderRemoved) {
            responsibilityNotificationService.cancelOutboundPending(outboundId, existing.getUpdatedBy());
        }
        return orderRemoved;
    }

    /**
     * 确认出库，校验库存后扣减可用库存并写入库存流水。
     * @param outboundId 出库单ID
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean confirmOutboundOrder(Long outboundId, Long operatorId, String remark) {
        WmsOutboundOrder outboundOrder = getOutboundOrderById(outboundId);
        if (outboundOrder == null || !SUBMITTED_STATUS.equals(outboundOrder.getAuditStatus())) {
            return false;
        }
        List<WmsOutboundDetail> details = listActiveDetails(outboundId);
        if (details.isEmpty() || !canDecreaseInventory(outboundOrder, details)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        for (WmsOutboundDetail detail : details) {
            decreaseInventory(outboundOrder, detail);
            insertOutboundMovement(outboundOrder, detail, operatorId, remark, now);
        }
        WmsOutboundOrder update = new WmsOutboundOrder();
        update.setOutboundId(outboundId);
        update.setVersion(outboundOrder.getVersion());
        update.setAuditStatus(APPROVED_STATUS);
        update.setOutboundTime(now);
        update.setRemark(StringUtils.hasText(remark) ? remark : outboundOrder.getRemark());
        boolean updated = baseMapper.updateById(update) > 0;
        if (updated) {
            insertStatusChangeLog(outboundOrder.getOutboundId(), outboundOrder.getAuditStatus(), APPROVED_STATUS,
                    "WMS_OUTBOUND_ORDER", "CONFIRM_OUTBOUND", operatorId, remark, now);
            insertOperationLog(outboundOrder.getOutboundId(), "CONFIRM_OUTBOUND", "wms_outbound_order", operatorId, remark, now);
            responsibilityNotificationService.cancelOutboundPending(outboundId, operatorId);
            responsibilityNotificationService.publishOutboundCompleted(outboundOrder, operatorId);
        }
        return updated;
    }

    /**
     * 构建出库单查询条件，默认过滤已删除记录。
     * @param keyword 关键字，匹配出库单号
     * @param outboundType 出库类型
     * @param warehouseId 仓库ID
     * @param workOrderId 工单ID
     * @param applicantId 申请人ID
     * @param auditStatus 审核状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsOutboundOrder> buildOutboundOrderQuery(String keyword, String outboundType,
                                                                         Long warehouseId, Long workOrderId,
                                                                         Long applicantId, String auditStatus) {
        LambdaQueryWrapper<WmsOutboundOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsOutboundOrder::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), WmsOutboundOrder::getOutboundNo, keyword)
                .eq(StringUtils.hasText(outboundType), WmsOutboundOrder::getOutboundType, outboundType)
                .eq(warehouseId != null, WmsOutboundOrder::getWarehouseId, warehouseId)
                .eq(workOrderId != null, WmsOutboundOrder::getWorkOrderId, workOrderId)
                .eq(applicantId != null, WmsOutboundOrder::getApplicantId, applicantId)
                .eq(StringUtils.hasText(auditStatus), WmsOutboundOrder::getAuditStatus, auditStatus)
                .orderByDesc(WmsOutboundOrder::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 软删除指定出库单的所有明细。
     * @param outboundId 出库单ID
     */
    private void softDeleteDetails(Long outboundId) {
        List<WmsOutboundDetail> details = listActiveDetails(outboundId);
        for (WmsOutboundDetail detail : details) {
            WmsOutboundDetail update = new WmsOutboundDetail();
            update.setDetailId(detail.getDetailId());
            update.setDeletedFlag(1);
            wmsOutboundDetailMapper.updateById(update);
        }
    }

    /**
     * 查询指定出库单的未删除明细。
     * @param outboundId 出库单ID
     * @return 出库明细列表
     */
    private List<WmsOutboundDetail> listActiveDetails(Long outboundId) {
        LambdaQueryWrapper<WmsOutboundDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsOutboundDetail::getOutboundId, outboundId)
                .eq(WmsOutboundDetail::getDeletedFlag, 0);
        return wmsOutboundDetailMapper.selectList(queryWrapper);
    }

    /**
     * 校验出库明细库存是否足够扣减。
     * @param outboundOrder 出库单实体
     * @param details 出库明细列表
     * @return 是否可以扣减
     */
    private boolean canDecreaseInventory(WmsOutboundOrder outboundOrder, List<WmsOutboundDetail> details) {
        for (WmsOutboundDetail detail : details) {
            BigDecimal outboundQuantity = detail.getOutboundQuantity() == null ? BigDecimal.ZERO : detail.getOutboundQuantity();
            WmsInventory inventory = selectInventory(outboundOrder.getWarehouseId(), detail.getLocationId(), detail.getMaterialId(), detail.getBatchId());
            BigDecimal availableQuantity = inventory == null || inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
            if (outboundQuantity.compareTo(BigDecimal.ZERO) <= 0 || availableQuantity.compareTo(outboundQuantity) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据仓库、库位、物料和批次查询未删除库存余额。
     * @param warehouseId 仓库ID
     * @param locationId 库位ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 库存余额实体
     */
    private WmsInventory selectInventory(Long warehouseId, Long locationId, Long materialId, Long batchId) {
        LambdaQueryWrapper<WmsInventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInventory::getWarehouseId, warehouseId)
                .eq(WmsInventory::getLocationId, locationId)
                .eq(WmsInventory::getMaterialId, materialId)
                .eq(WmsInventory::getBatchId, batchId)
                .eq(WmsInventory::getDeletedFlag, 0);
        return wmsInventoryMapper.selectOne(queryWrapper);
    }

    /**
     * 根据出库明细扣减可用库存。
     * @param outboundOrder 出库单实体
     * @param detail 出库明细实体
     */
    private void decreaseInventory(WmsOutboundOrder outboundOrder, WmsOutboundDetail detail) {
        WmsInventory inventory = selectInventory(outboundOrder.getWarehouseId(), detail.getLocationId(), detail.getMaterialId(), detail.getBatchId());
        BigDecimal availableQuantity = inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
        WmsInventory update = new WmsInventory();
        update.setInventoryId(inventory.getInventoryId());
        update.setVersion(inventory.getVersion());
        update.setAvailableQuantity(availableQuantity.subtract(detail.getOutboundQuantity()));
        wmsInventoryMapper.updateById(update);
    }

    /**
     * 写入出库库存流水。
     * @param outboundOrder 出库单实体
     * @param detail 出库明细实体
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param operationTime 操作时间
     */
    private void insertOutboundMovement(WmsOutboundOrder outboundOrder, WmsOutboundDetail detail,
                                        Long operatorId, String remark, LocalDateTime operationTime) {
        WmsStockMovement movement = new WmsStockMovement();
        movement.setMovementNo("MV" + System.currentTimeMillis() + detail.getDetailId());
        movement.setMovementType("OUTBOUND");
        movement.setSourceOrderType("OUTBOUND_ORDER");
        movement.setSourceOrderId(outboundOrder.getOutboundId());
        movement.setMaterialId(detail.getMaterialId());
        movement.setBatchId(detail.getBatchId());
        movement.setQuantity(detail.getOutboundQuantity());
        movement.setDirection("OUT");
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
