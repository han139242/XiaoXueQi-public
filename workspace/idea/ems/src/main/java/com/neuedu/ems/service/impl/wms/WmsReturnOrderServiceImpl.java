package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.entity.mfg.MfgReportMaterial;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.entity.wms.WmsOutboundDetail;
import com.neuedu.ems.entity.wms.WmsOutboundOrder;
import com.neuedu.ems.entity.wms.WmsReturnDetail;
import com.neuedu.ems.entity.wms.WmsReturnOrder;
import com.neuedu.ems.entity.wms.WmsStockMovement;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.mfg.MfgReportMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundDetailMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundOrderMapper;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 退料单管理 Service 实现类。
 */
@Service
public class WmsReturnOrderServiceImpl extends ServiceImpl<WmsReturnOrderMapper, WmsReturnOrder> implements WmsReturnOrderService {

    private static final String SUBMITTED_STATUS = BusinessStatus.Document.SUBMITTED;
    private static final String APPROVED_STATUS = BusinessStatus.Document.APPROVED;
    private static final String MATERIAL_ISSUE_TYPE = "MATERIAL_ISSUE";
    private static final Set<String> MATERIAL_RETURN_TYPES = Set.of("GOOD", "DEFECTIVE");
    private static final DateTimeFormatter RETURN_NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired
    private WmsReturnDetailMapper wmsReturnDetailMapper;

    @Autowired
    private WmsInventoryMapper wmsInventoryMapper;

    @Autowired
    private MfgReportMaterialMapper mfgReportMaterialMapper;

    @Autowired
    private MfgWorkOrderMapper mfgWorkOrderMapper;

    @Autowired
    private WmsOutboundDetailMapper wmsOutboundDetailMapper;

    @Autowired
    private WmsOutboundOrderMapper wmsOutboundOrderMapper;

