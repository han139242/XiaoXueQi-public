package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgReportMaterial;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgTaskOperator;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.qms.QmsInspectionScheme;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.trace.TraceMaterialUsage;
import com.neuedu.ems.entity.trace.TraceProductLot;
import com.neuedu.ems.entity.wms.WmsMaterialBatch;
import com.neuedu.ems.entity.wms.WmsOutboundDetail;
import com.neuedu.ems.entity.wms.WmsReturnDetail;
import com.neuedu.ems.mapper.mfg.MfgReportMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskOperatorMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionSchemeMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.mapper.trace.TraceMaterialUsageMapper;
import com.neuedu.ems.mapper.trace.TraceProductLotMapper;
import com.neuedu.ems.mapper.wms.WmsMaterialBatchMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundDetailMapper;
import com.neuedu.ems.mapper.wms.WmsReturnDetailMapper;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.service.mfg.MfgWorkReportService;
import com.neuedu.ems.service.impl.qms.QmsResponsibilityNotificationService;
import com.neuedu.ems.service.sys.DataScopeService;
import com.neuedu.ems.exception.DataScopeAccessDeniedException;
import com.neuedu.ems.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 生产报工 Service 实现类。
 */
@Service
public class MfgWorkReportServiceImpl extends ServiceImpl<MfgWorkReportMapper, MfgWorkReport> implements MfgWorkReportService {

    private static final String SUBMITTED_STATUS = "SUBMITTED";
    private static final String APPROVED_STATUS = "APPROVED";

    @Autowired
    private MfgReportMaterialMapper mfgReportMaterialMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private MfgTaskMapper mfgTaskMapper;

    @Autowired
    private MfgTaskOperatorMapper mfgTaskOperatorMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private MfgWorkOrderMapper mfgWorkOrderMapper;

    @Autowired
    private MfgProductionPlanMapper mfgProductionPlanMapper;

    @Autowired
    private MfgProductionOrderMapper mfgProductionOrderMapper;

    @Autowired
    private ProcessRouteItemMapper processRouteItemMapper;

    @Autowired
    private WmsMaterialBatchMapper wmsMaterialBatchMapper;

    @Autowired
    private WmsOutboundDetailMapper wmsOutboundDetailMapper;

    @Autowired
    private WmsReturnDetailMapper wmsReturnDetailMapper;

    @Autowired
    private TraceProductLotMapper traceProductLotMapper;

    @Autowired
    private TraceMaterialUsageMapper traceMaterialUsageMapper;

    @Autowired
    private QmsInspectionSchemeMapper qmsInspectionSchemeMapper;

    @Autowired
    private QmsInspectionOrderMapper qmsInspectionOrderMapper;

    @Autowired(required = false)
    private QmsResponsibilityNotificationService qmsResponsibilityNotificationService;

    @Autowired(required = false)
    private DataScopeService dataScopeService;

    @Autowired(required = false)
    private MfgResponsibilityNotificationService responsibilityNotificationService;

