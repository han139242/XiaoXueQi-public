package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.wms.WmsInboundDetail;
import com.neuedu.ems.entity.wms.WmsInboundOrder;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.entity.wms.WmsStockMovement;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.mapper.wms.WmsInboundDetailMapper;
import com.neuedu.ems.mapper.wms.WmsInboundOrderMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.mapper.wms.WmsStockMovementMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.wms.WmsInboundOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 入库单管理 Service 实现类。
 */
@Service
public class WmsInboundOrderServiceImpl extends ServiceImpl<WmsInboundOrderMapper, WmsInboundOrder> implements WmsInboundOrderService {

    private static final String APPROVED_STATUS = "APPROVED";

    @Autowired
    private WmsInboundDetailMapper wmsInboundDetailMapper;

    @Autowired
    private WmsInventoryMapper wmsInventoryMapper;

    @Autowired
    private WmsStockMovementMapper wmsStockMovementMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 分页查询入库单列表，支持入库单号、类型、仓库、来源单据和审核状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配入库单号）
     * @param inboundType 入库类型
     * @param warehouseId 仓库ID
     * @param sourceOrderType 来源单据类型
     * @param sourceOrderId 来源单据ID
     * @param auditStatus 审核状态
     * @return 分页结果
     */
    @Override
    public Page<WmsInboundOrder> pageInboundOrders(Integer current, Integer size, String keyword, String inboundType,
                                                   Long warehouseId, String sourceOrderType, Long sourceOrderId, String auditStatus) {
        Page<WmsInboundOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildInboundOrderQuery(keyword, inboundType, warehouseId, sourceOrderType, sourceOrderId, auditStatus));
    }

    /**
     * 查询入库单列表，支持入库单号、类型、仓库、来源单据和审核状态筛选。
     * @param keyword 关键字（匹配入库单号）
     * @param inboundType 入库类型
     * @param warehouseId 仓库ID
     * @param sourceOrderType 来源单据类型
     * @param sourceOrderId 来源单据ID
     * @param auditStatus 审核状态
     * @return 入库单列表
     */
    @Override
    public List<WmsInboundOrder> listInboundOrders(String keyword, String inboundType, Long warehouseId,
                                                   String sourceOrderType, Long sourceOrderId, String auditStatus) {
        return baseMapper.selectList(buildInboundOrderQuery(keyword, inboundType, warehouseId, sourceOrderType, sourceOrderId, auditStatus));
    }

    /**
     * 根据入库单ID查询未删除的入库单详情。
     * @param inboundId 入库单ID
     * @return 入库单实体
     */
    @Override
    public WmsInboundOrder getInboundOrderById(Long inboundId) {
        LambdaQueryWrapper<WmsInboundOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInboundOrder::getInboundId, inboundId)
                .eq(WmsInboundOrder::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增入库单，自动设置删除标志为0。
     * @param inboundOrder 入库单实体
     * @return 是否成功
     */
    @Override
    public boolean saveInboundOrder(WmsInboundOrder inboundOrder) {
        if (inboundOrder.getDeletedFlag() == null) {
            inboundOrder.setDeletedFlag(0);
        }
        return baseMapper.insert(inboundOrder) > 0;
    }

    /**
     * 新增入库单并保存明细。
     * @param inboundOrder 入库单实体
     * @param details 入库明细列表
     * @return 入库单ID
     */
    @Override
    @Transactional
    public Long saveInboundOrderFull(WmsInboundOrder inboundOrder, List<WmsInboundDetail> details) {
        saveInboundOrder(inboundOrder);
        if (details != null) {
            for (WmsInboundDetail detail : details) {
                detail.setInboundId(inboundOrder.getInboundId());
                if (detail.getDeletedFlag() == null) {
                    detail.setDeletedFlag(0);
                }
                wmsInboundDetailMapper.insert(detail);
            }
        }
        return inboundOrder.getInboundId();
    }

    /**
     * 更新入库单信息，不允许修改删除标志。
     * @param inboundOrder 入库单实体
     * @return 是否成功
     */
    @Override
    public boolean updateInboundOrder(WmsInboundOrder inboundOrder) {
        if (inboundOrder.getInboundId() == null) {
            return false;
        }
        WmsInboundOrder existing = getInboundOrderById(inboundOrder.getInboundId());
        if (existing == null || APPROVED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        inboundOrder.setDeletedFlag(null);
        if (inboundOrder.getVersion() == null) {
            inboundOrder.setVersion(existing.getVersion());
        }
        return baseMapper.updateById(inboundOrder) > 0;
    }

    /**
     * 更新入库单并替换明细。
     * @param inboundOrder 入库单实体
     * @param details 入库明细列表
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateInboundOrderFull(WmsInboundOrder inboundOrder, List<WmsInboundDetail> details) {
        if (!updateInboundOrder(inboundOrder)) {
            return false;
        }
        if (details != null) {
            softDeleteDetails(inboundOrder.getInboundId());
            for (WmsInboundDetail detail : details) {
                detail.setDetailId(null);
                detail.setInboundId(inboundOrder.getInboundId());
                if (detail.getDeletedFlag() == null) {
                    detail.setDeletedFlag(0);
                }
                wmsInboundDetailMapper.insert(detail);
            }
        }
        return true;
    }

    /**
     * 根据ID删除入库单和明细（软删除，将删除标志设为1）。
     * @param inboundId 入库单ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean removeInboundOrderById(Long inboundId) {
        WmsInboundOrder existing = getInboundOrderById(inboundId);
        if (existing == null || APPROVED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        WmsInboundOrder inboundOrder = new WmsInboundOrder();
        inboundOrder.setInboundId(inboundId);
        inboundOrder.setDeletedFlag(1);
        inboundOrder.setVersion(existing.getVersion());
        boolean orderRemoved = baseMapper.updateById(inboundOrder) > 0;
        softDeleteDetails(inboundId);
        return orderRemoved;
    }

    /**
     * 确认入库，更新库存余额并写入库存流水。
     * @param inboundId 入库单ID
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean confirmInboundOrder(Long inboundId, Long operatorId, String remark) {
        WmsInboundOrder inboundOrder = getInboundOrderById(inboundId);
        if (inboundOrder == null) {
            return false;
        }
        if (APPROVED_STATUS.equals(inboundOrder.getAuditStatus())) {
            return false;
        }
        List<WmsInboundDetail> details = listActiveDetails(inboundId);
        if (details.isEmpty()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        for (WmsInboundDetail detail : details) {
            increaseInventory(inboundOrder, detail);
            insertInboundMovement(inboundOrder, detail, operatorId, remark, now);
        }
        WmsInboundOrder update = new WmsInboundOrder();
        update.setInboundId(inboundId);
        update.setVersion(inboundOrder.getVersion());
        update.setAuditStatus(APPROVED_STATUS);
        update.setInboundTime(now);
        update.setRemark(StringUtils.hasText(remark) ? remark : inboundOrder.getRemark());
        boolean updated = baseMapper.updateById(update) > 0;
        if (updated) {
            insertStatusChangeLog(inboundOrder, operatorId, remark, now);
            insertOperationLog(inboundOrder, operatorId, remark, now);
        }
        return updated;
    }

    /**
     * 构建入库单查询条件，默认过滤已删除记录。
     * @param keyword 关键字（匹配入库单号）
     * @param inboundType 入库类型
     * @param warehouseId 仓库ID
     * @param sourceOrderType 来源单据类型
     * @param sourceOrderId 来源单据ID
     * @param auditStatus 审核状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsInboundOrder> buildInboundOrderQuery(String keyword, String inboundType,
                                                                       Long warehouseId, String sourceOrderType,
                                                                       Long sourceOrderId, String auditStatus) {
        LambdaQueryWrapper<WmsInboundOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInboundOrder::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), WmsInboundOrder::getInboundNo, keyword)
                .eq(StringUtils.hasText(inboundType), WmsInboundOrder::getInboundType, inboundType)
                .eq(warehouseId != null, WmsInboundOrder::getWarehouseId, warehouseId)
                .eq(StringUtils.hasText(sourceOrderType), WmsInboundOrder::getSourceOrderType, sourceOrderType)
                .eq(sourceOrderId != null, WmsInboundOrder::getSourceOrderId, sourceOrderId)
                .eq(StringUtils.hasText(auditStatus), WmsInboundOrder::getAuditStatus, auditStatus)
                .orderByDesc(WmsInboundOrder::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 软删除指定入库单的所有明细。
     * @param inboundId 入库单ID
     */
    private void softDeleteDetails(Long inboundId) {
        List<WmsInboundDetail> details = listActiveDetails(inboundId);
        for (WmsInboundDetail detail : details) {
            WmsInboundDetail update = new WmsInboundDetail();
            update.setDetailId(detail.getDetailId());
            update.setDeletedFlag(1);
            wmsInboundDetailMapper.updateById(update);
        }
    }

    /**
     * 查询指定入库单的未删除明细。
     * @param inboundId 入库单ID
     * @return 入库明细列表
     */
    private List<WmsInboundDetail> listActiveDetails(Long inboundId) {
        LambdaQueryWrapper<WmsInboundDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInboundDetail::getInboundId, inboundId)
                .eq(WmsInboundDetail::getDeletedFlag, 0);
        return wmsInboundDetailMapper.selectList(queryWrapper);
    }

    /**
     * 根据入库明细增加库存余额。
     * @param inboundOrder 入库单实体
     * @param detail 入库明细实体
     */
    private void increaseInventory(WmsInboundOrder inboundOrder, WmsInboundDetail detail) {
        LambdaQueryWrapper<WmsInventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInventory::getWarehouseId, inboundOrder.getWarehouseId())
                .eq(WmsInventory::getLocationId, detail.getLocationId())
                .eq(WmsInventory::getMaterialId, detail.getMaterialId())
                .eq(WmsInventory::getBatchId, detail.getBatchId())
                .eq(WmsInventory::getDeletedFlag, 0);
        WmsInventory inventory = wmsInventoryMapper.selectOne(queryWrapper);
        BigDecimal inboundQuantity = detail.getInboundQuantity() == null ? BigDecimal.ZERO : detail.getInboundQuantity();
        if (inventory == null) {
            inventory = new WmsInventory();
            inventory.setWarehouseId(inboundOrder.getWarehouseId());
            inventory.setLocationId(detail.getLocationId());
            inventory.setMaterialId(detail.getMaterialId());
            inventory.setBatchId(detail.getBatchId());
            inventory.setAvailableQuantity(inboundQuantity);
            inventory.setLockedQuantity(BigDecimal.ZERO);
            inventory.setInspectionQuantity(BigDecimal.ZERO);
            inventory.setDeletedFlag(0);
            wmsInventoryMapper.insert(inventory);
        } else {
            WmsInventory update = new WmsInventory();
            update.setInventoryId(inventory.getInventoryId());
            update.setVersion(inventory.getVersion());
            BigDecimal currentAvailable = inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
            update.setAvailableQuantity(currentAvailable.add(inboundQuantity));
            wmsInventoryMapper.updateById(update);
        }
    }

    /**
     * 写入入库库存流水。
     * @param inboundOrder 入库单实体
     * @param detail 入库明细实体
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param operationTime 操作时间
     */
    private void insertInboundMovement(WmsInboundOrder inboundOrder, WmsInboundDetail detail,
                                       Long operatorId, String remark, LocalDateTime operationTime) {
        WmsStockMovement movement = new WmsStockMovement();
        movement.setMovementNo("MV" + System.currentTimeMillis() + detail.getDetailId());
        movement.setMovementType("INBOUND");
        movement.setSourceOrderType("INBOUND_ORDER");
        movement.setSourceOrderId(inboundOrder.getInboundId());
        movement.setMaterialId(detail.getMaterialId());
        movement.setBatchId(detail.getBatchId());
        movement.setQuantity(detail.getInboundQuantity());
        movement.setDirection("IN");
        movement.setOperationTime(operationTime);
        movement.setCreatedBy(operatorId);
        movement.setRemark(remark);
        movement.setDeletedFlag(0);
        wmsStockMovementMapper.insert(movement);
    }

    /**
     * 记录入库单确认的状态变更日志。
     * @param inboundOrder 入库单实体
     * @param operatorId 操作人ID
     * @param remark 变更备注
     * @param changedTime 变更时间
     */
    private void insertStatusChangeLog(WmsInboundOrder inboundOrder, Long operatorId, String remark, LocalDateTime changedTime) {
        SysStatusChangeLog statusLog = new SysStatusChangeLog();
        statusLog.setBizType("WMS_INBOUND_ORDER");
        statusLog.setBizId(inboundOrder.getInboundId());
        statusLog.setOldStatus(inboundOrder.getAuditStatus());
        statusLog.setNewStatus(APPROVED_STATUS);
        statusLog.setChangeReason("CONFIRM_INBOUND");
        statusLog.setChangedBy(operatorId);
        statusLog.setChangedTime(changedTime);
        statusLog.setCreatedBy(operatorId);
        statusLog.setCreatedTime(changedTime);
        statusLog.setRemark(remark);
        statusLog.setDeletedFlag(0);
        sysStatusChangeLogMapper.insert(statusLog);
    }

    /**
     * 记录入库单确认的操作日志。
     * @param inboundOrder 入库单实体
     * @param operatorId 操作人ID
     * @param remark 操作备注
     * @param operationTime 操作时间
     */
    private void insertOperationLog(WmsInboundOrder inboundOrder, Long operatorId, String remark, LocalDateTime operationTime) {
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setUserId(operatorId);
        operationLog.setOperationType("CONFIRM_INBOUND");
        operationLog.setModuleCode("wms_inbound_order");
        operationLog.setBizId(inboundOrder.getInboundId());
        operationLog.setOperationTime(operationTime);
        operationLog.setCreatedBy(operatorId);
        operationLog.setCreatedTime(operationTime);
        operationLog.setRemark(remark);
        operationLog.setDeletedFlag(0);
        sysOperationLogMapper.insert(operationLog);
    }
}
