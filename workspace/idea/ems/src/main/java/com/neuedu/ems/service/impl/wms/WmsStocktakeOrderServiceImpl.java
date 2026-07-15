package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.entity.wms.WmsStockMovement;
import com.neuedu.ems.entity.wms.WmsStocktakeDetail;
import com.neuedu.ems.entity.wms.WmsStocktakeOrder;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.mapper.wms.WmsStockMovementMapper;
import com.neuedu.ems.mapper.wms.WmsStocktakeDetailMapper;
import com.neuedu.ems.mapper.wms.WmsStocktakeOrderMapper;
import com.neuedu.ems.service.wms.WmsStocktakeOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 盘点单管理 Service 实现类。
 */
@Service
public class WmsStocktakeOrderServiceImpl extends ServiceImpl<WmsStocktakeOrderMapper, WmsStocktakeOrder> implements WmsStocktakeOrderService {

    private static final String DRAFT_STATUS = BusinessStatus.Document.DRAFT;
    private static final String FINISHED_STATUS = BusinessStatus.Production.FINISHED;

    @Autowired
    private WmsStocktakeDetailMapper wmsStocktakeDetailMapper;

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
     * 分页查询盘点单，支持单号、仓库、盘点类型和盘点状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配盘点单号
     * @param warehouseId 仓库ID
     * @param stocktakeType 盘点类型
     * @param stocktakeStatus 盘点状态
     * @return 盘点单分页结果
     */
    @Override
    public Page<WmsStocktakeOrder> pageStocktakeOrders(Integer current, Integer size, String keyword, Long warehouseId,
                                                       String stocktakeType, String stocktakeStatus) {
        Page<WmsStocktakeOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildStocktakeOrderQuery(keyword, warehouseId, stocktakeType, stocktakeStatus));
    }

    /**
     * 查询盘点单列表，支持单号、仓库、盘点类型和盘点状态筛选。
     * @param keyword 关键字，匹配盘点单号
     * @param warehouseId 仓库ID
     * @param stocktakeType 盘点类型
     * @param stocktakeStatus 盘点状态
     * @return 盘点单列表
     */
    @Override
    public List<WmsStocktakeOrder> listStocktakeOrders(String keyword, Long warehouseId, String stocktakeType, String stocktakeStatus) {
        return baseMapper.selectList(buildStocktakeOrderQuery(keyword, warehouseId, stocktakeType, stocktakeStatus));
    }

    /**
     * 根据盘点单ID查询未删除的盘点单详情。
     * @param stocktakeId 盘点单ID
     * @return 盘点单实体
     */
    @Override
    public WmsStocktakeOrder getStocktakeOrderById(Long stocktakeId) {
        LambdaQueryWrapper<WmsStocktakeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsStocktakeOrder::getStocktakeId, stocktakeId)
                .eq(WmsStocktakeOrder::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增盘点单，自动设置删除标记为0。
     * @param stocktakeOrder 盘点单实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean saveStocktakeOrder(WmsStocktakeOrder stocktakeOrder) {
        if (!StringUtils.hasText(stocktakeOrder.getStocktakeStatus())) {
            stocktakeOrder.setStocktakeStatus(DRAFT_STATUS);
        }
        if (stocktakeOrder.getDeletedFlag() == null) {
            stocktakeOrder.setDeletedFlag(0);
        }
        boolean saved = baseMapper.insert(stocktakeOrder) > 0;
        if (saved) {
            responsibilityNotificationService.publishStocktakePending(stocktakeOrder, stocktakeOrder.getCreatedBy());
        }
        return saved;
    }

    /**
     * 新增盘点单并保存盘点明细。
     * @param stocktakeOrder 盘点单实体
     * @param details 盘点明细列表
     * @return 盘点单ID
     */
    @Override
    @Transactional
    public Long saveStocktakeOrderFull(WmsStocktakeOrder stocktakeOrder, List<WmsStocktakeDetail> details) {
        saveStocktakeOrder(stocktakeOrder);
        if (details != null) {
            for (WmsStocktakeDetail detail : details) {
                detail.setStocktakeId(stocktakeOrder.getStocktakeId());
                setDefaultStocktakeQuantities(detail);
                if (detail.getDeletedFlag() == null) {
                    detail.setDeletedFlag(0);
                }
                wmsStocktakeDetailMapper.insert(detail);
            }
        }
        return stocktakeOrder.getStocktakeId();
    }

    /**
     * 更新盘点单，已完成盘点单不允许普通修改。
     * @param stocktakeOrder 盘点单实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateStocktakeOrder(WmsStocktakeOrder stocktakeOrder) {
        if (stocktakeOrder.getStocktakeId() == null) {
            return false;
        }
        WmsStocktakeOrder existing = getStocktakeOrderById(stocktakeOrder.getStocktakeId());
        if (existing == null || FINISHED_STATUS.equals(existing.getStocktakeStatus())) {
            return false;
        }
        stocktakeOrder.setDeletedFlag(null);
        if (stocktakeOrder.getVersion() == null) {
            stocktakeOrder.setVersion(existing.getVersion());
        }
        boolean warehouseChanged = stocktakeOrder.getWarehouseId() != null
                && !Objects.equals(existing.getWarehouseId(), stocktakeOrder.getWarehouseId());
        boolean updated = baseMapper.updateById(stocktakeOrder) > 0;
        if (updated && warehouseChanged) {
            responsibilityNotificationService.cancelStocktakePending(existing.getStocktakeId(), stocktakeOrder.getUpdatedBy());
            responsibilityNotificationService.publishStocktakePending(
                    getStocktakeOrderById(existing.getStocktakeId()), stocktakeOrder.getUpdatedBy());
        }
        return updated;
    }

    /**
     * 更新盘点单并替换盘点明细，已完成盘点单不允许替换明细。
     * @param stocktakeOrder 盘点单实体
     * @param details 盘点明细列表
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateStocktakeOrderFull(WmsStocktakeOrder stocktakeOrder, List<WmsStocktakeDetail> details) {
        if (!updateStocktakeOrder(stocktakeOrder)) {
            return false;
        }
        if (details != null) {
            softDeleteDetails(stocktakeOrder.getStocktakeId());
            for (WmsStocktakeDetail detail : details) {
                detail.setDetailId(null);
                detail.setStocktakeId(stocktakeOrder.getStocktakeId());
                setDefaultStocktakeQuantities(detail);
                if (detail.getDeletedFlag() == null) {
                    detail.setDeletedFlag(0);
                }
                wmsStocktakeDetailMapper.insert(detail);
            }
        }
        return true;
    }

    /**
     * 根据ID软删除盘点单和明细，已完成盘点单不允许删除。
     * @param stocktakeId 盘点单ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean removeStocktakeOrderById(Long stocktakeId) {
        WmsStocktakeOrder existing = getStocktakeOrderById(stocktakeId);
        if (existing == null || FINISHED_STATUS.equals(existing.getStocktakeStatus())) {
            return false;
        }
        WmsStocktakeOrder stocktakeOrder = new WmsStocktakeOrder();
        stocktakeOrder.setStocktakeId(stocktakeId);
        stocktakeOrder.setDeletedFlag(1);
        stocktakeOrder.setVersion(existing.getVersion());
        boolean orderRemoved = baseMapper.updateById(stocktakeOrder) > 0;
        softDeleteDetails(stocktakeId);
        if (orderRemoved) {
            responsibilityNotificationService.cancelStocktakePending(stocktakeId, existing.getUpdatedBy());
        }
        return orderRemoved;
    }

    /**
     * 完成盘点，根据盘点差异调整库存并写入库存流水。
     * @param stocktakeId 盘点单ID
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean finishStocktakeOrder(Long stocktakeId, Long operatorId, String remark) {
        WmsStocktakeOrder stocktakeOrder = getStocktakeOrderById(stocktakeId);
        if (stocktakeOrder == null || FINISHED_STATUS.equals(stocktakeOrder.getStocktakeStatus())) {
            return false;
        }
        List<WmsStocktakeDetail> details = listActiveDetails(stocktakeId);
        if (details.isEmpty() || !canAdjustInventory(stocktakeOrder, details)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        for (WmsStocktakeDetail detail : details) {
            BigDecimal differenceQuantity = calculateDifference(detail);
            updateDetailDifference(detail, differenceQuantity);
            if (differenceQuantity.compareTo(BigDecimal.ZERO) != 0) {
                adjustInventory(stocktakeOrder, detail, differenceQuantity);
                insertStocktakeMovement(stocktakeOrder, detail, differenceQuantity, operatorId, remark, now);
            }
        }
        WmsStocktakeOrder update = new WmsStocktakeOrder();
        update.setStocktakeId(stocktakeId);
        update.setVersion(stocktakeOrder.getVersion());
        update.setStocktakeStatus(FINISHED_STATUS);
        update.setFinishedTime(now);
        update.setRemark(StringUtils.hasText(remark) ? remark : stocktakeOrder.getRemark());
        boolean updated = baseMapper.updateById(update) > 0;
        if (updated) {
            insertStatusChangeLog(stocktakeOrder.getStocktakeId(), stocktakeOrder.getStocktakeStatus(), FINISHED_STATUS,
                    "WMS_STOCKTAKE_ORDER", "FINISH_STOCKTAKE", operatorId, remark, now);
            insertOperationLog(stocktakeOrder.getStocktakeId(), "FINISH_STOCKTAKE", "wms_stocktake_order", operatorId, remark, now);
            responsibilityNotificationService.cancelStocktakePending(stocktakeId, operatorId);
            responsibilityNotificationService.publishStocktakeCompleted(stocktakeOrder, operatorId);
        }
        return updated;
    }

    /**
     * 构建盘点单查询条件，默认过滤已删除记录。
     * @param keyword 关键字，匹配盘点单号
     * @param warehouseId 仓库ID
     * @param stocktakeType 盘点类型
     * @param stocktakeStatus 盘点状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsStocktakeOrder> buildStocktakeOrderQuery(String keyword, Long warehouseId,
                                                                           String stocktakeType, String stocktakeStatus) {
        LambdaQueryWrapper<WmsStocktakeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsStocktakeOrder::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), WmsStocktakeOrder::getStocktakeNo, keyword)
                .eq(warehouseId != null, WmsStocktakeOrder::getWarehouseId, warehouseId)
                .eq(StringUtils.hasText(stocktakeType), WmsStocktakeOrder::getStocktakeType, stocktakeType)
                .eq(StringUtils.hasText(stocktakeStatus), WmsStocktakeOrder::getStocktakeStatus, stocktakeStatus)
                .orderByDesc(WmsStocktakeOrder::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 软删除指定盘点单的所有明细。
     * @param stocktakeId 盘点单ID
     */
    private void softDeleteDetails(Long stocktakeId) {
        List<WmsStocktakeDetail> details = listActiveDetails(stocktakeId);
        for (WmsStocktakeDetail detail : details) {
            WmsStocktakeDetail update = new WmsStocktakeDetail();
            update.setDetailId(detail.getDetailId());
            update.setDeletedFlag(1);
            wmsStocktakeDetailMapper.updateById(update);
        }
    }

    /**
     * 查询指定盘点单的未删除明细。
     * @param stocktakeId 盘点单ID
     * @return 盘点明细列表
     */
    private List<WmsStocktakeDetail> listActiveDetails(Long stocktakeId) {
        LambdaQueryWrapper<WmsStocktakeDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsStocktakeDetail::getStocktakeId, stocktakeId)
                .eq(WmsStocktakeDetail::getDeletedFlag, 0);
        return wmsStocktakeDetailMapper.selectList(queryWrapper);
    }

    /**
     * 设置盘点明细数量默认值并计算差异。
     * @param detail 盘点明细实体
     */
    private void setDefaultStocktakeQuantities(WmsStocktakeDetail detail) {
        BigDecimal bookQuantity = detail.getBookQuantity() == null ? BigDecimal.ZERO : detail.getBookQuantity();
        BigDecimal actualQuantity = detail.getActualQuantity() == null ? BigDecimal.ZERO : detail.getActualQuantity();
        detail.setBookQuantity(bookQuantity);
        detail.setActualQuantity(actualQuantity);
        detail.setDifferenceQuantity(actualQuantity.subtract(bookQuantity));
    }

    /**
     * 计算盘点差异数量。
     * @param detail 盘点明细实体
     * @return 差异数量，正数为盘盈，负数为盘亏
     */
    private BigDecimal calculateDifference(WmsStocktakeDetail detail) {
        BigDecimal bookQuantity = detail.getBookQuantity() == null ? BigDecimal.ZERO : detail.getBookQuantity();
        BigDecimal actualQuantity = detail.getActualQuantity() == null ? BigDecimal.ZERO : detail.getActualQuantity();
        return actualQuantity.subtract(bookQuantity);
    }

    /**
     * 校验盘点差异是否可以调整库存。
     * @param stocktakeOrder 盘点单实体
     * @param details 盘点明细列表
     * @return 是否可以调整
     */
    private boolean canAdjustInventory(WmsStocktakeOrder stocktakeOrder, List<WmsStocktakeDetail> details) {
        for (WmsStocktakeDetail detail : details) {
            BigDecimal differenceQuantity = calculateDifference(detail);
            WmsInventory inventory = selectInventory(stocktakeOrder.getWarehouseId(), detail.getLocationId(), detail.getMaterialId(), detail.getBatchId());
            if (differenceQuantity.compareTo(BigDecimal.ZERO) < 0) {
                BigDecimal availableQuantity = inventory == null || inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
                if (availableQuantity.add(differenceQuantity).compareTo(BigDecimal.ZERO) < 0) {
                    return false;
                }
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
     * 回写盘点明细差异数量。
     * @param detail 盘点明细实体
     * @param differenceQuantity 差异数量
     */
    private void updateDetailDifference(WmsStocktakeDetail detail, BigDecimal differenceQuantity) {
        WmsStocktakeDetail update = new WmsStocktakeDetail();
        update.setDetailId(detail.getDetailId());
        update.setDifferenceQuantity(differenceQuantity);
        wmsStocktakeDetailMapper.updateById(update);
    }

    /**
     * 根据盘点差异调整库存余额。
     * @param stocktakeOrder 盘点单实体
     * @param detail 盘点明细实体
     * @param differenceQuantity 差异数量
     */
    private void adjustInventory(WmsStocktakeOrder stocktakeOrder, WmsStocktakeDetail detail, BigDecimal differenceQuantity) {
        WmsInventory inventory = selectInventory(stocktakeOrder.getWarehouseId(), detail.getLocationId(), detail.getMaterialId(), detail.getBatchId());
        if (inventory == null) {
            inventory = new WmsInventory();
            inventory.setWarehouseId(stocktakeOrder.getWarehouseId());
            inventory.setLocationId(detail.getLocationId());
            inventory.setMaterialId(detail.getMaterialId());
            inventory.setBatchId(detail.getBatchId());
            inventory.setAvailableQuantity(differenceQuantity);
            inventory.setLockedQuantity(BigDecimal.ZERO);
            inventory.setInspectionQuantity(BigDecimal.ZERO);
            inventory.setDeletedFlag(0);
            wmsInventoryMapper.insert(inventory);
            return;
        }
        BigDecimal availableQuantity = inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
        WmsInventory update = new WmsInventory();
        update.setInventoryId(inventory.getInventoryId());
        update.setVersion(inventory.getVersion());
        update.setAvailableQuantity(availableQuantity.add(differenceQuantity));
        wmsInventoryMapper.updateById(update);
    }

    /**
     * 写入盘点库存流水。
     * @param stocktakeOrder 盘点单实体
     * @param detail 盘点明细实体
     * @param differenceQuantity 差异数量
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param operationTime 操作时间
     */
    private void insertStocktakeMovement(WmsStocktakeOrder stocktakeOrder, WmsStocktakeDetail detail, BigDecimal differenceQuantity,
                                         Long operatorId, String remark, LocalDateTime operationTime) {
        WmsStockMovement movement = new WmsStockMovement();
        movement.setMovementNo("MV" + System.currentTimeMillis() + detail.getDetailId());
        movement.setMovementType("STOCKTAKE");
        movement.setSourceOrderType("STOCKTAKE_ORDER");
        movement.setSourceOrderId(stocktakeOrder.getStocktakeId());
        movement.setMaterialId(detail.getMaterialId());
        movement.setBatchId(detail.getBatchId());
        movement.setQuantity(differenceQuantity.abs());
        movement.setDirection(differenceQuantity.compareTo(BigDecimal.ZERO) > 0 ? "IN" : "OUT");
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
