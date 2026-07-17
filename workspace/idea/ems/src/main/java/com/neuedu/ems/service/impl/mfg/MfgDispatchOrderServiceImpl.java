package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.mfg.MfgWorkOrderMaterial;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.factory.FactoryStation;
import com.neuedu.ems.entity.factory.FactoryStationOperation;
import com.neuedu.ems.entity.org.OrgTeam;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.factory.FactoryStationMapper;
import com.neuedu.ems.mapper.factory.FactoryStationOperationMapper;
import com.neuedu.ems.mapper.org.OrgTeamMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.mfg.MfgDispatchOrderService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * 派工单业务逻辑实现类。
 * 实现派工单的分页查询、条件查询、新增、更新和删除功能。
 */
@Service
public class MfgDispatchOrderServiceImpl extends ServiceImpl<MfgDispatchOrderMapper, MfgDispatchOrder> implements MfgDispatchOrderService {

    private static final String WAIT_DISPATCH = "WAIT_DISPATCH";
    private static final String MATERIAL_READY = "READY";
    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired private MfgTaskMapper taskMapper;
    @Autowired private MfgWorkReportMapper workReportMapper;
    @Autowired private MfgWorkOrderMapper workOrderMapper;
    @Autowired private MfgWorkOrderMaterialMapper workOrderMaterialMapper;
    @Autowired private MfgProductionPlanMapper productionPlanMapper;
    @Autowired private ProcessRouteItemMapper routeItemMapper;
    @Autowired private FactoryStationOperationMapper stationOperationMapper;
    @Autowired private FactoryStationMapper stationMapper;
    @Autowired private OrgTeamMapper teamMapper;
    @Autowired private SysStatusChangeLogMapper statusLogMapper;
    @Autowired private SysOperationLogMapper operationLogMapper;
    @Autowired(required = false) private MfgResponsibilityNotificationService notificationService;

