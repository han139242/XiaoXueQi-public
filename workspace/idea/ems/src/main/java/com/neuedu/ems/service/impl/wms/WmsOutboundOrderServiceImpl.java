package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
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
import com.neuedu.ems.service.wms.WmsOutboundOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 出库单管理 Service 实现类。
 */
@Service
public class WmsOutboundOrderServiceImpl extends ServiceImpl<WmsOutboundOrderMapper, WmsOutboundOrder> implements WmsOutboundOrderService {

    private static final String SUBMITTED_STATUS = BusinessStatus.Document.SUBMITTED;
    private static final String APPROVED_STATUS = BusinessStatus.Document.APPROVED;
    private static final Set<String> RELEASED_QUALITY_STATUSES = Set.of(
            BusinessStatus.Quality.PASS, BusinessStatus.Quality.CONCESSION);
    private static final Set<String> ISSUABLE_BATCH_STATUSES = Set.of("AVAILABLE", "ENABLE");

    @Autowired
    private WmsOutboundDetailMapper wmsOutboundDetailMapper;

    @Autowired
    private WmsInventoryMapper wmsInventoryMapper;

    @Autowired
    private WmsMaterialBatchMapper wmsMaterialBatchMapper;

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
        outboundOrder.setOutboundId(null);
        outboundOrder.setAuditStatus(SUBMITTED_STATUS);
        outboundOrder.setOutboundTime(null);
        outboundOrder.setDeletedFlag(0);
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
        if (details == null || details.isEmpty()) {
            throw business(ResultCode.PARAM_ERROR, "出库单至少需要一条明细");
        }
        if (!saveOutboundOrder(outboundOrder)) {
            throw business(ResultCode.FAILED, "出库单保存失败");
        }
        for (WmsOutboundDetail detail : details) {
            detail.setDetailId(null);
            detail.setOutboundId(outboundOrder.getOutboundId());
            detail.setDeletedFlag(0);
            if (wmsOutboundDetailMapper.insert(detail) == 0) {
                throw business(ResultCode.FAILED, "出库明细保存失败");
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
                detail.setDeletedFlag(0);
                if (wmsOutboundDetailMapper.insert(detail) == 0) {
                    throw business(ResultCode.FAILED, "出库明细保存失败");
                }
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
        if (baseMapper.updateById(outboundOrder) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "出库单已被其他用户修改，请刷新后重试");
        }
        softDeleteDetails(outboundId);
        responsibilityNotificationService.cancelOutboundPending(outboundId, existing.getUpdatedBy());
        return true;
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
        if (operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "当前登录用户不能为空");
        }
        WmsOutboundOrder outboundOrder = getOutboundOrderById(outboundId);
        if (outboundOrder == null) {
            throw business(ResultCode.NOT_FOUND, "出库单不存在或已删除");
        }
        if (!SUBMITTED_STATUS.equals(outboundOrder.getAuditStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有 SUBMITTED 状态的出库单可以确认");
        }
        if (outboundOrder.getWarehouseId() == null) {
            throw business(ResultCode.PARAM_ERROR, "出库仓库不能为空");
        }
        List<WmsOutboundDetail> details = listActiveDetails(outboundId);
        if (details.isEmpty()) {
            throw business(ResultCode.BUSINESS_ERROR, "出库单至少需要一条有效明细");
        }
        Map<InventoryKey, InventoryDeduction> deductions = prepareInventoryDeductions(outboundOrder, details);
        LocalDateTime now = LocalDateTime.now();
        WmsOutboundOrder update = new WmsOutboundOrder();
        update.setOutboundId(outboundId);
        update.setVersion(outboundOrder.getVersion());
        update.setAuditStatus(APPROVED_STATUS);
        update.setOutboundTime(now);
        update.setRemark(StringUtils.hasText(remark) ? remark : outboundOrder.getRemark());
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "出库单已被其他用户修改，请刷新后重试");
        }
        for (InventoryDeduction deduction : deductions.values()) {
            decreaseInventory(deduction);
        }
        for (WmsOutboundDetail detail : details) {
            insertOutboundMovement(outboundOrder, detail, operatorId, remark, now);
        }
        insertStatusChangeLog(outboundOrder.getOutboundId(), outboundOrder.getAuditStatus(), APPROVED_STATUS,
                "WMS_OUTBOUND_ORDER", "CONFIRM_OUTBOUND", operatorId, remark, now);
        insertOperationLog(outboundOrder.getOutboundId(), "CONFIRM_OUTBOUND", "wms_outbound_order", operatorId, remark, now);
        responsibilityNotificationService.cancelOutboundPending(outboundId, operatorId);
        responsibilityNotificationService.publishOutboundCompleted(outboundOrder, operatorId);
        return true;
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
            if (wmsOutboundDetailMapper.updateById(update) == 0) {
                throw business(ResultCode.DATA_CONFLICT, "出库明细已被其他用户修改，请刷新后重试");
            }
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
     * @return 按库存唯一键汇总后的扣减计划
     */
    private Map<InventoryKey, InventoryDeduction> prepareInventoryDeductions(
            WmsOutboundOrder outboundOrder, List<WmsOutboundDetail> details) {
        Map<InventoryKey, BigDecimal> requiredQuantities = new LinkedHashMap<>();
        for (WmsOutboundDetail detail : details) {
            if (detail.getDetailId() == null || detail.getMaterialId() == null || detail.getBatchId() == null
                    || detail.getLocationId() == null || detail.getOutboundQuantity() == null
                    || detail.getOutboundQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw business(ResultCode.PARAM_ERROR, "出库明细的物料、批次、库位和正数数量不能为空");
            }
            WmsMaterialBatch batch = getActiveBatch(detail.getBatchId());
            if (batch == null) {
                throw business(ResultCode.NOT_FOUND, "出库明细关联的物料批次不存在或已删除");
            }
            if (!detail.getMaterialId().equals(batch.getMaterialId())) {
                throw business(ResultCode.BUSINESS_ERROR, "出库明细物料与批次物料不一致");
            }
            if (!RELEASED_QUALITY_STATUSES.contains(batch.getQualityStatus())
                    || !ISSUABLE_BATCH_STATUSES.contains(batch.getStatus())) {
                throw business(ResultCode.BUSINESS_ERROR, "批次未放行或不可用，禁止出库");
            }
            InventoryKey key = new InventoryKey(outboundOrder.getWarehouseId(), detail.getLocationId(),
                    detail.getMaterialId(), detail.getBatchId());
            requiredQuantities.merge(key, detail.getOutboundQuantity(), BigDecimal::add);
        }

        Map<InventoryKey, InventoryDeduction> deductions = new LinkedHashMap<>();
        for (Map.Entry<InventoryKey, BigDecimal> entry : requiredQuantities.entrySet()) {
            InventoryKey key = entry.getKey();
            WmsInventory inventory = selectInventory(
                    key.warehouseId(), key.locationId(), key.materialId(), key.batchId());
            BigDecimal availableQuantity = inventory == null || inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
            if (availableQuantity.compareTo(entry.getValue()) < 0) {
                throw business(ResultCode.BUSINESS_ERROR, "出库库存不足，请核对仓库、库位、物料、批次和数量");
            }
            deductions.put(key, new InventoryDeduction(inventory, entry.getValue()));
        }
        return deductions;
    }

    private WmsMaterialBatch getActiveBatch(Long batchId) {
        LambdaQueryWrapper<WmsMaterialBatch> query = new LambdaQueryWrapper<>();
        query.eq(WmsMaterialBatch::getBatchId, batchId)
                .eq(WmsMaterialBatch::getDeletedFlag, 0);
        return wmsMaterialBatchMapper.selectOne(query);
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
     * @param deduction 库存扣减计划
     */
    private void decreaseInventory(InventoryDeduction deduction) {
        WmsInventory inventory = deduction.inventory();
        BigDecimal availableQuantity = inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
        WmsInventory update = new WmsInventory();
        update.setInventoryId(inventory.getInventoryId());
        update.setVersion(inventory.getVersion());
        update.setAvailableQuantity(availableQuantity.subtract(deduction.quantity()));
        if (wmsInventoryMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "库存余额已被其他请求修改，请刷新后重试");
        }
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
        movement.setMovementNo("WMS-OUT-" + outboundOrder.getOutboundId() + "-" + detail.getDetailId());
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
        if (wmsStockMovementMapper.insert(movement) == 0) {
            throw business(ResultCode.FAILED, "出库库存流水写入失败");
        }
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
        if (sysStatusChangeLogMapper.insert(statusLog) == 0) {
            throw business(ResultCode.FAILED, "出库状态日志写入失败");
        }
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
        if (sysOperationLogMapper.insert(operationLog) == 0) {
            throw business(ResultCode.FAILED, "出库操作日志写入失败");
        }
    }

    private BusinessException business(ResultCode code, String message) {
        return new BusinessException(code.getCode(), message);
    }

    private record InventoryKey(Long warehouseId, Long locationId, Long materialId, Long batchId) {
    }

    private record InventoryDeduction(WmsInventory inventory, BigDecimal quantity) {
    }
}
