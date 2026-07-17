package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkOrderMaterial;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.entity.wms.WmsMaterialBatch;
import com.neuedu.ems.entity.wms.WmsOutboundDetail;
import com.neuedu.ems.entity.wms.WmsOutboundOrder;
import com.neuedu.ems.entity.wms.WmsStockMovement;
import com.neuedu.ems.entity.wms.WmsWarehouse;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.mapper.wms.WmsMaterialBatchMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundDetailMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundOrderMapper;
import com.neuedu.ems.mapper.wms.WmsStockMovementMapper;
import com.neuedu.ems.mapper.wms.WmsWarehouseMapper;
import com.neuedu.ems.service.mfg.MfgDispatchOrderService;
import com.neuedu.ems.service.wms.WmsOutboundOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final String MATERIAL_ISSUE_TYPE = "MATERIAL_ISSUE";
    private static final Set<String> RELEASED_QUALITY_STATUSES = Set.of(
            BusinessStatus.Quality.PASS, BusinessStatus.Quality.CONCESSION);
    private static final Set<String> ISSUABLE_BATCH_STATUSES = Set.of("AVAILABLE", "ENABLE");
    private static final DateTimeFormatter ISSUE_NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

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

    @Autowired
    private MfgWorkOrderMaterialMapper workOrderMaterialMapper;

    @Autowired
    private MfgWorkOrderMapper workOrderMapper;

    @Autowired
    private WmsWarehouseMapper wmsWarehouseMapper;

    @Autowired
    private MfgDispatchOrderService mfgDispatchOrderService;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsOutboundOrder generateMaterialIssue(Long workOrderId, Long warehouseId,
                                                   Long operatorId, String remark) {
        if (workOrderId == null || warehouseId == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "生产工单、领料仓库和操作人不能为空");
        }
        MfgWorkOrder workOrder = workOrderMapper.selectOne(new LambdaQueryWrapper<MfgWorkOrder>()
                .eq(MfgWorkOrder::getWorkOrderId, workOrderId)
                .eq(MfgWorkOrder::getDeletedFlag, 0)
                .last("FOR UPDATE"));
        if (workOrder == null) {
            throw business(ResultCode.NOT_FOUND, "生产工单不存在或已删除");
        }
        if (!Set.of(BusinessStatus.Document.RELEASED, BusinessStatus.Production.RUNNING)
                .contains(workOrder.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有已下达或生产中的工单可以发起领料");
        }
        WmsWarehouse warehouse = wmsWarehouseMapper.selectOne(new LambdaQueryWrapper<WmsWarehouse>()
                .eq(WmsWarehouse::getWarehouseId, warehouseId)
                .eq(WmsWarehouse::getStatus, BusinessStatus.Common.ENABLE)
                .eq(WmsWarehouse::getDeletedFlag, 0));
        if (warehouse == null) {
            throw business(ResultCode.BUSINESS_ERROR, "目标领料仓库不存在、已停用或已删除");
        }
        List<WmsOutboundOrder> pendingOrders = baseMapper.selectList(new LambdaQueryWrapper<WmsOutboundOrder>()
                .eq(WmsOutboundOrder::getOutboundType, MATERIAL_ISSUE_TYPE)
                .eq(WmsOutboundOrder::getWorkOrderId, workOrderId)
                .eq(WmsOutboundOrder::getAuditStatus, SUBMITTED_STATUS)
                .eq(WmsOutboundOrder::getDeletedFlag, 0));
        if (pendingOrders.size() > 1) {
            throw business(ResultCode.DATA_CONFLICT, "该工单存在多张待确认生产领料单，请先处理重复数据");
        }
        if (pendingOrders.size() == 1) {
            return pendingOrders.get(0);
        }
        List<MfgWorkOrderMaterial> requirements = workOrderMaterialMapper.selectList(
                new LambdaQueryWrapper<MfgWorkOrderMaterial>()
                        .eq(MfgWorkOrderMaterial::getWorkOrderId, workOrderId)
                        .eq(MfgWorkOrderMaterial::getDeletedFlag, 0)
                        .orderByAsc(MfgWorkOrderMaterial::getRequirementId)
                        .last("FOR UPDATE"));
        Map<Long, BigDecimal> outstandingByMaterial = new LinkedHashMap<>();
        for (MfgWorkOrderMaterial requirement : requirements) {
            BigDecimal outstanding = zero(requirement.getRequiredQuantity())
                    .subtract(zero(requirement.getIssuedQuantity())).max(BigDecimal.ZERO);
            if (requirement.getMaterialId() != null && outstanding.signum() > 0) {
                outstandingByMaterial.merge(requirement.getMaterialId(), outstanding, BigDecimal::add);
            }
        }
        if (outstandingByMaterial.isEmpty()) {
            throw business(ResultCode.BUSINESS_ERROR, "该工单物料已全部足额领用，无需重复发起领料");
        }
        List<WmsOutboundDetail> details = allocateMaterialIssueDetails(
                warehouseId, outstandingByMaterial, workOrderId, operatorId);
        if (details.isEmpty()) {
            throw business(ResultCode.BUSINESS_ERROR, "目标仓库没有可用于该工单的已放行库存，不能生成领料单");
        }
        WmsOutboundOrder order = new WmsOutboundOrder();
        order.setOutboundNo("MI-WO" + workOrderId + "-" + LocalDateTime.now().format(ISSUE_NUMBER_TIME));
        order.setOutboundType(MATERIAL_ISSUE_TYPE);
        order.setWarehouseId(warehouseId);
        order.setWorkOrderId(workOrderId);
        order.setApplicantId(operatorId);
        order.setRemark(StringUtils.hasText(remark) ? remark.trim() : "从生产工单用料需求自动生成");
        order.setCreatedBy(operatorId);
        order.setUpdatedBy(operatorId);
        saveOutboundOrderFull(order, details);
        return order;
    }

    private List<WmsOutboundDetail> allocateMaterialIssueDetails(Long warehouseId,
                                                                  Map<Long, BigDecimal> outstandingByMaterial,
                                                                  Long workOrderId, Long operatorId) {
        List<WmsOutboundDetail> details = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> request : outstandingByMaterial.entrySet()) {
            List<MaterialIssueSource> sources = new ArrayList<>();
            List<WmsInventory> inventories = wmsInventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseId, warehouseId)
                    .eq(WmsInventory::getMaterialId, request.getKey())
                    .gt(WmsInventory::getAvailableQuantity, BigDecimal.ZERO)
                    .eq(WmsInventory::getDeletedFlag, 0));
            for (WmsInventory inventory : inventories) {
                if (inventory.getBatchId() == null || inventory.getLocationId() == null) {
                    continue;
                }
                WmsMaterialBatch batch = getActiveBatch(inventory.getBatchId());
                if (batch != null && Objects.equals(batch.getMaterialId(), request.getKey())
                        && RELEASED_QUALITY_STATUSES.contains(batch.getQualityStatus())
                        && ISSUABLE_BATCH_STATUSES.contains(batch.getStatus())) {
                    sources.add(new MaterialIssueSource(inventory, batch));
                }
            }
            sources.sort(Comparator
                    .comparing((MaterialIssueSource source) -> source.batch().getReceivedDate(),
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(source -> source.batch().getBatchId()));
            BigDecimal remaining = request.getValue();
            for (MaterialIssueSource source : sources) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal allocated = remaining.min(zero(source.inventory().getAvailableQuantity()));
                if (allocated.signum() <= 0) {
                    continue;
                }
                WmsOutboundDetail detail = new WmsOutboundDetail();
                detail.setMaterialId(request.getKey());
                detail.setBatchId(source.batch().getBatchId());
                detail.setLocationId(source.inventory().getLocationId());
                detail.setOutboundQuantity(allocated);
                detail.setRemark("工单 " + workOrderId + " 按 FIFO 自动预分配");
                detail.setCreatedBy(operatorId);
                detail.setUpdatedBy(operatorId);
                details.add(detail);
                remaining = remaining.subtract(allocated);
            }
        }
        return details;
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
        if (outboundOrder != null && MATERIAL_ISSUE_TYPE.equals(outboundOrder.getOutboundType())) {
            throw business(ResultCode.BUSINESS_ERROR, "生产领料必须从工单用料需求发起并同时提交领料明细");
        }
        return saveOutboundOrderHeader(outboundOrder);
    }

    private boolean saveOutboundOrderHeader(WmsOutboundOrder outboundOrder) {
        if (outboundOrder == null || outboundOrder.getWarehouseId() == null) {
            throw business(ResultCode.PARAM_ERROR, "出库单和出库仓库不能为空");
        }
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
        prepareMaterialIssueUpdates(outboundOrder, details);
        if (!saveOutboundOrderHeader(outboundOrder)) {
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
        if (existing == null || !SUBMITTED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        outboundOrder.setDeletedFlag(null);
        outboundOrder.setAuditStatus(null);
        outboundOrder.setOutboundTime(null);
        outboundOrder.setOutboundType(null);
        outboundOrder.setWorkOrderId(null);
        outboundOrder.setApplicantId(null);
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
        WmsOutboundOrder existing = outboundOrder == null ? null : getOutboundOrderById(outboundOrder.getOutboundId());
        if (existing != null && MATERIAL_ISSUE_TYPE.equals(existing.getOutboundType()) && details != null) {
            prepareMaterialIssueUpdates(existing, details);
        }
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
        if (existing == null || !SUBMITTED_STATUS.equals(existing.getAuditStatus())) {
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawOutboundOrder(Long outboundId, Long operatorId, String remark) {
        WmsOutboundOrder existing = getOutboundOrderById(outboundId);
        if (existing == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "出库单和操作人不能为空");
        }
        remark = StringUtils.hasText(remark) ? remark.trim() : "用户主动撤回，未填写补充原因";
        if (!SUBMITTED_STATUS.equals(existing.getAuditStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有未确认出库单可以撤回");
        }
        WmsOutboundOrder update = new WmsOutboundOrder();
        update.setOutboundId(outboundId); update.setVersion(existing.getVersion());
        update.setAuditStatus(BusinessStatus.Document.CLOSED); update.setUpdatedBy(operatorId); update.setUpdatedTime(LocalDateTime.now());
        if (baseMapper.updateById(update) == 0) throw business(ResultCode.DATA_CONFLICT, "出库单已被并发修改");
        LocalDateTime now = LocalDateTime.now();
        insertStatusChangeLog(outboundId, existing.getAuditStatus(), BusinessStatus.Document.CLOSED,
                "WMS_OUTBOUND_ORDER", "WITHDRAW_OUTBOUND", operatorId, remark, now);
        insertOperationLog(outboundId, "WITHDRAW_OUTBOUND", "wms_outbound_order", operatorId, remark, now);
        responsibilityNotificationService.cancelOutboundPending(outboundId, operatorId);
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
        List<MaterialIssueUpdate> materialIssueUpdates = prepareMaterialIssueUpdates(outboundOrder, details);
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
        applyMaterialIssueUpdates(materialIssueUpdates, operatorId, now);
        if (MATERIAL_ISSUE_TYPE.equals(outboundOrder.getOutboundType())) {
            mfgDispatchOrderService.refreshMaterialReadiness(outboundOrder.getWorkOrderId(), operatorId);
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

    private List<MaterialIssueUpdate> prepareMaterialIssueUpdates(WmsOutboundOrder outboundOrder,
                                                                   List<WmsOutboundDetail> details) {
        if (outboundOrder == null || !MATERIAL_ISSUE_TYPE.equals(outboundOrder.getOutboundType())) {
            return List.of();
        }
        if (outboundOrder.getWorkOrderId() == null) {
            throw business(ResultCode.PARAM_ERROR, "生产领料必须关联生产工单");
        }
        List<MfgWorkOrderMaterial> requirements = workOrderMaterialMapper.selectList(
                new LambdaQueryWrapper<MfgWorkOrderMaterial>()
                        .eq(MfgWorkOrderMaterial::getWorkOrderId, outboundOrder.getWorkOrderId())
                        .eq(MfgWorkOrderMaterial::getDeletedFlag, 0)
                        .orderByAsc(MfgWorkOrderMaterial::getRequirementId)
                        .last("FOR UPDATE"));
        if (requirements.isEmpty()) {
            throw business(ResultCode.BUSINESS_ERROR, "生产工单没有有效用料需求，不能创建或确认领料");
        }
        Map<Long, BigDecimal> requestedByMaterial = new LinkedHashMap<>();
        for (WmsOutboundDetail detail : details == null ? List.<WmsOutboundDetail>of() : details) {
            if (detail.getMaterialId() == null || detail.getOutboundQuantity() == null
                    || detail.getOutboundQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw business(ResultCode.PARAM_ERROR, "生产领料明细必须包含物料和正数数量");
            }
            requestedByMaterial.merge(detail.getMaterialId(), detail.getOutboundQuantity(), BigDecimal::add);
        }
        List<MaterialIssueUpdate> updates = new java.util.ArrayList<>();
        for (Map.Entry<Long, BigDecimal> request : requestedByMaterial.entrySet()) {
            BigDecimal remaining = request.getValue();
            for (MfgWorkOrderMaterial requirement : requirements) {
                if (!Objects.equals(requirement.getMaterialId(), request.getKey()) || remaining.signum() <= 0) {
                    continue;
                }
                BigDecimal required = zero(requirement.getRequiredQuantity());
                BigDecimal issued = zero(requirement.getIssuedQuantity());
                BigDecimal outstanding = required.subtract(issued).max(BigDecimal.ZERO);
                BigDecimal applied = remaining.min(outstanding);
                if (applied.signum() > 0) {
                    BigDecimal newIssued = issued.add(applied);
                    BigDecimal shortage = required.subtract(newIssued).max(BigDecimal.ZERO);
                    String status = shortage.signum() == 0 ? "READY"
                            : newIssued.signum() > 0 ? "PARTIAL_READY" : "WAIT_READY";
                    updates.add(new MaterialIssueUpdate(requirement, newIssued, shortage, status));
                    remaining = remaining.subtract(applied);
                }
            }
            if (remaining.signum() > 0) {
                throw business(ResultCode.BUSINESS_ERROR,
                        "物料 " + request.getKey() + " 的本次领料数量超过工单尚未领用数量，超出 "
                                + remaining.stripTrailingZeros().toPlainString());
            }
        }
        return updates;
    }

    private void applyMaterialIssueUpdates(List<MaterialIssueUpdate> updates, Long operatorId, LocalDateTime now) {
        for (MaterialIssueUpdate issue : updates) {
            MfgWorkOrderMaterial update = new MfgWorkOrderMaterial();
            update.setRequirementId(issue.requirement().getRequirementId());
            update.setVersion(issue.requirement().getVersion());
            update.setIssuedQuantity(issue.issuedQuantity());
            update.setShortageQuantity(issue.shortageQuantity());
            update.setStatus(issue.status());
            update.setUpdatedBy(operatorId);
            update.setUpdatedTime(now);
            if (workOrderMaterialMapper.updateById(update) == 0) {
                throw business(ResultCode.DATA_CONFLICT, "工单用料齐套状态已变化，请刷新后重试");
            }
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    private record MaterialIssueUpdate(MfgWorkOrderMaterial requirement, BigDecimal issuedQuantity,
                                       BigDecimal shortageQuantity, String status) {
    }

    private record MaterialIssueSource(WmsInventory inventory, WmsMaterialBatch batch) {
    }
}