    /**
     * 分页查询派工单，支持关键字、状态、工单ID和班组ID筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配派工单编号），可选
     * @param status 派工单状态（待派发、已派发、已接受、生产中、已完成、已驳回），可选
     * @param workOrderId 工单ID，可选
     * @param teamId 班组ID，可选
     * @return 分页结果
     */
    @Override
    public Page<MfgDispatchOrder> pageDispatchOrders(Integer current, Integer size, String keyword, String status, Long workOrderId, Long teamId) {
        Page<MfgDispatchOrder> page = new Page<>(current, size);
        LambdaQueryWrapper<MfgDispatchOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgDispatchOrder::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), MfgDispatchOrder::getDispatchNo, keyword)
                .eq(StringUtils.hasText(status), MfgDispatchOrder::getStatus, status)
                .eq(workOrderId != null, MfgDispatchOrder::getWorkOrderId, workOrderId)
                .eq(teamId != null, MfgDispatchOrder::getTeamId, teamId)
                .orderByDesc(MfgDispatchOrder::getCreatedTime);
        return this.page(page, queryWrapper);
    }

    /**
     * 根据条件查询派工单列表，支持关键字、状态、工单ID和班组ID筛选。
     * @param keyword 关键字（匹配派工单编号），可选
     * @param status 派工单状态，可选
     * @param workOrderId 工单ID，可选
     * @param teamId 班组ID，可选
     * @return 派工单列表
     */
    @Override
    public List<MfgDispatchOrder> listDispatchOrders(String keyword, String status, Long workOrderId, Long teamId) {
        LambdaQueryWrapper<MfgDispatchOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgDispatchOrder::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), MfgDispatchOrder::getDispatchNo, keyword)
                .eq(StringUtils.hasText(status), MfgDispatchOrder::getStatus, status)
                .eq(workOrderId != null, MfgDispatchOrder::getWorkOrderId, workOrderId)
                .eq(teamId != null, MfgDispatchOrder::getTeamId, teamId)
                .orderByDesc(MfgDispatchOrder::getCreatedTime);
        return this.list(queryWrapper);
    }

    /**
     * 根据派工单ID查询派工单详情。
     * @param dispatchId 派工单ID
     * @return 派工单实体
     */
    @Override
    public MfgDispatchOrder getDispatchOrderById(Long dispatchId) {
        LambdaQueryWrapper<MfgDispatchOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgDispatchOrder::getDispatchId, dispatchId)
                .eq(MfgDispatchOrder::getDeletedFlag, 0);
        return this.getOne(queryWrapper);
    }

    /**
     * 新增派工单。
     * @param dispatchOrder 派工单实体
     * @return 操作是否成功
     */
    @Override
    public boolean saveDispatchOrder(MfgDispatchOrder dispatchOrder) {
        throw business(ResultCode.BUSINESS_ERROR, "派工单必须从已下达生产工单生成，不能脱离来源手工新增");
    }

    /**
     * 更新派工单信息。
     * @param dispatchOrder 派工单实体（必须包含dispatchId）
     * @return 操作是否成功
     */
    @Override
    public boolean updateDispatchOrder(MfgDispatchOrder dispatchOrder) {
        if (dispatchOrder == null || dispatchOrder.getDispatchId() == null) {
            return false;
        }
        MfgDispatchOrder existing = getDispatchOrderById(dispatchOrder.getDispatchId());
        if (existing == null || !WAIT_DISPATCH.equals(existing.getStatus())) {
            return false;
        }
        dispatchOrder.setDispatchNo(null);
        dispatchOrder.setWorkOrderId(null);
        dispatchOrder.setPlannedQuantity(null);
        dispatchOrder.setMaterialReadyStatus(null);
        dispatchOrder.setStatus(null);
        dispatchOrder.setDeletedFlag(null);
        if (dispatchOrder.getVersion() == null) {
            dispatchOrder.setVersion(existing.getVersion());
        }
        return this.updateById(dispatchOrder);
    }

    /**
     * 删除派工单（软删除，将deletedFlag设为1）。
     * @param dispatchId 派工单ID
     * @return 操作是否成功
     */
    @Override
    public boolean removeDispatchOrderById(Long dispatchId) {
        MfgDispatchOrder existing = getDispatchOrderById(dispatchId);
        if (existing == null || !WAIT_DISPATCH.equals(existing.getStatus())) {
            return false;
        }
        MfgDispatchOrder dispatchOrder = new MfgDispatchOrder();
        dispatchOrder.setDispatchId(dispatchId);
        dispatchOrder.setVersion(existing.getVersion());
        dispatchOrder.setDeletedFlag(1);
        return this.updateById(dispatchOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgDispatchOrder generateDispatchOrder(Long workOrderId, MfgDispatchOrder request, Long operatorId) {
        if (workOrderId == null || request == null || request.getTeamId() == null
                || request.getDueTime() == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "生产工单、派工班组、要求完成时间和操作人不能为空");
        }
        MfgWorkOrder workOrder = workOrderMapper.selectOne(new LambdaQueryWrapper<MfgWorkOrder>()
                .eq(MfgWorkOrder::getWorkOrderId, workOrderId)
                .eq(MfgWorkOrder::getDeletedFlag, 0)
                .last("FOR UPDATE"));
        if (workOrder == null) {
            throw business(ResultCode.NOT_FOUND, "生产工单不存在或已删除");
        }
        if (!BusinessStatus.Document.RELEASED.equals(workOrder.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有已下达生产工单可以生成派工单");
        }
        MfgProductionPlan plan = activePlan(workOrder.getPlanId());
        LocalDateTime now = LocalDateTime.now();
        validateDispatchTime(request.getDueTime(), plan, now);
        OrgTeam team = activeTeam(request.getTeamId());
        if (team == null) {
            throw business(ResultCode.BUSINESS_ERROR, "派工班组不存在或已停用");
        }
        long activeDispatches = baseMapper.selectCount(new LambdaQueryWrapper<MfgDispatchOrder>()
                .eq(MfgDispatchOrder::getWorkOrderId, workOrderId)
                .eq(MfgDispatchOrder::getDeletedFlag, 0)
                .notIn(MfgDispatchOrder::getStatus, BusinessStatus.Document.REJECTED, BusinessStatus.Document.CLOSED));
        if (activeDispatches > 0) {
            throw business(ResultCode.BUSINESS_ERROR, "该生产工单已有有效派工单，不能重复生成");
        }
        MaterialReadiness readiness = calculateMaterialReadiness(workOrderId);
        MfgDispatchOrder dispatch = new MfgDispatchOrder();
        dispatch.setDispatchNo("DO-" + workOrderId + "-" + now.format(NUMBER_TIME));
        dispatch.setWorkOrderId(workOrderId);
        dispatch.setTeamId(team.getTeamId());
        dispatch.setPlannedQuantity(workOrder.getPlannedQuantity());
        dispatch.setDueTime(request.getDueTime());
        dispatch.setMaterialReadyStatus(readiness.status());
        dispatch.setStatus(WAIT_DISPATCH);
        dispatch.setRemark(request.getRemark());
        dispatch.setVersion(0);
        dispatch.setCreatedBy(operatorId);
        dispatch.setCreatedTime(now);
        dispatch.setUpdatedBy(operatorId);
        dispatch.setUpdatedTime(now);
        dispatch.setDeletedFlag(0);
        if (baseMapper.insert(dispatch) == 0 || dispatch.getDispatchId() == null) {
            throw business(ResultCode.FAILED, "派工单生成失败");
        }
        audit("MFG_DISPATCH_ORDER", "mfg_dispatch_order", dispatch.getDispatchId(), null, WAIT_DISPATCH,
                "GENERATE_DISPATCH_ORDER", operatorId, request.getRemark(), now);
        return dispatch;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MfgTask> releaseDispatchOrder(Long dispatchId, Long operatorId, String remark) {
        if (dispatchId == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "派工单和操作人不能为空");
        }
        MfgDispatchOrder dispatch = baseMapper.selectOne(new LambdaQueryWrapper<MfgDispatchOrder>()
                .eq(MfgDispatchOrder::getDispatchId, dispatchId)
                .eq(MfgDispatchOrder::getDeletedFlag, 0)
                .last("FOR UPDATE"));
        if (dispatch == null) {
            throw business(ResultCode.NOT_FOUND, "派工单不存在或已删除");
        }
        if (!WAIT_DISPATCH.equals(dispatch.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有待派发派工单可以派发并生成任务");
        }
        MfgWorkOrder workOrder = activeReleasedWorkOrder(dispatch.getWorkOrderId());
        MfgProductionPlan plan = activePlan(workOrder.getPlanId());
        LocalDateTime now = LocalDateTime.now();
        validateDispatchTime(dispatch.getDueTime(), plan, now);
        MaterialReadiness readiness = calculateMaterialReadiness(workOrder.getWorkOrderId());
        if (!MATERIAL_READY.equals(readiness.status())) {
            throw business(ResultCode.BUSINESS_ERROR,
                    "物料未齐套，不能派发任务：仍有 " + readiness.shortageMaterialCount()
                            + " 种物料未足额发到本工单");
        }
        List<ProcessRouteItem> routeItems = routeItemMapper.selectList(new LambdaQueryWrapper<ProcessRouteItem>()
                .eq(ProcessRouteItem::getRouteId, workOrder.getRouteId())
                .eq(ProcessRouteItem::getDeletedFlag, 0)
                .orderByAsc(ProcessRouteItem::getSequenceNo, ProcessRouteItem::getRouteItemId));
        if (routeItems.isEmpty()) {
            throw business(ResultCode.BUSINESS_ERROR, "生产工单的工艺路线没有有效工序，不能派发");
        }
        long activeTasks = taskMapper.selectCount(new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getDispatchId, dispatchId)
                .eq(MfgTask::getDeletedFlag, 0)
                .ne(MfgTask::getStatus, BusinessStatus.Document.CLOSED));
        if (activeTasks > 0) {
            throw business(ResultCode.BUSINESS_ERROR, "派工单已有有效生产任务，不能重复生成");
        }
        OrgTeam team = activeTeam(dispatch.getTeamId());
        if (team == null) {
            throw business(ResultCode.BUSINESS_ERROR, "派工班组不存在或已停用");
        }
        List<MfgTask> tasks = new ArrayList<>(routeItems.size());
        for (ProcessRouteItem routeItem : routeItems) {
            if (routeItem.getOperationId() == null) {
                throw business(ResultCode.BUSINESS_ERROR, "工艺路线存在未关联工序的明细，不能派发");
            }
            Long stationId = resolveDefaultStation(routeItem.getOperationId(), plan.getLineId());
            MfgTask task = new MfgTask();
            task.setTaskNo("TASK-" + dispatchId + "-" + routeItem.getRouteItemId() + "-" + now.format(NUMBER_TIME));
            task.setDispatchId(dispatchId);
            task.setWorkOrderId(workOrder.getWorkOrderId());
            task.setOperationId(routeItem.getOperationId());
            task.setStationId(stationId);
            task.setPlannedQuantity(dispatch.getPlannedQuantity());
            task.setCompletedQuantity(java.math.BigDecimal.ZERO);
            task.setDefectQuantity(java.math.BigDecimal.ZERO);
            task.setTaskType("NORMAL");
            task.setStatus(BusinessStatus.Production.WAIT_START);
            task.setRemark(routeItem.getRemark());
            task.setVersion(0);
            task.setCreatedBy(operatorId);
            task.setCreatedTime(now);
            task.setUpdatedBy(operatorId);
            task.setUpdatedTime(now);
            task.setDeletedFlag(0);
            if (taskMapper.insert(task) == 0 || task.getTaskId() == null) {
                throw business(ResultCode.FAILED, "生产任务生成失败");
            }
            audit("MFG_TASK", "mfg_task", task.getTaskId(), null, BusinessStatus.Production.WAIT_START,
                    "GENERATE_TASK_FROM_DISPATCH", operatorId, remark, now);
            if (notificationService != null) {
                notificationService.publishTaskUnassigned(task, team.getTeamId(), team.getLeaderId());
            }
            tasks.add(task);
        }
        MfgDispatchOrder update = new MfgDispatchOrder();
        update.setDispatchId(dispatchId);
        update.setVersion(dispatch.getVersion());
        update.setStatus(BusinessStatus.Document.RELEASED);
        update.setMaterialReadyStatus(readiness.status());
        update.setRemark(StringUtils.hasText(remark) ? remark : dispatch.getRemark());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "派工单已被并发修改");
        }
        audit("MFG_DISPATCH_ORDER", "mfg_dispatch_order", dispatchId, WAIT_DISPATCH,
                BusinessStatus.Document.RELEASED, "RELEASE_DISPATCH_ORDER", operatorId, remark, now);
        return tasks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawDispatchOrder(Long dispatchId, Long operatorId, String remark) {
        if (dispatchId == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "派工单和操作人不能为空");
        }
        remark = StringUtils.hasText(remark) ? remark.trim() : "用户主动撤回，未填写补充原因";
        MfgDispatchOrder dispatch = baseMapper.selectOne(new LambdaQueryWrapper<MfgDispatchOrder>()
                .eq(MfgDispatchOrder::getDispatchId, dispatchId)
                .eq(MfgDispatchOrder::getDeletedFlag, 0).last("FOR UPDATE"));
        if (dispatch == null || !(BusinessStatus.Document.RELEASED.equals(dispatch.getStatus())
                || "ACCEPTED".equals(dispatch.getStatus()))) {
            throw business(ResultCode.BUSINESS_ERROR, "只有已派发或已接受且尚未执行的派工单可以撤回");
        }
        List<MfgTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getDispatchId, dispatchId).eq(MfgTask::getDeletedFlag, 0).last("FOR UPDATE"));
        for (MfgTask task : tasks) {
            if (!(BusinessStatus.Production.WAIT_START.equals(task.getStatus())
                    || BusinessStatus.Document.CLOSED.equals(task.getStatus()))
                    || positive(task.getCompletedQuantity()) || positive(task.getDefectQuantity())) {
                throw business(ResultCode.BUSINESS_ERROR, "派工任务已经开工或产生数量，不能撤回派工");
            }
        }
        List<Long> taskIds = tasks.stream().map(MfgTask::getTaskId).toList();
        if (!taskIds.isEmpty() && workReportMapper.selectCount(new LambdaQueryWrapper<MfgWorkReport>()
                .in(MfgWorkReport::getTaskId, taskIds).eq(MfgWorkReport::getDeletedFlag, 0)
                .ne(MfgWorkReport::getAuditStatus, BusinessStatus.Document.WITHDRAWN)) > 0) {
            throw business(ResultCode.BUSINESS_ERROR, "派工任务已有有效报工，不能撤回派工");
        }
        LocalDateTime now = LocalDateTime.now();
        for (MfgTask task : tasks) {
            if (BusinessStatus.Document.CLOSED.equals(task.getStatus())) continue;
            MfgTask update = new MfgTask();
            update.setTaskId(task.getTaskId()); update.setVersion(task.getVersion());
            update.setStatus(BusinessStatus.Document.CLOSED); update.setUpdatedBy(operatorId); update.setUpdatedTime(now);
            if (taskMapper.updateById(update) == 0) throw business(ResultCode.DATA_CONFLICT, "生产任务已被并发修改");
            audit("MFG_TASK", "mfg_task", task.getTaskId(), task.getStatus(), BusinessStatus.Document.CLOSED,
                    "WITHDRAW_DISPATCH_ORDER", operatorId, remark, now);
            if (notificationService != null) notificationService.cancelOpenTaskNotifications(task.getTaskId(), operatorId);
        }
        MfgDispatchOrder update = new MfgDispatchOrder();
        update.setDispatchId(dispatchId); update.setVersion(dispatch.getVersion()); update.setStatus(WAIT_DISPATCH);
        update.setUpdatedBy(operatorId); update.setUpdatedTime(now);
        if (baseMapper.updateById(update) == 0) throw business(ResultCode.DATA_CONFLICT, "派工单已被并发修改");
        audit("MFG_DISPATCH_ORDER", "mfg_dispatch_order", dispatchId, dispatch.getStatus(), WAIT_DISPATCH,
                "WITHDRAW_DISPATCH_ORDER", operatorId, remark, now);
    }

    private MfgWorkOrder activeReleasedWorkOrder(Long workOrderId) {
        MfgWorkOrder workOrder = workOrderMapper.selectOne(new LambdaQueryWrapper<MfgWorkOrder>()
                .eq(MfgWorkOrder::getWorkOrderId, workOrderId)
                .eq(MfgWorkOrder::getDeletedFlag, 0));
        if (workOrder == null || !BusinessStatus.Document.RELEASED.equals(workOrder.getStatus())
                || workOrder.getRouteId() == null) {
            throw business(ResultCode.BUSINESS_ERROR, "派工单未关联有效的已下达生产工单和工艺路线");
        }
        return workOrder;
    }

    private MfgProductionPlan activePlan(Long planId) {
        MfgProductionPlan plan = productionPlanMapper.selectOne(new LambdaQueryWrapper<MfgProductionPlan>()
                .eq(MfgProductionPlan::getPlanId, planId)
                .eq(MfgProductionPlan::getDeletedFlag, 0));
        if (plan == null || plan.getLineId() == null || !BusinessStatus.Document.RELEASED.equals(plan.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "生产工单未关联有效产线计划");
        }
        return plan;
    }

    private void validateDispatchTime(LocalDateTime dueTime, MfgProductionPlan plan, LocalDateTime now) {
        if (dueTime == null || !dueTime.isAfter(now)) {
            throw business(ResultCode.BUSINESS_ERROR, "派工要求完成时间已过，请重新选择未来有效时间");
        }
        if (plan.getPlannedEndTime() != null && dueTime.isAfter(plan.getPlannedEndTime())) {
            throw business(ResultCode.BUSINESS_ERROR, "派工要求完成时间不能晚于生产计划结束时间");
        }
    }

    private MaterialReadiness calculateMaterialReadiness(Long workOrderId) {
        List<MfgWorkOrderMaterial> requirements = workOrderMaterialMapper.selectList(
                new LambdaQueryWrapper<MfgWorkOrderMaterial>()
                        .eq(MfgWorkOrderMaterial::getWorkOrderId, workOrderId)
                        .eq(MfgWorkOrderMaterial::getDeletedFlag, 0));
        if (requirements.isEmpty()) {
            throw business(ResultCode.BUSINESS_ERROR, "生产工单没有有效用料需求，不能派工");
        }
        int shortageCount = 0;
        boolean anyIssued = false;
        for (MfgWorkOrderMaterial requirement : requirements) {
            java.math.BigDecimal required = requirement.getRequiredQuantity() == null
                    ? java.math.BigDecimal.ZERO : requirement.getRequiredQuantity();
            java.math.BigDecimal issued = requirement.getIssuedQuantity() == null
                    ? java.math.BigDecimal.ZERO : requirement.getIssuedQuantity();
            anyIssued = anyIssued || issued.signum() > 0;
            if (issued.compareTo(required) < 0) {
                shortageCount++;
            }
        }
        String status = shortageCount == 0 ? MATERIAL_READY : anyIssued ? "PARTIAL_READY" : "WAIT_READY";
        return new MaterialReadiness(status, shortageCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshMaterialReadiness(Long workOrderId, Long operatorId) {
        if (workOrderId == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "生产工单和操作人不能为空");
        }
        MaterialReadiness readiness = calculateMaterialReadiness(workOrderId);
        List<MfgDispatchOrder> dispatchOrders = baseMapper.selectList(
                new LambdaQueryWrapper<MfgDispatchOrder>()
                        .eq(MfgDispatchOrder::getWorkOrderId, workOrderId)
                        .eq(MfgDispatchOrder::getDeletedFlag, 0)
                        .notIn(MfgDispatchOrder::getStatus,
                                BusinessStatus.Document.CLOSED,
                                BusinessStatus.Document.REJECTED,
                                BusinessStatus.Production.FINISHED));
        LocalDateTime now = LocalDateTime.now();
        for (MfgDispatchOrder dispatchOrder : dispatchOrders) {
            if (java.util.Objects.equals(dispatchOrder.getMaterialReadyStatus(), readiness.status())) {
                continue;
            }
            MfgDispatchOrder update = new MfgDispatchOrder();
            update.setDispatchId(dispatchOrder.getDispatchId());
            update.setVersion(dispatchOrder.getVersion());
            update.setMaterialReadyStatus(readiness.status());
            update.setUpdatedBy(operatorId);
            update.setUpdatedTime(now);
            if (baseMapper.updateById(update) == 0) {
                throw business(ResultCode.DATA_CONFLICT, "派工单齐套状态已变化，请刷新后重试");
            }
            audit("MFG_DISPATCH_ORDER", "mfg_dispatch_order", dispatchOrder.getDispatchId(),
                    dispatchOrder.getMaterialReadyStatus(), readiness.status(),
                    "REFRESH_MATERIAL_READINESS", operatorId,
                    "确认生产领料后同步工单齐套状态", now);
        }
    }

    private OrgTeam activeTeam(Long teamId) {
        return teamId == null ? null : teamMapper.selectOne(new LambdaQueryWrapper<OrgTeam>()
                .eq(OrgTeam::getTeamId, teamId)
                .eq(OrgTeam::getDeletedFlag, 0));
    }

    private Long resolveDefaultStation(Long operationId, Long lineId) {
        List<FactoryStationOperation> candidates = stationOperationMapper.selectList(
                new LambdaQueryWrapper<FactoryStationOperation>()
                        .eq(FactoryStationOperation::getOperationId, operationId)
                        .eq(FactoryStationOperation::getStatus, BusinessStatus.Common.ENABLE)
                        .eq(FactoryStationOperation::getDeletedFlag, 0)
                        .orderByDesc(FactoryStationOperation::getIsDefault)
                        .orderByAsc(FactoryStationOperation::getId));
        for (FactoryStationOperation candidate : candidates) {
            FactoryStation station = stationMapper.selectOne(new LambdaQueryWrapper<FactoryStation>()
                    .eq(FactoryStation::getStationId, candidate.getStationId())
                    .eq(FactoryStation::getLineId, lineId)
                    .eq(FactoryStation::getStatus, BusinessStatus.Factory.RUNNING)
                    .eq(FactoryStation::getDeletedFlag, 0));
            if (station != null) {
                return station.getStationId();
            }
        }
        throw business(ResultCode.BUSINESS_ERROR,
                "工序 " + operationId + " 未配置当前计划产线的可用工位，不能生成任务");
    }

    private boolean positive(java.math.BigDecimal value) { return value != null && value.signum() > 0; }

    private record MaterialReadiness(String status, int shortageMaterialCount) {
    }

    private void audit(String bizType, String module, Long id, String oldStatus, String newStatus, String reason,
                       Long operatorId, String remark, LocalDateTime now) {
        SysStatusChangeLog status = new SysStatusChangeLog();
        status.setBizType(bizType); status.setBizId(id); status.setOldStatus(oldStatus); status.setNewStatus(newStatus);
        status.setChangeReason(reason); status.setChangedBy(operatorId); status.setChangedTime(now);
        status.setRemark(remark); status.setCreatedBy(operatorId); status.setCreatedTime(now); status.setDeletedFlag(0);
        if (statusLogMapper.insert(status) == 0) throw new BusinessException(ResultCode.FAILED);
        SysOperationLog operation = new SysOperationLog();
        operation.setUserId(operatorId); operation.setModuleCode(module); operation.setBizId(id);
        operation.setOperationType(reason); operation.setOperationTime(now); operation.setRemark(remark);
        operation.setCreatedBy(operatorId); operation.setCreatedTime(now); operation.setDeletedFlag(0);
        if (operationLogMapper.insert(operation) == 0) throw new BusinessException(ResultCode.FAILED);
    }

    private BusinessException business(ResultCode code, String message) {
        return new BusinessException(code.getCode(), message);
    }
}