    @Autowired
    private WmsStockMovementMapper wmsStockMovementMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private WmsResponsibilityNotificationService responsibilityNotificationService;

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
     * 新增退料单并保存退料明细。
     * @param returnOrder 退料单实体
     * @param details 退料明细列表
     * @param operatorId 当前登录用户ID
     * @return 退料单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveReturnOrderFull(WmsReturnOrder returnOrder, List<WmsReturnDetail> details, Long operatorId) {
        if (operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "当前登录用户不能为空");
        }
        validateReturnHeader(returnOrder);
        List<ReturnLine> returnLines = prepareReturnLines(returnOrder, details, null);
        LocalDateTime now = LocalDateTime.now();
        returnOrder.setReturnId(null);
        returnOrder.setReturnNo("RT-WO" + returnOrder.getWorkOrderId() + "-" + now.format(RETURN_NUMBER_TIME));
        returnOrder.setApplicantId(operatorId);
        returnOrder.setAuditStatus(SUBMITTED_STATUS);
        returnOrder.setReturnTime(null);
        returnOrder.setVersion(0);
        returnOrder.setCreatedBy(operatorId);
        returnOrder.setCreatedTime(now);
        returnOrder.setUpdatedBy(operatorId);
        returnOrder.setUpdatedTime(now);
        returnOrder.setDeletedFlag(0);
        if (baseMapper.insert(returnOrder) == 0 || returnOrder.getReturnId() == null) {
            throw business(ResultCode.FAILED, "退料单创建失败");
        }
        for (ReturnLine line : returnLines) {
            WmsReturnDetail detail = line.detail();
            detail.setDetailId(null);
            detail.setReturnId(returnOrder.getReturnId());
            detail.setVersion(0);
            detail.setCreatedBy(operatorId);
            detail.setCreatedTime(now);
            detail.setUpdatedBy(operatorId);
            detail.setUpdatedTime(now);
            detail.setDeletedFlag(0);
            if (wmsReturnDetailMapper.insert(detail) == 0) {
                throw business(ResultCode.FAILED, "退料明细创建失败");
            }
        }
        responsibilityNotificationService.publishReturnPending(returnOrder, operatorId);
        return returnOrder.getReturnId();
    }

    /**
     * 更新退料单，已确认退料单不允许普通修改。
     * @param returnOrder 退料单实体
     * @param operatorId 当前登录用户ID
     * @return 是否成功
     */
    @Override
    public boolean updateReturnOrder(WmsReturnOrder returnOrder, Long operatorId) {
        if (returnOrder == null || returnOrder.getReturnId() == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "退料单和当前登录用户不能为空");
        }
        WmsReturnOrder existing = getReturnOrderById(returnOrder.getReturnId());
        if (existing == null || !SUBMITTED_STATUS.equals(existing.getAuditStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有待确认退料单可以修改");
        }
        returnOrder.setReturnNo(null);
        returnOrder.setReturnType(null);
        returnOrder.setWorkOrderId(null);
        returnOrder.setApplicantId(null);
        returnOrder.setDeletedFlag(null);
        returnOrder.setAuditStatus(null);
        returnOrder.setReturnTime(null);
        returnOrder.setCreatedBy(null);
        returnOrder.setCreatedTime(null);
        returnOrder.setUpdatedBy(operatorId);
        returnOrder.setUpdatedTime(LocalDateTime.now());
        if (returnOrder.getVersion() == null) {
            returnOrder.setVersion(existing.getVersion());
        }
        if (baseMapper.updateById(returnOrder) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "退料单已被其他用户修改，请刷新后重试");
        }
        return true;
    }

    /**
     * 更新退料单并替换退料明细，已确认退料单不允许替换明细。
     * @param returnOrder 退料单实体
     * @param details 退料明细列表
     * @param operatorId 当前登录用户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateReturnOrderFull(WmsReturnOrder returnOrder, List<WmsReturnDetail> details, Long operatorId) {
        if (returnOrder == null || returnOrder.getReturnId() == null) {
            throw business(ResultCode.PARAM_ERROR, "退料单不能为空");
        }
        WmsReturnOrder existing = getReturnOrderById(returnOrder.getReturnId());
        if (existing == null || !SUBMITTED_STATUS.equals(existing.getAuditStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有待确认退料单可以修改");
        }
        List<ReturnLine> returnLines = prepareReturnLines(existing, details, existing.getReturnId());
        returnOrder.setVersion(existing.getVersion());
        updateReturnOrder(returnOrder, operatorId);
        softDeleteDetails(existing.getReturnId());
        LocalDateTime now = LocalDateTime.now();
        for (ReturnLine line : returnLines) {
            WmsReturnDetail detail = line.detail();
            detail.setDetailId(null);
            detail.setReturnId(existing.getReturnId());
            detail.setVersion(0);
            detail.setCreatedBy(operatorId);
            detail.setCreatedTime(now);
            detail.setUpdatedBy(operatorId);
            detail.setUpdatedTime(now);
            detail.setDeletedFlag(0);
            if (wmsReturnDetailMapper.insert(detail) == 0) {
                throw business(ResultCode.FAILED, "退料明细更新失败");
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
        if (existing == null || !SUBMITTED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        WmsReturnOrder returnOrder = new WmsReturnOrder();
        returnOrder.setReturnId(returnId);
        returnOrder.setDeletedFlag(1);
        returnOrder.setVersion(existing.getVersion());
        boolean orderRemoved = baseMapper.updateById(returnOrder) > 0;
        softDeleteDetails(returnId);
        if (orderRemoved) {
            responsibilityNotificationService.cancelReturnPending(returnId, existing.getUpdatedBy());
        }
        return orderRemoved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawReturnOrder(Long returnId, Long operatorId, String remark) {
        WmsReturnOrder existing = getReturnOrderById(returnId);
        if (existing == null || operatorId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "退料单和操作人不能为空");
        }
        remark = StringUtils.hasText(remark) ? remark.trim() : "用户主动撤回，未填写补充原因";
        if (!SUBMITTED_STATUS.equals(existing.getAuditStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "只有未确认退料单可以撤回");
        }
        WmsReturnOrder update = new WmsReturnOrder();
        update.setReturnId(returnId); update.setVersion(existing.getVersion());
        update.setAuditStatus(BusinessStatus.Document.CLOSED); update.setUpdatedBy(operatorId); update.setUpdatedTime(LocalDateTime.now());
        if (baseMapper.updateById(update) == 0) throw new BusinessException(ResultCode.DATA_CONFLICT);
        LocalDateTime now = LocalDateTime.now();
        insertStatusChangeLog(returnId, existing.getAuditStatus(), BusinessStatus.Document.CLOSED,
                "WMS_RETURN_ORDER", "WITHDRAW_RETURN", operatorId, remark, now);
        insertOperationLog(returnId, "WITHDRAW_RETURN", "wms_return_order", operatorId, remark, now);
        responsibilityNotificationService.cancelReturnPending(returnId, operatorId);
    }

    /**
     * 确认退料入库，回加库存并写入库存流水。
     * @param returnId 退料单ID
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReturnOrder(Long returnId, Long operatorId, String remark) {
        if (returnId == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "退料单和当前登录用户不能为空");
        }
        WmsReturnOrder returnOrder = getReturnOrderForUpdate(returnId);
        if (returnOrder == null) {
            throw business(ResultCode.NOT_FOUND, "退料单不存在或已删除");
        }
        List<WmsReturnDetail> details = listActiveDetails(returnId);
        if (APPROVED_STATUS.equals(returnOrder.getAuditStatus())) {
            long movementCount = wmsStockMovementMapper.selectCount(new LambdaQueryWrapper<WmsStockMovement>()
                    .eq(WmsStockMovement::getSourceOrderType, "RETURN_ORDER")
                    .eq(WmsStockMovement::getSourceOrderId, returnId)
                    .eq(WmsStockMovement::getDeletedFlag, 0));
            if (movementCount == details.size() && !details.isEmpty()) {
                return true;
            }
            throw business(ResultCode.DATA_CONFLICT, "退料单已确认但库存流水不完整，请联系管理员核对");
        }
        if (!SUBMITTED_STATUS.equals(returnOrder.getAuditStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有待确认退料单可以确认入库");
        }
        List<ReturnLine> returnLines = prepareReturnLines(returnOrder, details, returnId);
        Map<Long, InventoryAddition> additions = prepareInventoryAdditions(returnLines);
        LocalDateTime now = LocalDateTime.now();
        WmsReturnOrder update = new WmsReturnOrder();
        update.setReturnId(returnId);
        update.setVersion(returnOrder.getVersion());
        update.setAuditStatus(APPROVED_STATUS);
        update.setReturnTime(now);
        update.setRemark(StringUtils.hasText(remark) ? remark : returnOrder.getRemark());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "退料单已被其他用户修改，请刷新后重试");
        }
        for (InventoryAddition addition : additions.values()) {
            increaseInventory(addition, operatorId, now);
        }
        for (ReturnLine line : returnLines) {
            insertReturnMovement(returnOrder, line, operatorId, remark, now);
        }
        insertStatusChangeLog(returnOrder.getReturnId(), returnOrder.getAuditStatus(), APPROVED_STATUS,
                "WMS_RETURN_ORDER", "CONFIRM_RETURN", operatorId, remark, now);
        insertOperationLog(returnOrder.getReturnId(), "CONFIRM_RETURN", "wms_return_order", operatorId, remark, now);
        responsibilityNotificationService.cancelReturnPending(returnId, operatorId);
        responsibilityNotificationService.publishReturnCompleted(returnOrder, operatorId);
        return true;
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
            if (wmsReturnDetailMapper.updateById(update) == 0) {
                throw business(ResultCode.DATA_CONFLICT, "退料明细已被其他用户修改，请刷新后重试");
            }
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

    private void validateReturnHeader(WmsReturnOrder returnOrder) {
        if (returnOrder == null || returnOrder.getWorkOrderId() == null
                || !StringUtils.hasText(returnOrder.getReturnType())) {
            throw business(ResultCode.PARAM_ERROR, "退料类型和生产工单不能为空");
        }
        String returnType = BusinessStatus.normalizeCode(returnOrder.getReturnType());
        if (!MATERIAL_RETURN_TYPES.contains(returnType)) {
            throw business(ResultCode.PARAM_ERROR, "生产退料类型只能是 GOOD 或 DEFECTIVE；成品退库必须走独立成品退库流程");
        }
        returnOrder.setReturnType(returnType);
    }

    private List<ReturnLine> prepareReturnLines(WmsReturnOrder returnOrder, List<WmsReturnDetail> details,
                                                Long excludedReturnId) {
        validateReturnHeader(returnOrder);
        MfgWorkOrder workOrder = mfgWorkOrderMapper.selectOne(new LambdaQueryWrapper<MfgWorkOrder>()
                .eq(MfgWorkOrder::getWorkOrderId, returnOrder.getWorkOrderId())
                .eq(MfgWorkOrder::getDeletedFlag, 0)
                .last("FOR UPDATE"));
        if (workOrder == null || BusinessStatus.Document.CLOSED.equals(workOrder.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "生产工单不存在或已关闭，不能办理生产退料");
        }
        if (details == null || details.isEmpty()) {
            throw business(ResultCode.BUSINESS_ERROR, "退料单至少需要一条来源领料明细");
        }
        Set<Long> sourceDetailIds = new HashSet<>();
        List<ReturnLine> lines = new ArrayList<>();
        for (WmsReturnDetail detail : details) {
            if (detail == null || detail.getOutboundDetailId() == null || detail.getReturnQuantity() == null
                    || detail.getReturnQuantity().signum() <= 0 || !StringUtils.hasText(detail.getReturnReason())) {
                throw business(ResultCode.PARAM_ERROR, "退料明细的来源领料明细、正数数量和退料原因不能为空");
            }
            if (!sourceDetailIds.add(detail.getOutboundDetailId())) {
                throw business(ResultCode.BUSINESS_ERROR, "同一来源领料明细只能填写一次，请合并退料数量");
            }
            WmsOutboundDetail sourceDetail = wmsOutboundDetailMapper.selectOne(
                    new LambdaQueryWrapper<WmsOutboundDetail>()
                            .eq(WmsOutboundDetail::getDetailId, detail.getOutboundDetailId())
                            .eq(WmsOutboundDetail::getDeletedFlag, 0));
            if (sourceDetail == null) {
                throw business(ResultCode.NOT_FOUND, "来源领料明细不存在或已删除");
            }
            WmsOutboundOrder sourceOrder = wmsOutboundOrderMapper.selectOne(
                    new LambdaQueryWrapper<WmsOutboundOrder>()
                            .eq(WmsOutboundOrder::getOutboundId, sourceDetail.getOutboundId())
                            .eq(WmsOutboundOrder::getDeletedFlag, 0));
            if (sourceOrder == null || !MATERIAL_ISSUE_TYPE.equals(sourceOrder.getOutboundType())
                    || !APPROVED_STATUS.equals(sourceOrder.getAuditStatus())
                    || !Objects.equals(returnOrder.getWorkOrderId(), sourceOrder.getWorkOrderId())) {
                throw business(ResultCode.BUSINESS_ERROR, "来源明细必须属于当前工单已确认的生产领料单");
            }
            if (sourceOrder.getWarehouseId() == null || sourceDetail.getMaterialId() == null
                    || sourceDetail.getBatchId() == null || sourceDetail.getLocationId() == null
                    || sourceDetail.getOutboundQuantity() == null || sourceDetail.getOutboundQuantity().signum() <= 0) {
                throw business(ResultCode.BUSINESS_ERROR, "来源领料明细缺少仓库、物料、批次、库位或有效数量");
            }
            rejectSourceFieldMismatch(detail, sourceDetail);
            BigDecimal returned = wmsReturnDetailMapper.sumActiveReturnedQuantity(
                    sourceDetail.getDetailId(), excludedReturnId);
            BigDecimal cumulative = (returned == null ? BigDecimal.ZERO : returned).add(detail.getReturnQuantity());
            if (cumulative.compareTo(sourceDetail.getOutboundQuantity()) > 0) {
                throw business(ResultCode.BUSINESS_ERROR,
                        "退料数量超过来源领料明细尚未退回数量；来源明细ID=" + sourceDetail.getDetailId());
            }
            detail.setMaterialId(sourceDetail.getMaterialId());
            detail.setBatchId(sourceDetail.getBatchId());
            detail.setLocationId(sourceDetail.getLocationId());
            detail.setReturnReason(detail.getReturnReason().trim());
            lines.add(new ReturnLine(detail, sourceDetail, sourceOrder));
        }
        validateAggregateReturnAvailability(returnOrder.getWorkOrderId(), lines, excludedReturnId);
        return lines;
    }

    private void validateAggregateReturnAvailability(Long workOrderId, List<ReturnLine> lines,
                                                     Long excludedReturnId) {
        Map<ReturnMaterialKey, BigDecimal> requested = new LinkedHashMap<>();
        for (ReturnLine line : lines) {
            ReturnMaterialKey key = new ReturnMaterialKey(
                    line.detail().getMaterialId(), line.detail().getBatchId());
            requested.merge(key, line.detail().getReturnQuantity(), BigDecimal::add);
        }
        for (Map.Entry<ReturnMaterialKey, BigDecimal> entry : requested.entrySet()) {
            ReturnMaterialKey key = entry.getKey();
            BigDecimal issued = wmsOutboundDetailMapper.selectList(new LambdaQueryWrapper<WmsOutboundDetail>()
                            .eq(WmsOutboundDetail::getMaterialId, key.materialId())
                            .eq(WmsOutboundDetail::getBatchId, key.batchId())
                            .eq(WmsOutboundDetail::getDeletedFlag, 0)
                            .inSql(WmsOutboundDetail::getOutboundId,
                                    "SELECT outbound_id FROM wms_outbound_order WHERE deleted_flag = 0 "
                                            + "AND outbound_type = 'MATERIAL_ISSUE' AND audit_status = 'APPROVED' "
                                            + "AND work_order_id = " + workOrderId))
                    .stream().map(WmsOutboundDetail::getOutboundQuantity).map(this::zero)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal consumed = mfgReportMaterialMapper.selectList(new LambdaQueryWrapper<MfgReportMaterial>()
                            .eq(MfgReportMaterial::getMaterialId, key.materialId())
                            .eq(MfgReportMaterial::getBatchId, key.batchId())
                            .eq(MfgReportMaterial::getDeletedFlag, 0)
                            .inSql(MfgReportMaterial::getReportId,
                                    "SELECT report.report_id FROM mfg_work_report report "
                                            + "JOIN mfg_task task ON task.task_id = report.task_id "
                                            + "WHERE report.deleted_flag = 0 AND task.deleted_flag = 0 "
                                            + "AND report.audit_status IN ('SUBMITTED','APPROVED') "
                                            + "AND task.work_order_id = " + workOrderId))
                    .stream().map(item -> zero(item.getUsedQuantity()).add(zero(item.getScrapQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal returned = zero(wmsReturnDetailMapper.sumActiveReturnedQuantityByBatch(
                    workOrderId, key.materialId(), key.batchId(), excludedReturnId));
            BigDecimal available = issued.subtract(consumed).subtract(returned).max(BigDecimal.ZERO);
            if (entry.getValue().compareTo(available) > 0) {
                throw business(ResultCode.BUSINESS_ERROR,
                        "退料数量超过本工单该批次未消耗领料量；物料ID=" + key.materialId()
                                + "，批次ID=" + key.batchId() + "，可退=" + available
                                + "，本次=" + entry.getValue());
            }
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void rejectSourceFieldMismatch(WmsReturnDetail detail, WmsOutboundDetail sourceDetail) {
        if ((detail.getMaterialId() != null && !Objects.equals(detail.getMaterialId(), sourceDetail.getMaterialId()))
                || (detail.getBatchId() != null && !Objects.equals(detail.getBatchId(), sourceDetail.getBatchId()))
                || (detail.getLocationId() != null && !Objects.equals(detail.getLocationId(), sourceDetail.getLocationId()))) {
            throw business(ResultCode.BUSINESS_ERROR, "退料物料、批次或库位与来源领料明细不一致");
        }
    }

    private Map<Long, InventoryAddition> prepareInventoryAdditions(List<ReturnLine> lines) {
        Map<Long, InventoryAddition> additions = new LinkedHashMap<>();
        for (ReturnLine line : lines) {
            WmsInventory inventory = wmsInventoryMapper.selectOne(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseId, line.sourceOrder().getWarehouseId())
                    .eq(WmsInventory::getLocationId, line.detail().getLocationId())
                    .eq(WmsInventory::getMaterialId, line.detail().getMaterialId())
                    .eq(WmsInventory::getBatchId, line.detail().getBatchId())
                    .eq(WmsInventory::getDeletedFlag, 0)
                    .last("FOR UPDATE"));
            if (inventory == null) {
                throw business(ResultCode.BUSINESS_ERROR, "来源领料库存行不存在，不能确认退料");
            }
            InventoryAddition existing = additions.get(inventory.getInventoryId());
            BigDecimal quantity = line.detail().getReturnQuantity();
            additions.put(inventory.getInventoryId(), existing == null
                    ? new InventoryAddition(inventory, quantity)
                    : new InventoryAddition(existing.inventory(), existing.quantity().add(quantity)));
        }
        return additions;
    }

    private WmsReturnOrder getReturnOrderForUpdate(Long returnId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<WmsReturnOrder>()
                .eq(WmsReturnOrder::getReturnId, returnId)
                .eq(WmsReturnOrder::getDeletedFlag, 0)
                .last("FOR UPDATE"));
    }

    private void increaseInventory(InventoryAddition addition, Long operatorId, LocalDateTime now) {
        WmsInventory inventory = addition.inventory();
        BigDecimal availableQuantity = inventory.getAvailableQuantity() == null
                ? BigDecimal.ZERO : inventory.getAvailableQuantity();
        WmsInventory update = new WmsInventory();
        update.setInventoryId(inventory.getInventoryId());
        update.setVersion(inventory.getVersion());
        update.setAvailableQuantity(availableQuantity.add(addition.quantity()));
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (wmsInventoryMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "库存余额已被其他请求修改，请刷新后重试");
        }
    }

    /**
     * 写入退料入库库存流水。
     * @param returnOrder 退料单实体
     * @param line 退料明细及来源领料信息
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param operationTime 操作时间
     */
    private void insertReturnMovement(WmsReturnOrder returnOrder, ReturnLine line,
                                      Long operatorId, String remark, LocalDateTime operationTime) {
        WmsReturnDetail detail = line.detail();
        WmsStockMovement movement = new WmsStockMovement();
        movement.setMovementNo("WMS-RETURN-" + returnOrder.getReturnId() + "-" + detail.getDetailId());
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
        if (wmsStockMovementMapper.insert(movement) == 0) {
            throw business(ResultCode.FAILED, "退料库存流水写入失败");
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
            throw business(ResultCode.FAILED, "退料状态日志写入失败");
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
            throw business(ResultCode.FAILED, "退料操作日志写入失败");
        }
    }

    private BusinessException business(ResultCode code, String message) {
        return new BusinessException(code.getCode(), message);
    }

    private record ReturnLine(WmsReturnDetail detail, WmsOutboundDetail sourceDetail,
                              WmsOutboundOrder sourceOrder) {
    }

    private record InventoryAddition(WmsInventory inventory, BigDecimal quantity) {
    }

    private record ReturnMaterialKey(Long materialId, Long batchId) {
    }
}