    /**
     * 分页查询生产报工，支持编号、任务、人员、工位和审核状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 报工编号或任务编号关键字
     * @param taskId 任务ID
     * @param operatorId 操作工ID
     * @param stationId 工位ID
     * @param auditStatus 审核状态
     * @return 报工分页结果
     */
    @Override
    public Page<MfgWorkReport> pageWorkReports(Integer current, Integer size, String keyword, Long taskId,
                                               Long operatorId, Long stationId, String auditStatus) {
        Page<MfgWorkReport> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildWorkReportQuery(keyword, taskId, operatorId, stationId, auditStatus));
    }

    /**
     * 查询生产报工列表，支持编号、任务、人员、工位和审核状态筛选。
     * @param keyword 报工编号关键字
     * @param taskId 任务ID
     * @param operatorId 操作工ID
     * @param stationId 工位ID
     * @param auditStatus 审核状态
     * @return 报工列表
     */
    @Override
    public List<MfgWorkReport> listWorkReports(String keyword, Long taskId, Long operatorId, Long stationId, String auditStatus) {
        return baseMapper.selectList(buildWorkReportQuery(keyword, taskId, operatorId, stationId, auditStatus));
    }

    /**
     * 根据ID查询未删除的生产报工。
     * @param reportId 报工ID
     * @return 报工实体
     */
    @Override
    public MfgWorkReport getWorkReportById(Long reportId) {
        LambdaQueryWrapper<MfgWorkReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgWorkReport::getReportId, reportId)
                .eq(MfgWorkReport::getDeletedFlag, 0);
        applyDataScope(queryWrapper);
        MfgWorkReport report = baseMapper.selectOne(queryWrapper);
        if (report == null && dataScopeService != null && dataScopeService.isRestricted()
                && baseMapper.selectCount(new LambdaQueryWrapper<MfgWorkReport>()
                .eq(MfgWorkReport::getReportId, reportId).eq(MfgWorkReport::getDeletedFlag, 0)) > 0) {
            throw new DataScopeAccessDeniedException();
        }
        return report;
    }

    /**
     * 新增生产报工，自动补齐编号、数量、时间、状态和删除标记。
     * @param workReport 报工实体
     * @return 是否成功
     */
    @Override
    public boolean saveWorkReport(MfgWorkReport workReport) {
        if (workReport == null) {
            throw business(ResultCode.PARAM_ERROR, "报工信息不能为空");
        }
        prepareWorkReport(workReport);
        validateWorkReport(workReport);
        if (dataScopeService != null) {
            dataScopeService.assertCanWriteWorkReport(workReport);
        }
        return baseMapper.insert(workReport) > 0;
    }

    /**
     * 新增报工主表和用料明细。
     * @param workReport 报工实体
     * @param materials 报工用料明细列表
     * @return 报工ID
     */
    @Override
    @Transactional
    public Long saveWorkReportFull(MfgWorkReport workReport, List<MfgReportMaterial> materials) {
        validateReportMaterials(workReport, materials);
        saveWorkReport(workReport);
        insertMaterials(workReport.getReportId(), materials);
        return workReport.getReportId();
    }

    /**
     * 修改生产报工，已审核通过报工不允许普通修改。
     * @param workReport 报工实体
     * @return 是否成功
     */
    @Override
    public boolean updateWorkReport(MfgWorkReport workReport) {
        if (workReport.getReportId() == null) {
            return false;
        }
        MfgWorkReport existing = getWorkReportById(workReport.getReportId());
        if (existing == null || !(SUBMITTED_STATUS.equals(existing.getAuditStatus())
                || BusinessStatus.Document.REJECTED.equals(existing.getAuditStatus()))) {
            return false;
        }
        fillMissingQuantities(workReport, existing);
        workReport.setDeletedFlag(null);
        workReport.setAuditStatus(null);
        workReport.setReportNo(null);
        workReport.setTaskId(null);
        workReport.setOperatorId(null);
        workReport.setStationId(null);
        workReport.setReportTime(null);
        if (workReport.getVersion() == null) {
            workReport.setVersion(existing.getVersion());
        }
        return baseMapper.updateById(workReport) > 0;
    }

    /**
     * 修改报工主表并替换用料明细，已审核通过报工不允许替换明细。
     * @param workReport 报工实体
     * @param materials 报工用料明细列表
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateWorkReportFull(MfgWorkReport workReport, List<MfgReportMaterial> materials) {
        if (!updateWorkReport(workReport)) {
            return false;
        }
        if (materials != null) {
            softDeleteMaterials(workReport.getReportId());
            insertMaterials(workReport.getReportId(), materials);
        }
        return true;
    }

    /**
     * 根据ID软删除生产报工及用料明细，已审核通过报工不允许删除。
     * @param reportId 报工ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean removeWorkReportById(Long reportId) {
        MfgWorkReport existing = getWorkReportById(reportId);
        if (existing == null || !(SUBMITTED_STATUS.equals(existing.getAuditStatus())
                || BusinessStatus.Document.REJECTED.equals(existing.getAuditStatus()))) {
            return false;
        }
        MfgWorkReport workReport = new MfgWorkReport();
        workReport.setReportId(reportId);
        workReport.setVersion(existing.getVersion());
        workReport.setDeletedFlag(1);
        boolean removed = baseMapper.updateById(workReport) > 0;
        softDeleteMaterials(reportId);
        return removed;
    }

    /**
     * 提交生产报工并保存用料明细，状态写为SUBMITTED。
     * @param workReport 报工实体
     * @param materials 报工用料明细列表
     * @return 报工ID
     */
    @Override
    @Transactional
    public Long submitWorkReport(MfgWorkReport workReport, List<MfgReportMaterial> materials, Long userId) {
        bindCurrentOperator(workReport, userId);
        workReport.setAuditStatus(SUBMITTED_STATUS);
        Long reportId = saveWorkReportFull(workReport, materials);
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = userId;
        insertStatusChangeLog(reportId, null, SUBMITTED_STATUS, "MFG_WORK_REPORT", "SUBMIT_WORK_REPORT",
                currentUserId, workReport.getRemark(), now);
        insertOperationLog(reportId, "SUBMIT_WORK_REPORT", "mfg_work_report", currentUserId, workReport.getRemark(), now);
        if (responsibilityNotificationService != null) {
            responsibilityNotificationService.publishWorkReportSubmitted(workReport, currentUserId);
        }
        return reportId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawWorkReport(Long reportId, Long userId, String remark) {
        if (reportId == null || userId == null) {
            throw business(ResultCode.PARAM_ERROR, "报工单和操作人不能为空");
        }
        remark = StringUtils.hasText(remark) ? remark.trim() : "用户主动撤回，未填写补充原因";
        MfgWorkReport existing = getWorkReportById(reportId);
        if (existing == null) {
            throw business(ResultCode.NOT_FOUND, "生产报工不存在或已删除");
        }
        if (!SUBMITTED_STATUS.equals(existing.getAuditStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有待审核报工可以撤回修改");
        }
        SysUser user = activeUser(userId);
        if (user.getEmployeeId() == null || !user.getEmployeeId().equals(existing.getOperatorId())) {
            throw business(ResultCode.FORBIDDEN, "只能撤回本人提交的生产报工");
        }
        MfgWorkReport update = new MfgWorkReport();
        update.setReportId(reportId);
        update.setVersion(existing.getVersion());
        update.setAuditStatus(BusinessStatus.Document.WITHDRAWN);
        update.setUpdatedBy(userId);
        update.setUpdatedTime(LocalDateTime.now());
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产报工已被其他用户修改，请刷新后重试");
        }
        LocalDateTime now = LocalDateTime.now();
        insertStatusChangeLog(reportId, existing.getAuditStatus(), BusinessStatus.Document.WITHDRAWN,
                "MFG_WORK_REPORT", "WITHDRAW_WORK_REPORT", userId, remark, now);
        insertOperationLog(reportId, "WITHDRAW_WORK_REPORT", "mfg_work_report", userId, remark, now);
        if (responsibilityNotificationService != null) {
            responsibilityNotificationService.cancelWorkReportSubmitted(reportId, userId);
        }
    }

    /**
     * 审核生产报工，审核结果使用APPROVED或REJECTED。
     * @param reportId 报工ID
     * @param auditResult 审核结果
     * @param auditComment 审核备注
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean auditWorkReport(Long reportId, String auditResult, String auditComment, Long operatorId) {
        String normalizedAuditResult = BusinessStatus.normalizeCode(auditResult);
        if (!BusinessStatus.isApprovalResult(normalizedAuditResult) || operatorId == null) {
            return false;
        }
        MfgWorkReport existing = getWorkReportById(reportId);
        if (existing == null || !SUBMITTED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        MfgWorkOrder workOrder = APPROVED_STATUS.equals(normalizedAuditResult)
                ? validateApprovalQuantity(existing)
                : null;
        MfgWorkReport update = new MfgWorkReport();
        update.setReportId(reportId);
        update.setVersion(existing.getVersion());
        update.setAuditStatus(normalizedAuditResult);
        update.setRemark(StringUtils.hasText(auditComment) ? auditComment : existing.getRemark());
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产报工已被其他用户修改，请刷新后重试");
        }
        LocalDateTime now = LocalDateTime.now();
        insertStatusChangeLog(reportId, existing.getAuditStatus(), normalizedAuditResult, "MFG_WORK_REPORT", "AUDIT_WORK_REPORT",
                operatorId, auditComment, now);
        insertOperationLog(reportId, "AUDIT_WORK_REPORT", "mfg_work_report", operatorId, auditComment, now);
        if (workOrder != null) {
            aggregateWorkOrder(workOrder, operatorId, now);
            generateFinalQualityChain(existing, workOrder, operatorId, now);
        }
        if (responsibilityNotificationService != null) {
            responsibilityNotificationService.cancelWorkReportSubmitted(reportId, operatorId);
            existing.setAuditStatus(normalizedAuditResult);
            responsibilityNotificationService.publishWorkReportAudited(existing, normalizedAuditResult, operatorId);
        }
        return true;
    }

    private MfgWorkOrder validateApprovalQuantity(MfgWorkReport report) {
        MfgTask task = activeTask(report.getTaskId());
        if (task == null || task.getWorkOrderId() == null) {
            throw business(ResultCode.BUSINESS_ERROR, "报工未关联有效生产任务和工单");
        }
        MfgWorkOrder workOrder = activeWorkOrder(task.getWorkOrderId());
        if (workOrder == null || !List.of(BusinessStatus.Document.RELEASED, BusinessStatus.Production.RUNNING)
                .contains(workOrder.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "报工关联的生产工单不存在或当前状态不可审核");
        }
        BigDecimal currentApproved = approvedQuantity(workOrder);
        BigDecimal candidate = isFinalOperation(workOrder, task.getOperationId())
                ? currentApproved.add(zero(report.getQualifiedQuantity()))
                : currentApproved;
        if (candidate.compareTo(workOrder.getPlannedQuantity()) > 0) {
            throw business(ResultCode.BUSINESS_ERROR, "累计审核合格数量不能超过工单计划数量");
        }
        return workOrder;
    }

    private void aggregateWorkOrder(MfgWorkOrder workOrder, Long operatorId, LocalDateTime now) {
        BigDecimal completed = approvedQuantity(workOrder);
        if (completed.compareTo(workOrder.getPlannedQuantity()) > 0) {
            throw business(ResultCode.BUSINESS_ERROR, "累计审核合格数量不能超过工单计划数量");
        }
        String oldStatus = workOrder.getStatus();
        String newStatus = completed.compareTo(workOrder.getPlannedQuantity()) >= 0
                ? BusinessStatus.Production.FINISHED
                : completed.signum() > 0 ? BusinessStatus.Production.RUNNING : oldStatus;
        MfgWorkOrder update = new MfgWorkOrder();
        update.setWorkOrderId(workOrder.getWorkOrderId());
        update.setCompletedQuantity(completed);
        update.setStatus(newStatus);
        update.setVersion(workOrder.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (mfgWorkOrderMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "工单完成数量已被其他请求修改，请刷新后重试");
        }
        if (!java.util.Objects.equals(oldStatus, newStatus)) {
            insertStatusChangeLog(workOrder.getWorkOrderId(), oldStatus, newStatus, "MFG_WORK_ORDER",
                    "AGGREGATE_APPROVED_WORK_REPORT", operatorId, null, now);
        }
        insertOperationLog(workOrder.getWorkOrderId(), "AGGREGATE_WORK_ORDER_QUANTITY", "mfg_work_order",
                operatorId, "审核报工后重算完成数量=" + completed.toPlainString(), now);
        if (BusinessStatus.Production.FINISHED.equals(newStatus)) {
            finishPlanAndOrderIfComplete(workOrder.getPlanId(), operatorId, now);
        }
    }

    private void finishPlanAndOrderIfComplete(Long planId, Long operatorId, LocalDateTime now) {
        MfgProductionPlan plan = activePlan(planId);
        if (plan == null || BusinessStatus.Document.CLOSED.equals(plan.getStatus())) {
            return;
        }
        LambdaQueryWrapper<MfgWorkOrder> workOrderQuery = new LambdaQueryWrapper<>();
        workOrderQuery.eq(MfgWorkOrder::getPlanId, planId)
                .eq(MfgWorkOrder::getDeletedFlag, 0)
                .ne(MfgWorkOrder::getStatus, BusinessStatus.Document.CLOSED);
        List<MfgWorkOrder> workOrders = mfgWorkOrderMapper.selectList(workOrderQuery);
        if (workOrders.isEmpty() || workOrders.stream().anyMatch(item ->
                !BusinessStatus.Production.FINISHED.equals(item.getStatus()))
                || workOrders.stream().map(MfgWorkOrder::getPlannedQuantity).map(this::zero)
                .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(plan.getPlannedQuantity()) < 0) {
            return;
        }
        if (!BusinessStatus.Production.FINISHED.equals(plan.getStatus())) {
            MfgProductionPlan update = new MfgProductionPlan();
            update.setPlanId(planId);
            update.setStatus(BusinessStatus.Production.FINISHED);
            update.setVersion(plan.getVersion());
            update.setUpdatedBy(operatorId);
            update.setUpdatedTime(now);
            if (mfgProductionPlanMapper.updateById(update) == 0) {
                throw business(ResultCode.DATA_CONFLICT, "生产计划已被其他请求修改，请刷新后重试");
            }
            insertStatusChangeLog(planId, plan.getStatus(), BusinessStatus.Production.FINISHED,
                    "MFG_PRODUCTION_PLAN", "ALL_WORK_ORDERS_FINISHED", operatorId, null, now);
        }
        finishProductionOrderIfComplete(plan.getProductionOrderId(), operatorId, now);
    }

    private void finishProductionOrderIfComplete(Long productionOrderId, Long operatorId, LocalDateTime now) {
        MfgProductionOrder order = activeOrder(productionOrderId);
        if (order == null || BusinessStatus.Document.CLOSED.equals(order.getStatus())) {
            return;
        }
        LambdaQueryWrapper<MfgProductionPlan> planQuery = new LambdaQueryWrapper<>();
        planQuery.eq(MfgProductionPlan::getProductionOrderId, productionOrderId)
                .eq(MfgProductionPlan::getDeletedFlag, 0)
                .ne(MfgProductionPlan::getStatus, BusinessStatus.Document.CLOSED);
        List<MfgProductionPlan> plans = mfgProductionPlanMapper.selectList(planQuery);
        if (plans.isEmpty() || plans.stream().anyMatch(item ->
                !BusinessStatus.Production.FINISHED.equals(item.getStatus()))
                || plans.stream().map(MfgProductionPlan::getPlannedQuantity).map(this::zero)
                .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(order.getOrderQuantity()) < 0) {
            return;
        }
        if (!BusinessStatus.Production.FINISHED.equals(order.getStatus())) {
            MfgProductionOrder update = new MfgProductionOrder();
            update.setProductionOrderId(productionOrderId);
            update.setStatus(BusinessStatus.Production.FINISHED);
            update.setVersion(order.getVersion());
            update.setUpdatedBy(operatorId);
            update.setUpdatedTime(now);
            if (mfgProductionOrderMapper.updateById(update) == 0) {
                throw business(ResultCode.DATA_CONFLICT, "生产订单已被其他请求修改，请刷新后重试");
            }
            insertStatusChangeLog(productionOrderId, order.getStatus(), BusinessStatus.Production.FINISHED,
                    "MFG_PRODUCTION_ORDER", "ALL_PRODUCTION_PLANS_FINISHED", operatorId, null, now);
        }
    }

    private BigDecimal approvedQuantity(MfgWorkOrder workOrder) {
        Long finalOperationId = finalOperationId(workOrder);
        LambdaQueryWrapper<MfgWorkReport> query = new LambdaQueryWrapper<>();
        query.eq(MfgWorkReport::getDeletedFlag, 0)
                .eq(MfgWorkReport::getAuditStatus, APPROVED_STATUS)
                .inSql(MfgWorkReport::getTaskId,
                        "SELECT task_id FROM mfg_task WHERE deleted_flag = 0 AND work_order_id = "
                                + workOrder.getWorkOrderId() + " AND operation_id = " + finalOperationId);
        return baseMapper.selectList(query).stream()
                .map(MfgWorkReport::getQualifiedQuantity)
                .map(this::zero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isFinalOperation(MfgWorkOrder workOrder, Long operationId) {
        return operationId != null && operationId.equals(finalOperationId(workOrder));
    }

    private Long finalOperationId(MfgWorkOrder workOrder) {
        if (workOrder == null || workOrder.getRouteId() == null) {
            throw business(ResultCode.BUSINESS_ERROR, "生产工单未关联有效工艺路线，不能汇总完成数量");
        }
        List<ProcessRouteItem> finalItems = processRouteItemMapper.selectList(
                new LambdaQueryWrapper<ProcessRouteItem>()
                        .eq(ProcessRouteItem::getRouteId, workOrder.getRouteId())
                        .eq(ProcessRouteItem::getDeletedFlag, 0)
                        .orderByDesc(ProcessRouteItem::getSequenceNo, ProcessRouteItem::getRouteItemId));
        if (finalItems.isEmpty() || finalItems.get(0).getOperationId() == null) {
            throw business(ResultCode.BUSINESS_ERROR, "工艺路线没有可识别的末工序，不能汇总完成数量");
        }
        Integer maxSequence = finalItems.get(0).getSequenceNo();
        long finalCount = finalItems.stream()
                .filter(item -> java.util.Objects.equals(maxSequence, item.getSequenceNo()))
                .count();
        if (maxSequence == null || finalCount != 1) {
            throw business(ResultCode.BUSINESS_ERROR, "工艺路线存在多个末工序且未配置并行汇总规则");
        }
        return finalItems.get(0).getOperationId();
    }

    private void generateFinalQualityChain(MfgWorkReport report, MfgWorkOrder workOrder,
                                           Long operatorId, LocalDateTime now) {
        MfgTask task = activeTask(report.getTaskId());
        if (task == null || !isFinalOperation(workOrder, task.getOperationId())
                || zero(report.getQualifiedQuantity()).signum() <= 0) {
            return;
        }
        TraceProductLot productLot = traceProductLotMapper.selectOne(new LambdaQueryWrapper<TraceProductLot>()
                .eq(TraceProductLot::getSourceReportId, report.getReportId())
                .eq(TraceProductLot::getDeletedFlag, 0));
        if (productLot == null) {
            productLot = new TraceProductLot();
            productLot.setLotNo("PL-" + workOrder.getWorkOrderId() + "-" + report.getReportId());
            productLot.setProductId(workOrder.getProductId());
            productLot.setWorkOrderId(workOrder.getWorkOrderId());
            productLot.setSourceReportId(report.getReportId());
            productLot.setBatchQuantity(zero(report.getQualifiedQuantity()).add(zero(report.getDefectQuantity())));
            productLot.setQualifiedQuantity(zero(report.getQualifiedQuantity()));
            productLot.setDefectQuantity(zero(report.getDefectQuantity()));
            productLot.setStatus("WAIT_INSPECTION");
            productLot.setRemark("由末工序审核报工自动结批");
            productLot.setVersion(0);
            productLot.setCreatedBy(operatorId);
            productLot.setCreatedTime(now);
            productLot.setUpdatedBy(operatorId);
            productLot.setUpdatedTime(now);
            productLot.setDeletedFlag(0);
            if (traceProductLotMapper.insert(productLot) == 0 || productLot.getProductLotId() == null) {
                throw business(ResultCode.FAILED, "末工序报工生成产品批次失败");
            }
            insertStatusChangeLog(productLot.getProductLotId(), null, "WAIT_INSPECTION",
                    "TRACE_PRODUCT_LOT", "GENERATE_PRODUCT_LOT_FROM_REPORT", operatorId,
                    "来源报工ID=" + report.getReportId(), now);
        }
        generateTraceMaterialUsages(productLot, report, workOrder, operatorId, now);
        generateFinalInspection(productLot, report, task, workOrder, operatorId, now);
    }

    private void generateTraceMaterialUsages(TraceProductLot productLot, MfgWorkReport report,
                                             MfgWorkOrder workOrder, Long operatorId, LocalDateTime now) {
        List<MfgReportMaterial> materials = mfgReportMaterialMapper.selectList(
                new LambdaQueryWrapper<MfgReportMaterial>()
                        .eq(MfgReportMaterial::getReportId, report.getReportId())
                        .eq(MfgReportMaterial::getDeletedFlag, 0));
        for (MfgReportMaterial material : materials) {
            long existing = traceMaterialUsageMapper.selectCount(new LambdaQueryWrapper<TraceMaterialUsage>()
                    .eq(TraceMaterialUsage::getProductLotId, productLot.getProductLotId())
                    .eq(TraceMaterialUsage::getReportId, report.getReportId())
                    .eq(TraceMaterialUsage::getMaterialId, material.getMaterialId())
                    .eq(TraceMaterialUsage::getBatchId, material.getBatchId())
                    .eq(TraceMaterialUsage::getDeletedFlag, 0));
            if (existing > 0) {
                continue;
            }
            TraceMaterialUsage usage = new TraceMaterialUsage();
            usage.setProductLotId(productLot.getProductLotId());
            usage.setWorkOrderId(workOrder.getWorkOrderId());
            usage.setReportId(report.getReportId());
            usage.setMaterialId(material.getMaterialId());
            usage.setBatchId(material.getBatchId());
            usage.setUsedQuantity(zero(material.getUsedQuantity()).add(zero(material.getScrapQuantity())));
            usage.setRemark("由末工序审核报工自动生成");
            usage.setVersion(0);
            usage.setCreatedBy(operatorId);
            usage.setCreatedTime(now);
            usage.setUpdatedBy(operatorId);
            usage.setUpdatedTime(now);
            usage.setDeletedFlag(0);
            if (traceMaterialUsageMapper.insert(usage) == 0) {
                throw business(ResultCode.FAILED, "产品批次物料追溯生成失败");
            }
        }
    }

    private void generateFinalInspection(TraceProductLot productLot, MfgWorkReport report, MfgTask task,
                                         MfgWorkOrder workOrder, Long operatorId, LocalDateTime now) {
        long activeInspection = qmsInspectionOrderMapper.selectCount(new LambdaQueryWrapper<QmsInspectionOrder>()
                .eq(QmsInspectionOrder::getProductLotId, productLot.getProductLotId())
                .eq(QmsInspectionOrder::getInspectionType, "FINAL")
                .eq(QmsInspectionOrder::getDeletedFlag, 0)
                .ne(QmsInspectionOrder::getStatus, BusinessStatus.Document.CLOSED));
        if (activeInspection > 0) {
            return;
        }
        List<QmsInspectionScheme> schemes = qmsInspectionSchemeMapper.selectList(
                new LambdaQueryWrapper<QmsInspectionScheme>()
                        .eq(QmsInspectionScheme::getProductId, workOrder.getProductId())
                        .eq(QmsInspectionScheme::getOperationId, task.getOperationId())
                        .eq(QmsInspectionScheme::getInspectionType, "FINAL")
                        .eq(QmsInspectionScheme::getStatus, BusinessStatus.Document.RELEASED)
                        .eq(QmsInspectionScheme::getDeletedFlag, 0));
        if (schemes.size() != 1) {
            throw business(ResultCode.BUSINESS_ERROR, schemes.isEmpty()
                    ? "产品末工序缺少已发布终检方案，报工不能审核通过"
                    : "产品末工序存在多个已发布终检方案，请先消除主数据歧义");
        }
        QmsInspectionOrder inspection = new QmsInspectionOrder();
        inspection.setInspectionNo("QI-FINAL-" + report.getReportId());
        inspection.setInspectionType("FINAL");
        inspection.setSchemeId(schemes.get(0).getSchemeId());
        inspection.setWorkOrderId(workOrder.getWorkOrderId());
        inspection.setTaskId(task.getTaskId());
        inspection.setReportId(report.getReportId());
        inspection.setBatchId(null);
        inspection.setProductLotId(productLot.getProductLotId());
        inspection.setSampleQuantity(BigDecimal.ZERO);
        inspection.setQualifiedQuantity(BigDecimal.ZERO);
        inspection.setUnqualifiedQuantity(BigDecimal.ZERO);
        inspection.setStatus(BusinessStatus.Document.DRAFT);
        inspection.setRemark("由待检产品批次自动生成终检单");
        inspection.setVersion(0);
        inspection.setCreatedBy(operatorId);
        inspection.setCreatedTime(now);
        inspection.setUpdatedBy(operatorId);
        inspection.setUpdatedTime(now);
        inspection.setDeletedFlag(0);
        if (qmsInspectionOrderMapper.insert(inspection) == 0 || inspection.getInspectionId() == null) {
            throw business(ResultCode.FAILED, "产品批次终检单生成失败");
        }
        insertStatusChangeLog(inspection.getInspectionId(), null, BusinessStatus.Document.DRAFT,
                "QMS_INSPECTION_ORDER", "GENERATE_FINAL_INSPECTION_FROM_PRODUCT_LOT", operatorId,
                "产品批次ID=" + productLot.getProductLotId(), now);
        if (qmsResponsibilityNotificationService != null) {
            qmsResponsibilityNotificationService.publishOpenInspection(inspection, operatorId);
        }
    }

    private MfgTask activeTask(Long taskId) {
        return taskId == null ? null : mfgTaskMapper.selectOne(new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getTaskId, taskId).eq(MfgTask::getDeletedFlag, 0));
    }

    private MfgWorkOrder activeWorkOrder(Long workOrderId) {
        return mfgWorkOrderMapper.selectOne(new LambdaQueryWrapper<MfgWorkOrder>()
                .eq(MfgWorkOrder::getWorkOrderId, workOrderId).eq(MfgWorkOrder::getDeletedFlag, 0));
    }

    private MfgProductionPlan activePlan(Long planId) {
        return planId == null ? null : mfgProductionPlanMapper.selectOne(new LambdaQueryWrapper<MfgProductionPlan>()
                .eq(MfgProductionPlan::getPlanId, planId).eq(MfgProductionPlan::getDeletedFlag, 0));
    }

    private MfgProductionOrder activeOrder(Long productionOrderId) {
        return productionOrderId == null ? null : mfgProductionOrderMapper.selectOne(
                new LambdaQueryWrapper<MfgProductionOrder>()
                        .eq(MfgProductionOrder::getProductionOrderId, productionOrderId)
                        .eq(MfgProductionOrder::getDeletedFlag, 0));
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BusinessException business(ResultCode code, String message) {
        return new BusinessException(code.getCode(), message);
    }

    /**
     * 构建生产报工查询条件，默认过滤已删除记录。
     * @param keyword 报工编号关键字
     * @param taskId 任务ID
     * @param operatorId 操作工ID
     * @param stationId 工位ID
     * @param auditStatus 审核状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<MfgWorkReport> buildWorkReportQuery(String keyword, Long taskId, Long operatorId,
                                                                   Long stationId, String auditStatus) {
        LambdaQueryWrapper<MfgWorkReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgWorkReport::getDeletedFlag, 0)
                .eq(taskId != null, MfgWorkReport::getTaskId, taskId)
                .eq(operatorId != null, MfgWorkReport::getOperatorId, operatorId)
                .eq(stationId != null, MfgWorkReport::getStationId, stationId)
                .eq(StringUtils.hasText(auditStatus), MfgWorkReport::getAuditStatus, auditStatus)
                .orderByDesc(MfgWorkReport::getReportTime)
                .orderByDesc(MfgWorkReport::getCreatedTime);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(search -> search.like(MfgWorkReport::getReportNo, keyword)
                    .or()
                    .apply("task_id IN (SELECT task_id FROM mfg_task WHERE deleted_flag = 0 "
                            + "AND task_no LIKE CONCAT('%', {0}, '%'))", keyword));
        }
        applyDataScope(queryWrapper);
        return queryWrapper;
    }

    private void applyDataScope(LambdaQueryWrapper<MfgWorkReport> queryWrapper) {
        if (dataScopeService != null) {
            dataScopeService.applyWorkReportScope(queryWrapper);
        }
    }

    private void bindCurrentOperator(MfgWorkReport report, Long userId) {
        if (report == null || userId == null || report.getTaskId() == null) {
            throw business(ResultCode.PARAM_ERROR, "生产任务和当前用户不能为空");
        }
        SysUser user = activeUser(userId);
        if (user.getEmployeeId() == null) {
            throw business(ResultCode.BUSINESS_ERROR, "当前账号未关联员工，不能提交报工");
        }
        MfgTask task = activeTask(report.getTaskId());
        if (task == null || !(BusinessStatus.Production.RUNNING.equals(task.getStatus())
                || BusinessStatus.Production.PAUSED.equals(task.getStatus()))) {
            throw business(ResultCode.BUSINESS_ERROR, "只有已开工任务可以提交报工");
        }
        long assigned = mfgTaskOperatorMapper.selectCount(new LambdaQueryWrapper<MfgTaskOperator>()
                .eq(MfgTaskOperator::getTaskId, task.getTaskId())
                .eq(MfgTaskOperator::getEmployeeId, user.getEmployeeId())
                .eq(MfgTaskOperator::getDeletedFlag, 0));
        if (assigned == 0) {
            throw business(ResultCode.FORBIDDEN, "当前员工未分配到该生产任务");
        }
        if (task.getStationId() == null) {
            throw business(ResultCode.BUSINESS_ERROR, "生产任务尚未确定工位，不能提交报工");
        }
        report.setReportId(null);
        report.setReportNo(null);
        report.setOperatorId(user.getEmployeeId());
        report.setStationId(task.getStationId());
        report.setReportTime(null);
        report.setVersion(0);
        report.setDeletedFlag(0);
        report.setCreatedBy(userId);
        report.setUpdatedBy(userId);
    }

    private SysUser activeUser(Long userId) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .eq(SysUser::getDeletedFlag, 0)
                .eq(SysUser::getStatus, BusinessStatus.Common.ENABLE));
        if (user == null) {
            throw business(ResultCode.UNAUTHORIZED, "当前账号不存在或已停用");
        }
        return user;
    }

    /**
     * 补齐报工默认值。
     * @param workReport 报工实体
     */
    private void prepareWorkReport(MfgWorkReport workReport) {
        if (!StringUtils.hasText(workReport.getReportNo())) {
            workReport.setReportNo("WR" + System.currentTimeMillis());
        }
        if (workReport.getQualifiedQuantity() == null) {
            workReport.setQualifiedQuantity(BigDecimal.ZERO);
        }
        if (workReport.getDefectQuantity() == null) {
            workReport.setDefectQuantity(BigDecimal.ZERO);
        }
        if (workReport.getActualWorkHours() == null) {
            workReport.setActualWorkHours(BigDecimal.ZERO);
        }
        if (workReport.getReportTime() == null) {
            workReport.setReportTime(LocalDateTime.now());
        }
        workReport.setAuditStatus(SUBMITTED_STATUS);
        if (workReport.getDeletedFlag() == null) {
            workReport.setDeletedFlag(0);
        }
    }

    private void validateWorkReport(MfgWorkReport workReport) {
        if (workReport.getTaskId() == null || workReport.getOperatorId() == null || workReport.getStationId() == null) {
            throw business(ResultCode.PARAM_ERROR, "生产任务、操作工和工位不能为空");
        }
        BigDecimal qualified = zero(workReport.getQualifiedQuantity());
        BigDecimal defect = zero(workReport.getDefectQuantity());
        BigDecimal workHours = zero(workReport.getActualWorkHours());
        if (qualified.signum() < 0 || defect.signum() < 0 || workHours.signum() < 0) {
            throw business(ResultCode.PARAM_ERROR, "合格数量、不良数量和实际工时不能为负数");
        }
        if (qualified.add(defect).signum() <= 0) {
            throw business(ResultCode.PARAM_ERROR, "报工数量必须大于 0");
        }
    }

    private void validateReportMaterials(MfgWorkReport report, List<MfgReportMaterial> materials) {
        if (materials == null || materials.isEmpty()) {
            return;
        }
        MfgTask task = activeTask(report == null ? null : report.getTaskId());
        if (task == null || task.getWorkOrderId() == null) {
            throw business(ResultCode.BUSINESS_ERROR, "报工未关联有效工单，不能登记实际用料");
        }
        Map<String, BigDecimal> requested = new HashMap<>();
        for (MfgReportMaterial material : materials) {
            BigDecimal used = zero(material.getUsedQuantity());
            BigDecimal scrap = zero(material.getScrapQuantity());
            if (material.getMaterialId() == null || material.getBatchId() == null
                    || used.signum() < 0 || scrap.signum() < 0 || used.add(scrap).signum() <= 0) {
                throw business(ResultCode.PARAM_ERROR, "报工用料必须包含物料、原料批次和正数消耗数量");
            }
            WmsMaterialBatch batch = wmsMaterialBatchMapper.selectOne(new LambdaQueryWrapper<WmsMaterialBatch>()
                    .eq(WmsMaterialBatch::getBatchId, material.getBatchId())
                    .eq(WmsMaterialBatch::getDeletedFlag, 0));
            if (batch == null || !material.getMaterialId().equals(batch.getMaterialId())) {
                throw business(ResultCode.BUSINESS_ERROR, "报工用料与原料批次不匹配");
            }
            String key = material.getMaterialId() + ":" + material.getBatchId();
            requested.merge(key, used.add(scrap), BigDecimal::add);
        }
        for (Map.Entry<String, BigDecimal> entry : requested.entrySet()) {
            String[] parts = entry.getKey().split(":");
            Long materialId = Long.valueOf(parts[0]);
            Long batchId = Long.valueOf(parts[1]);
            BigDecimal issued = wmsOutboundDetailMapper.selectList(new LambdaQueryWrapper<WmsOutboundDetail>()
                            .eq(WmsOutboundDetail::getMaterialId, materialId)
                            .eq(WmsOutboundDetail::getBatchId, batchId)
                            .eq(WmsOutboundDetail::getDeletedFlag, 0)
                            .inSql(WmsOutboundDetail::getOutboundId,
                                    "SELECT outbound_id FROM wms_outbound_order WHERE deleted_flag = 0 "
                                            + "AND outbound_type = 'MATERIAL_ISSUE' AND audit_status = 'APPROVED' "
                                            + "AND work_order_id = " + task.getWorkOrderId()))
                    .stream().map(WmsOutboundDetail::getOutboundQuantity).map(this::zero)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal returned = wmsReturnDetailMapper.selectList(new LambdaQueryWrapper<WmsReturnDetail>()
                            .eq(WmsReturnDetail::getMaterialId, materialId)
                            .eq(WmsReturnDetail::getBatchId, batchId)
                            .eq(WmsReturnDetail::getDeletedFlag, 0)
                            .inSql(WmsReturnDetail::getReturnId,
                                    "SELECT return_id FROM wms_return_order WHERE deleted_flag = 0 "
                                            + "AND audit_status = 'APPROVED' AND work_order_id = "
                                            + task.getWorkOrderId()))
                    .stream().map(WmsReturnDetail::getReturnQuantity).map(this::zero)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal consumed = mfgReportMaterialMapper.selectList(new LambdaQueryWrapper<MfgReportMaterial>()
                            .eq(MfgReportMaterial::getMaterialId, materialId)
                            .eq(MfgReportMaterial::getBatchId, batchId)
                            .eq(MfgReportMaterial::getDeletedFlag, 0)
                            .inSql(MfgReportMaterial::getReportId,
                                    "SELECT wr.report_id FROM mfg_work_report wr JOIN mfg_task t ON t.task_id = wr.task_id "
                                            + "WHERE wr.deleted_flag = 0 AND t.deleted_flag = 0 "
                                            + "AND wr.audit_status IN ('SUBMITTED','APPROVED') AND t.work_order_id = "
                                            + task.getWorkOrderId()))
                    .stream().map(item -> zero(item.getUsedQuantity()).add(zero(item.getScrapQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal available = issued.subtract(returned).subtract(consumed);
            if (entry.getValue().compareTo(available) > 0) {
                throw business(ResultCode.BUSINESS_ERROR,
                        "报工用料超过本工单该批次可用领料量；物料ID=" + materialId
                                + "，批次ID=" + batchId + "，可用=" + available.max(BigDecimal.ZERO)
                                + "，本次=" + entry.getValue());
            }
        }
    }

    /**
     * 部分更新报工时，用已有数量回填未传字段。
     * @param workReport 请求中的报工实体
     * @param existing 数据库中已有的报工实体
     */
    private void fillMissingQuantities(MfgWorkReport workReport, MfgWorkReport existing) {
        if (workReport.getQualifiedQuantity() == null) {
            workReport.setQualifiedQuantity(existing.getQualifiedQuantity());
        }
        if (workReport.getDefectQuantity() == null) {
            workReport.setDefectQuantity(existing.getDefectQuantity());
        }
        if (workReport.getActualWorkHours() == null) {
            workReport.setActualWorkHours(existing.getActualWorkHours());
        }
    }

    /**
     * 批量插入报工用料明细。
     * @param reportId 报工ID
     * @param materials 报工用料明细列表
     */
    private void insertMaterials(Long reportId, List<MfgReportMaterial> materials) {
        if (materials == null) {
            return;
        }
        for (MfgReportMaterial material : materials) {
            material.setId(null);
            material.setReportId(reportId);
            if (material.getUsedQuantity() == null) {
                material.setUsedQuantity(BigDecimal.ZERO);
            }
            if (material.getScrapQuantity() == null) {
                material.setScrapQuantity(BigDecimal.ZERO);
            }
            if (material.getDeletedFlag() == null) {
                material.setDeletedFlag(0);
            }
            mfgReportMaterialMapper.insert(material);
        }
    }

    /**
     * 软删除指定报工的全部用料明细。
     * @param reportId 报工ID
     */
    private void softDeleteMaterials(Long reportId) {
        LambdaQueryWrapper<MfgReportMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgReportMaterial::getReportId, reportId)
                .eq(MfgReportMaterial::getDeletedFlag, 0);
        List<MfgReportMaterial> materials = mfgReportMaterialMapper.selectList(queryWrapper);
        for (MfgReportMaterial material : materials) {
            MfgReportMaterial update = new MfgReportMaterial();
            update.setId(material.getId());
            update.setDeletedFlag(1);
            mfgReportMaterialMapper.updateById(update);
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
            throw new BusinessException(ResultCode.FAILED);
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
            throw new BusinessException(ResultCode.FAILED);
        }
    }
}
