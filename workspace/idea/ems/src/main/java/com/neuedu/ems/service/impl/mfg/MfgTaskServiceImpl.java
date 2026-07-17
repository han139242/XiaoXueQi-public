package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.eam.EamEquipment;
import com.neuedu.ems.entity.factory.FactoryStation;
import com.neuedu.ems.entity.factory.FactoryStationOperation;
import com.neuedu.ems.entity.mfg.MfgAndonAlert;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgTaskOperator;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkOrderMaterial;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.org.OrgEmployee;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.eam.EamEquipmentMapper;
import com.neuedu.ems.mapper.factory.FactoryStationMapper;
import com.neuedu.ems.mapper.factory.FactoryStationOperationMapper;
import com.neuedu.ems.mapper.mfg.MfgAndonAlertMapper;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskOperatorMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.org.OrgEmployeeMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.service.mfg.MfgTaskService;
import com.neuedu.ems.service.sys.DataScopeService;
import com.neuedu.ems.exception.DataScopeAccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

/**
 * 生产任务管理 Service 实现类。
 */
@Service
public class MfgTaskServiceImpl extends ServiceImpl<MfgTaskMapper, MfgTask> implements MfgTaskService {

    private static final String TASK_TYPE_REWORK = "REWORK";
    private static final String TASK_TYPE_SUPPLEMENT = "SUPPLEMENT";
    private static final DateTimeFormatter COMPENSATION_NUMBER_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired(required = false)
    private DataScopeService dataScopeService;

    @Autowired(required = false)
    private MfgResponsibilityNotificationService responsibilityNotificationService;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private MfgWorkReportMapper mfgWorkReportMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private OrgEmployeeMapper orgEmployeeMapper;

    @Autowired
    private MfgTaskOperatorMapper mfgTaskOperatorMapper;

    @Autowired
    private MfgWorkOrderMapper mfgWorkOrderMapper;

    @Autowired
    private MfgDispatchOrderMapper mfgDispatchOrderMapper;

    @Autowired
    private MfgWorkOrderMaterialMapper mfgWorkOrderMaterialMapper;

    @Autowired
    private ProcessRouteItemMapper processRouteItemMapper;

    @Autowired
    private FactoryStationMapper factoryStationMapper;

    @Autowired
    private FactoryStationOperationMapper factoryStationOperationMapper;

    @Autowired
    private MfgProductionPlanMapper mfgProductionPlanMapper;

    @Autowired
    private MfgProductionOrderMapper mfgProductionOrderMapper;

    @Autowired
    private EamEquipmentMapper eamEquipmentMapper;

    @Autowired
    private MfgAndonAlertMapper mfgAndonAlertMapper;

    /**
     * 分页查询生产任务列表，支持任务编号、状态、派工单、工单、工序和工位筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配任务编号）
     * @param status 任务状态
     * @param dispatchId 派工单ID
     * @param workOrderId 工单ID
     * @param operationId 工序ID
     * @param stationId 工位ID
     * @param employeeId 已分配员工ID
     * @param sort 创建时间排序
     * @return 分页结果
     */
    @Override
    public Page<MfgTask> pageTasks(Integer current, Integer size, String keyword, String status,
                                   Long dispatchId, Long workOrderId, Long operationId, Long stationId, Long employeeId, String sort) {
        Page<MfgTask> page = new Page<>(current, size);
        Page<MfgTask> result = baseMapper.selectPage(page,
                buildTaskQuery(keyword, status, dispatchId, workOrderId, operationId, stationId, employeeId, sort));
        enrichTaskView(result.getRecords());
        return result;
    }

    @Override
    public Page<MfgTask> pageTasksForView(Integer current, Integer size, String keyword, String status,
                                          Long dispatchId, Long workOrderId, Long operationId, Long stationId,
                                          Long employeeId, String sort) {
        Page<MfgTask> page = new Page<>(current, size);
        Page<MfgTask> result = baseMapper.selectPage(page,
                buildTaskQuery(keyword, status, dispatchId, workOrderId, operationId, stationId,
                        employeeId, sort, false));
        enrichTaskView(result.getRecords());
        return result;
    }

    /**
     * 查询生产任务列表，支持任务编号、状态、派工单、工单、工序和工位筛选。
     * @param keyword 关键字（匹配任务编号）
     * @param status 任务状态
     * @param dispatchId 派工单ID
     * @param workOrderId 工单ID
     * @param operationId 工序ID
     * @param stationId 工位ID
     * @param employeeId 已分配员工ID
     * @param sort 创建时间排序
     * @return 生产任务列表
     */
    @Override
    public List<MfgTask> listTasks(String keyword, String status, Long dispatchId, Long workOrderId, Long operationId, Long stationId, Long employeeId, String sort) {
        List<MfgTask> tasks = baseMapper.selectList(
                buildTaskQuery(keyword, status, dispatchId, workOrderId, operationId, stationId, employeeId, sort));
        enrichTaskView(tasks);
        return tasks;
    }

    /**
     * 根据生产任务ID查询未删除的生产任务信息。
     * @param taskId 生产任务ID
     * @return 生产任务实体
     */
    @Override
    public MfgTask getTaskById(Long taskId) {
        LambdaQueryWrapper<MfgTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgTask::getTaskId, taskId)
                .eq(MfgTask::getDeletedFlag, 0);
        applyDataScope(queryWrapper);
        MfgTask task = baseMapper.selectOne(queryWrapper);
        if (task == null && dataScopeService != null && dataScopeService.isRestricted()
                && baseMapper.selectCount(new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getTaskId, taskId).eq(MfgTask::getDeletedFlag, 0)) > 0) {
            throw new DataScopeAccessDeniedException();
        }
        enrichTaskView(task == null ? Collections.emptyList() : Collections.singletonList(task));
        return task;
    }

    @Override
    public MfgTask getTaskForView(Long taskId) {
        MfgTask task = baseMapper.selectOne(new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getTaskId, taskId)
                .eq(MfgTask::getDeletedFlag, 0));
        enrichTaskView(task == null ? Collections.emptyList() : Collections.singletonList(task));
        return task;
    }

    private void enrichTaskView(List<MfgTask> tasks) {
        enrichReportedQuantities(tasks);
        markCurrentUserAssignments(tasks);
    }

    private void markCurrentUserAssignments(List<MfgTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        if (dataScopeService == null) {
            tasks.forEach(task -> task.setAssignedToCurrentUser(true));
            return;
        }
        Set<Long> operableTaskIds = dataScopeService.findOperableTaskIds(
                tasks.stream().map(MfgTask::getTaskId).filter(java.util.Objects::nonNull).toList());
        tasks.forEach(task -> task.setAssignedToCurrentUser(
                task.getTaskId() != null && operableTaskIds.contains(task.getTaskId())));
    }

    private void enrichReportedQuantities(List<MfgTask> tasks) {
        if (tasks == null || tasks.isEmpty() || mfgWorkReportMapper == null) {
            return;
        }
        List<Long> taskIds = tasks.stream().map(MfgTask::getTaskId).filter(java.util.Objects::nonNull).toList();
        if (taskIds.isEmpty()) {
            return;
        }
        List<MfgWorkReport> reports = mfgWorkReportMapper.selectList(new LambdaQueryWrapper<MfgWorkReport>()
                .in(MfgWorkReport::getTaskId, taskIds)
                .eq(MfgWorkReport::getDeletedFlag, 0)
                .in(MfgWorkReport::getAuditStatus,
                        BusinessStatus.Document.SUBMITTED, BusinessStatus.Document.APPROVED));
        Map<Long, BigDecimal[]> totals = new HashMap<>();
        for (MfgWorkReport report : reports) {
            BigDecimal[] quantities = totals.computeIfAbsent(report.getTaskId(), ignored ->
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            quantities[0] = quantities[0].add(zeroIfNull(report.getQualifiedQuantity()));
            quantities[1] = quantities[1].add(zeroIfNull(report.getDefectQuantity()));
        }
        for (MfgTask task : tasks) {
            BigDecimal[] quantities = totals.get(task.getTaskId());
            if (quantities != null) {
                task.setReportedQualifiedQuantity(quantities[0]);
                task.setReportedDefectQuantity(quantities[1]);
            }
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 新增生产任务，自动设置删除标志为0。
     * @param task 生产任务实体
     * @return 是否成功
     */
    @Override
    public boolean saveTask(MfgTask task) {
        throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                "生产任务必须由派工单按工艺路线生成，不能脱离来源手工新增");
    }

    /**
     * 更新生产任务信息，不允许修改删除标志。
     * @param task 生产任务实体
     * @return 是否成功
     */
    @Override
    public boolean updateTask(MfgTask task) {
        if (task.getTaskId() == null) {
            return false;
        }
        MfgTask existing = getTaskById(task.getTaskId());
        if (existing == null || !BusinessStatus.Production.WAIT_START.equals(existing.getStatus())) {
            return false;
        }
        task.setDeletedFlag(null);
        task.setStatus(null);
        task.setCompletedQuantity(null);
        task.setDefectQuantity(null);
        task.setTaskNo(null);
        task.setDispatchId(null);
        task.setWorkOrderId(null);
        task.setOperationId(null);
        task.setPlannedQuantity(null);
        task.setTaskType(null);
        task.setSourceTaskId(null);
        task.setSourceNcrId(null);
        if (task.getVersion() == null) {
            task.setVersion(existing.getVersion());
        }
        return baseMapper.update(task, taskInState(task.getTaskId(), existing.getStatus())) > 0;
    }

    /**
     * 根据ID删除生产任务（软删除，将删除标志设为1）。
     * @param taskId 生产任务ID
     * @return 是否成功
     */
    @Override
    public boolean removeTaskById(Long taskId) {
        MfgTask existing = getTaskById(taskId);
        if (existing == null || !BusinessStatus.Production.WAIT_START.equals(existing.getStatus())) {
            return false;
        }
        MfgTask task = new MfgTask();
        task.setTaskId(taskId);
        task.setDeletedFlag(1);
        task.setVersion(existing.getVersion());
        return baseMapper.update(task, taskInState(taskId, existing.getStatus())) > 0;
    }

    /**
     * 开始生产任务，将任务状态改为RUNNING。
     * @param taskId 生产任务ID
     * @param stationId 工位ID，可选
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean startTask(Long taskId, Long stationId, Long operatorId) {
        if (taskId == null || operatorId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "生产任务和当前登录用户不能为空");
        }
        MfgTask existing = getTaskById(taskId);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "生产任务不存在或已删除");
        }
        if (!BusinessStatus.Production.WAIT_START.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "只有待开工任务可以开工，请刷新后确认任务状态");
        }
        Long employeeId = requireAssignedEmployee(taskId, operatorId);
        LocalDateTime now = LocalDateTime.now();
        MfgWorkOrder workOrder = requireStartableWorkOrder(existing.getWorkOrderId(), operatorId, now);
        requireExecutableDispatch(existing.getDispatchId());
        requireMaterialReady(existing.getWorkOrderId());
        requirePredecessorsFinished(existing, workOrder);
        Long actualStationId = stationId != null ? stationId : existing.getStationId();
        requireAvailableStation(actualStationId, existing.getOperationId(), workOrder.getPlanId());

        MfgTask task = new MfgTask();
        task.setTaskId(taskId);
        task.setVersion(existing.getVersion());
        if (!actualStationId.equals(existing.getStationId())) {
            task.setStationId(actualStationId);
        }
        task.setStatus(BusinessStatus.Production.RUNNING);
        boolean updated = baseMapper.update(task, taskInState(taskId, existing.getStatus())) > 0;
        if (!updated) {
            throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "生产任务已被其他人员处理，请刷新后重试");
        }
        insertStatusLog(taskId, existing.getStatus(), BusinessStatus.Production.RUNNING,
                "START_TASK", operatorId, "员工ID：" + employeeId, now);
        insertOperationLog(taskId, "START_TASK", operatorId, "员工ID：" + employeeId, now);
        if (responsibilityNotificationService != null) {
            responsibilityNotificationService.cancelOpenTaskNotifications(taskId, operatorId);
        }
        return true;
    }

    private Long requireAssignedEmployee(Long taskId, Long userId) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .eq(SysUser::getStatus, BusinessStatus.Common.ENABLE)
                .eq(SysUser::getDeletedFlag, 0));
        if (user == null || user.getEmployeeId() == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "当前登录账号未关联启用员工，不能执行车间开工");
        }
        OrgEmployee employee = orgEmployeeMapper.selectOne(new LambdaQueryWrapper<OrgEmployee>()
                .eq(OrgEmployee::getEmployeeId, user.getEmployeeId())
                .eq(OrgEmployee::getStatus, BusinessStatus.Common.ENABLE)
                .eq(OrgEmployee::getDeletedFlag, 0));
        if (employee == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "当前账号关联的员工已停用或离职，不能执行车间开工");
        }
        long assignments = mfgTaskOperatorMapper.selectCount(new LambdaQueryWrapper<MfgTaskOperator>()
                .eq(MfgTaskOperator::getTaskId, taskId)
                .eq(MfgTaskOperator::getEmployeeId, user.getEmployeeId())
                .eq(MfgTaskOperator::getDeletedFlag, 0));
        if (assignments == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "该任务未分配给当前员工，不能代替他人开工");
        }
        return user.getEmployeeId();
    }

    private MfgWorkOrder requireStartableWorkOrder(Long workOrderId, Long operatorId, LocalDateTime now) {
        MfgWorkOrder workOrder = mfgWorkOrderMapper.selectOne(new LambdaQueryWrapper<MfgWorkOrder>()
                .eq(MfgWorkOrder::getWorkOrderId, workOrderId)
                .eq(MfgWorkOrder::getDeletedFlag, 0)
                .last("FOR UPDATE"));
        if (workOrder == null || BusinessStatus.Document.CLOSED.equals(workOrder.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "父生产工单不存在或已关闭，任务不能开工");
        }
        if (BusinessStatus.Document.RELEASED.equals(workOrder.getStatus())) {
            MfgWorkOrder update = new MfgWorkOrder();
            update.setWorkOrderId(workOrder.getWorkOrderId());
            update.setVersion(workOrder.getVersion());
            update.setStatus(BusinessStatus.Production.RUNNING);
            update.setUpdatedBy(operatorId);
            update.setUpdatedTime(now);
            if (mfgWorkOrderMapper.updateById(update) == 0) {
                throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "父生产工单状态已变化，请刷新后重试");
            }
            insertStatusLog("MFG_WORK_ORDER", workOrder.getWorkOrderId(), workOrder.getStatus(),
                    BusinessStatus.Production.RUNNING, "START_BY_TASK", operatorId,
                    "员工任务开工同步父工单为生产中", now);
            workOrder.setStatus(BusinessStatus.Production.RUNNING);
            workOrder.setVersion(workOrder.getVersion() == null ? null : workOrder.getVersion() + 1);
        } else if (!BusinessStatus.Production.RUNNING.equals(workOrder.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "父生产工单状态不允许开工");
        }
        startPlanAndOrderIfNeeded(workOrder.getPlanId(), operatorId, now);
        return workOrder;
    }

    private void startPlanAndOrderIfNeeded(Long planId, Long operatorId, LocalDateTime now) {
        MfgProductionPlan plan = mfgProductionPlanMapper.selectOne(new LambdaQueryWrapper<MfgProductionPlan>()
                .eq(MfgProductionPlan::getPlanId, planId)
                .eq(MfgProductionPlan::getDeletedFlag, 0)
                .last("FOR UPDATE"));
        if (plan == null || BusinessStatus.Document.CLOSED.equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "父生产计划不存在或已关闭");
        }
        if (BusinessStatus.Document.RELEASED.equals(plan.getStatus())) {
            MfgProductionPlan update = new MfgProductionPlan();
            update.setPlanId(plan.getPlanId());
            update.setVersion(plan.getVersion());
            update.setStatus(BusinessStatus.Production.RUNNING);
            update.setUpdatedBy(operatorId);
            update.setUpdatedTime(now);
            if (mfgProductionPlanMapper.updateById(update) == 0) {
                throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "父生产计划状态已变化，请刷新后重试");
            }
            insertStatusLog("MFG_PRODUCTION_PLAN", plan.getPlanId(), plan.getStatus(),
                    BusinessStatus.Production.RUNNING, "START_BY_TASK", operatorId,
                    "员工任务开工同步父计划为生产中", now);
            plan.setStatus(BusinessStatus.Production.RUNNING);
        } else if (!BusinessStatus.Production.RUNNING.equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "父生产计划状态不允许开工");
        }

        MfgProductionOrder order = mfgProductionOrderMapper.selectOne(new LambdaQueryWrapper<MfgProductionOrder>()
                .eq(MfgProductionOrder::getProductionOrderId, plan.getProductionOrderId())
                .eq(MfgProductionOrder::getDeletedFlag, 0)
                .last("FOR UPDATE"));
        if (order == null || BusinessStatus.Document.CLOSED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "父生产订单不存在或已关闭");
        }
        if (BusinessStatus.Document.RELEASED.equals(order.getStatus())) {
            MfgProductionOrder update = new MfgProductionOrder();
            update.setProductionOrderId(order.getProductionOrderId());
            update.setVersion(order.getVersion());
            update.setStatus(BusinessStatus.Production.RUNNING);
            update.setUpdatedBy(operatorId);
            update.setUpdatedTime(now);
            if (mfgProductionOrderMapper.updateById(update) == 0) {
                throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "父生产订单状态已变化，请刷新后重试");
            }
            insertStatusLog("MFG_PRODUCTION_ORDER", order.getProductionOrderId(), order.getStatus(),
                    BusinessStatus.Production.RUNNING, "START_BY_TASK", operatorId,
                    "员工任务开工同步父订单为生产中", now);
        } else if (!BusinessStatus.Production.RUNNING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "父生产订单状态不允许开工");
        }
    }

    private void requireExecutableDispatch(Long dispatchId) {
        MfgDispatchOrder dispatch = mfgDispatchOrderMapper.selectOne(new LambdaQueryWrapper<MfgDispatchOrder>()
                .eq(MfgDispatchOrder::getDispatchId, dispatchId)
                .eq(MfgDispatchOrder::getDeletedFlag, 0));
        boolean executable = dispatch != null && (BusinessStatus.Document.RELEASED.equals(dispatch.getStatus())
                || "ACCEPTED".equals(dispatch.getStatus())
                || BusinessStatus.Production.RUNNING.equals(dispatch.getStatus()));
        if (!executable) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "派工单未派发、已驳回或已关闭，任务不能开工");
        }
    }

    private void requireMaterialReady(Long workOrderId) {
        List<MfgWorkOrderMaterial> requirements = mfgWorkOrderMaterialMapper.selectList(
                new LambdaQueryWrapper<MfgWorkOrderMaterial>()
                        .eq(MfgWorkOrderMaterial::getWorkOrderId, workOrderId)
                        .eq(MfgWorkOrderMaterial::getDeletedFlag, 0));
        if (requirements.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "生产工单没有有效用料需求，不能开工");
        }
        long shortageCount = requirements.stream()
                .filter(item -> zeroIfNull(item.getIssuedQuantity())
                        .compareTo(zeroIfNull(item.getRequiredQuantity())) < 0)
                .count();
        if (shortageCount > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "本工单物料尚未实际领料齐套，仍有 " + shortageCount + " 种物料不足");
        }
    }

    private void requirePredecessorsFinished(MfgTask task, MfgWorkOrder workOrder) {
        List<ProcessRouteItem> routeItems = processRouteItemMapper.selectList(
                new LambdaQueryWrapper<ProcessRouteItem>()
                        .eq(ProcessRouteItem::getRouteId, workOrder.getRouteId())
                        .eq(ProcessRouteItem::getDeletedFlag, 0)
                        .orderByAsc(ProcessRouteItem::getSequenceNo, ProcessRouteItem::getRouteItemId));
        List<ProcessRouteItem> currentItems = routeItems.stream()
                .filter(item -> task.getOperationId().equals(item.getOperationId()))
                .toList();
        if (currentItems.size() != 1 || currentItems.get(0).getSequenceNo() == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "工艺路线无法唯一定位当前工序，请维护路线后再开工");
        }
        int currentSequence = currentItems.get(0).getSequenceNo();
        List<ProcessRouteItem> predecessors = routeItems.stream()
                .filter(item -> item.getSequenceNo() != null && item.getSequenceNo() < currentSequence)
                .toList();
        for (ProcessRouteItem predecessor : predecessors) {
            long unfinished = baseMapper.selectCount(new LambdaQueryWrapper<MfgTask>()
                    .eq(MfgTask::getDispatchId, task.getDispatchId())
                    .eq(MfgTask::getOperationId, predecessor.getOperationId())
                    .eq(MfgTask::getDeletedFlag, 0)
                    .notIn(MfgTask::getStatus, BusinessStatus.Production.FINISHED,
                            BusinessStatus.Document.CLOSED));
            if (unfinished > 0) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                        "前置工序尚未完成，不能越序开工；前置工序ID：" + predecessor.getOperationId());
            }
            long predecessorTasks = baseMapper.selectCount(new LambdaQueryWrapper<MfgTask>()
                    .eq(MfgTask::getDispatchId, task.getDispatchId())
                    .eq(MfgTask::getOperationId, predecessor.getOperationId())
                    .eq(MfgTask::getDeletedFlag, 0));
            if (predecessorTasks == 0) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                        "前置工序任务缺失，不能开工；前置工序ID：" + predecessor.getOperationId());
            }
        }
    }

    private void requireAvailableStation(Long stationId, Long operationId, Long planId) {
        if (stationId == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "任务未分配工位，不能开工");
        }
        MfgProductionPlan plan = mfgProductionPlanMapper.selectOne(new LambdaQueryWrapper<MfgProductionPlan>()
                .eq(MfgProductionPlan::getPlanId, planId)
                .eq(MfgProductionPlan::getDeletedFlag, 0));
        FactoryStation station = factoryStationMapper.selectOne(new LambdaQueryWrapper<FactoryStation>()
                .eq(FactoryStation::getStationId, stationId)
                .eq(FactoryStation::getStatus, BusinessStatus.Factory.RUNNING)
                .eq(FactoryStation::getDeletedFlag, 0));
        if (plan == null || plan.getLineId() == null || station == null
                || !plan.getLineId().equals(station.getLineId())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "所选工位不属于生产计划产线或当前不可用");
        }
        long supported = factoryStationOperationMapper.selectCount(
                new LambdaQueryWrapper<FactoryStationOperation>()
                        .eq(FactoryStationOperation::getStationId, stationId)
                        .eq(FactoryStationOperation::getOperationId, operationId)
                        .eq(FactoryStationOperation::getStatus, BusinessStatus.Common.ENABLE)
                        .eq(FactoryStationOperation::getDeletedFlag, 0));
        if (supported == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "所选工位不支持当前工序，不能开工");
        }
        List<EamEquipment> equipment = eamEquipmentMapper.selectList(new LambdaQueryWrapper<EamEquipment>()
                .eq(EamEquipment::getStationId, stationId)
                .eq(EamEquipment::getDeletedFlag, 0));
        if (!equipment.isEmpty() && equipment.stream().noneMatch(item ->
                BusinessStatus.Equipment.IDLE.equals(item.getStatus())
                        || BusinessStatus.Equipment.RUNNING.equals(item.getStatus()))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "当前工位关联设备均不可用，请先完成维修、保养或停机处置");
        }
        long openAndon = mfgAndonAlertMapper.selectCount(new LambdaQueryWrapper<MfgAndonAlert>()
                .eq(MfgAndonAlert::getStationId, stationId)
                .eq(MfgAndonAlert::getDeletedFlag, 0)
                .in(MfgAndonAlert::getStatus, "ACTIVE", BusinessStatus.Workflow.PROCESSING));
        if (openAndon > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "当前工位存在未关闭安灯，请处置并解除后再开工");
        }
    }

    /**
     * 完成生产任务，记录完成数量和不良数量并将状态改为FINISHED。
     * @param taskId 生产任务ID
     * @param completedQuantity 完成数量
     * @param defectQuantity 不良数量
     * @param remark 备注
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean finishTask(Long taskId, BigDecimal completedQuantity, BigDecimal defectQuantity, String remark,
                              Long operatorId) {
        MfgTask existing = getTaskById(taskId);
        boolean finishable = existing != null
                && (BusinessStatus.Production.RUNNING.equals(existing.getStatus())
                || BusinessStatus.Production.PAUSED.equals(existing.getStatus()));
        if (!finishable || operatorId == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "只有生产中或已暂停的任务可以确认完工");
        }
        List<MfgWorkReport> approvedReports = mfgWorkReportMapper.selectList(new LambdaQueryWrapper<MfgWorkReport>()
                .eq(MfgWorkReport::getTaskId, taskId)
                .eq(MfgWorkReport::getDeletedFlag, 0)
                .eq(MfgWorkReport::getAuditStatus, BusinessStatus.Document.APPROVED));
        if (approvedReports.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "任务尚无已审核通过的报工，不能确认完工");
        }
        completedQuantity = approvedReports.stream().map(MfgWorkReport::getQualifiedQuantity)
                .map(this::zeroIfNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        defectQuantity = approvedReports.stream().map(MfgWorkReport::getDefectQuantity)
                .map(this::zeroIfNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (existing.getPlannedQuantity() != null) {
            BigDecimal reportedQuantity = completedQuantity.add(defectQuantity);
            int quantityComparison = reportedQuantity.compareTo(existing.getPlannedQuantity());
            if (quantityComparison > 0) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                        "任务累计审核报工数量超过任务计划数量，不能确认完工");
            }
            if (quantityComparison < 0) {
                BigDecimal remaining = existing.getPlannedQuantity().subtract(reportedQuantity);
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                        "任务尚有 " + remaining.stripTrailingZeros().toPlainString()
                                + " 未报工，不能普通完工；请继续报工，提前关闭需走受控异常流程");
            }
        }
        MfgTask task = new MfgTask();
        task.setTaskId(taskId);
        task.setVersion(existing.getVersion());
        task.setCompletedQuantity(completedQuantity);
        task.setDefectQuantity(defectQuantity);
        task.setRemark(remark);
        task.setStatus(BusinessStatus.Production.FINISHED);
        boolean updated = baseMapper.update(task, taskInState(taskId, existing.getStatus())) > 0;
        if (updated) {
            LocalDateTime now = LocalDateTime.now();
            insertStatusLog(taskId, existing.getStatus(), BusinessStatus.Production.FINISHED,
                    "FINISH_TASK", operatorId, remark, now);
            insertOperationLog(taskId, "FINISH_TASK", operatorId, remark, now);
            if (responsibilityNotificationService != null) {
                responsibilityNotificationService.cancelOpenTaskNotifications(taskId, operatorId);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgTask controlledCloseTask(Long taskId, Long operatorId, String reason) {
        if (taskId == null || operatorId == null || !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "任务、操作人和提前关闭原因不能为空");
        }
        getTaskById(taskId);
        MfgTask existing = sourceTaskForUpdate(taskId);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "生产任务不存在或已删除");
        }
        if (BusinessStatus.Document.CLOSED.equals(existing.getStatus())) {
            return existingCompensation(existing.getTaskId(), null, TASK_TYPE_SUPPLEMENT);
        }
        if (!List.of(BusinessStatus.Production.RUNNING, BusinessStatus.Production.PAUSED)
                .contains(existing.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "只有生产中或已暂停且尚未足量报工的任务可以提前关闭");
        }
        long pendingReports = mfgWorkReportMapper.selectCount(new LambdaQueryWrapper<MfgWorkReport>()
                .eq(MfgWorkReport::getTaskId, taskId)
                .eq(MfgWorkReport::getAuditStatus, BusinessStatus.Document.SUBMITTED)
                .eq(MfgWorkReport::getDeletedFlag, 0));
        if (pendingReports > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "任务仍有待审核报工，请先完成审核或撤回后再提前关闭");
        }
        List<MfgWorkReport> approvedReports = mfgWorkReportMapper.selectList(new LambdaQueryWrapper<MfgWorkReport>()
                .eq(MfgWorkReport::getTaskId, taskId)
                .eq(MfgWorkReport::getAuditStatus, BusinessStatus.Document.APPROVED)
                .eq(MfgWorkReport::getDeletedFlag, 0));
        BigDecimal completed = approvedReports.stream().map(MfgWorkReport::getQualifiedQuantity)
                .map(this::zeroIfNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal defects = approvedReports.stream().map(MfgWorkReport::getDefectQuantity)
                .map(this::zeroIfNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal planned = zeroIfNull(existing.getPlannedQuantity());
        BigDecimal reported = completed.add(defects);
        if (planned.signum() <= 0 || reported.compareTo(planned) >= 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "任务已足量报工或计划数量无效，请使用普通完工流程");
        }
        BigDecimal remaining = planned.subtract(reported);
        LocalDateTime now = LocalDateTime.now();
        MfgTask update = new MfgTask();
        update.setTaskId(taskId);
        update.setVersion(existing.getVersion());
        update.setCompletedQuantity(completed);
        update.setDefectQuantity(defects);
        update.setStatus(BusinessStatus.Document.CLOSED);
        update.setRemark(reason.trim());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (baseMapper.update(update, taskInState(taskId, existing.getStatus())) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(),
                    "生产任务已被其他用户修改，请刷新后重试");
        }
        insertStatusLog(taskId, existing.getStatus(), BusinessStatus.Document.CLOSED,
                "CONTROLLED_CLOSE_TASK", operatorId, reason.trim(), now);
        insertOperationLog(taskId, "CONTROLLED_CLOSE_TASK", operatorId,
                "提前关闭并生成剩余数量续作任务，剩余=" + remaining.toPlainString() + "；" + reason.trim(), now);
        if (responsibilityNotificationService != null) {
            responsibilityNotificationService.cancelOpenTaskNotifications(taskId, operatorId);
        }
        return createCompensationTaskInternal(existing, null, remaining, TASK_TYPE_SUPPLEMENT,
                operatorId, "来源任务 " + existing.getTaskNo() + " 提前关闭续作：" + reason.trim(), now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgTask createCompensationTask(Long sourceTaskId, Long sourceNcrId, BigDecimal plannedQuantity,
                                          String taskType, Long operatorId, String remark) {
        if (sourceTaskId == null || operatorId == null || plannedQuantity == null
                || plannedQuantity.signum() <= 0 || !StringUtils.hasText(taskType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "来源任务、补偿数量、任务类型和操作人不能为空");
        }
        String normalizedType = BusinessStatus.normalizeCode(taskType);
        if (!List.of(TASK_TYPE_REWORK, TASK_TYPE_SUPPLEMENT).contains(normalizedType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "补偿任务类型只能是 REWORK 或 SUPPLEMENT");
        }
        MfgTask source = sourceTaskForUpdate(sourceTaskId);
        if (source == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "补偿任务的来源生产任务不存在");
        }
        return createCompensationTaskInternal(source, sourceNcrId, plannedQuantity, normalizedType,
                operatorId, remark, LocalDateTime.now());
    }

    private MfgTask createCompensationTaskInternal(MfgTask source, Long sourceNcrId,
                                                    BigDecimal plannedQuantity, String taskType,
                                                    Long operatorId, String remark, LocalDateTime now) {
        LambdaQueryWrapper<MfgTask> existingQuery = new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getSourceTaskId, source.getTaskId())
                .eq(MfgTask::getTaskType, taskType)
                .eq(MfgTask::getDeletedFlag, 0);
        if (sourceNcrId == null) existingQuery.isNull(MfgTask::getSourceNcrId);
        else existingQuery.eq(MfgTask::getSourceNcrId, sourceNcrId);
        List<MfgTask> existingTasks = baseMapper.selectList(existingQuery);
        if (existingTasks.size() > 1) {
            throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(),
                    "同一来源存在多张补偿任务，请先处理重复数据");
        }
        if (existingTasks.size() == 1) return existingTasks.get(0);

        MfgWorkOrder workOrder = mfgWorkOrderMapper.selectOne(new LambdaQueryWrapper<MfgWorkOrder>()
                .eq(MfgWorkOrder::getWorkOrderId, source.getWorkOrderId())
                .eq(MfgWorkOrder::getDeletedFlag, 0)
                .last("FOR UPDATE"));
        if (workOrder == null || BusinessStatus.Document.CLOSED.equals(workOrder.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "来源生产工单不存在或已关闭，不能生成补偿任务");
        }
        if (BusinessStatus.Production.FINISHED.equals(workOrder.getStatus())) {
            MfgWorkOrder reopen = new MfgWorkOrder();
            reopen.setWorkOrderId(workOrder.getWorkOrderId());
            reopen.setVersion(workOrder.getVersion());
            reopen.setStatus(BusinessStatus.Production.RUNNING);
            reopen.setUpdatedBy(operatorId);
            reopen.setUpdatedTime(now);
            if (mfgWorkOrderMapper.updateById(reopen) == 0) {
                throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "生产工单补偿重开失败，请刷新后重试");
            }
            insertStatusLog("MFG_WORK_ORDER", workOrder.getWorkOrderId(), workOrder.getStatus(),
                    BusinessStatus.Production.RUNNING, "REOPEN_WORK_ORDER_FOR_COMPENSATION",
                    operatorId, remark, now);
        } else if (!List.of(BusinessStatus.Document.RELEASED, BusinessStatus.Production.RUNNING)
                .contains(workOrder.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "来源生产工单当前状态不允许生成补偿任务");
        }
        reopenPlanAndOrderForCompensation(workOrder.getPlanId(), operatorId, remark, now);

        MfgTask compensation = new MfgTask();
        compensation.setTaskNo("TASK-" + taskType + "-" + source.getTaskId() + "-"
                + now.format(COMPENSATION_NUMBER_TIME));
        compensation.setDispatchId(source.getDispatchId());
        compensation.setWorkOrderId(source.getWorkOrderId());
        compensation.setOperationId(source.getOperationId());
        compensation.setStationId(source.getStationId());
        compensation.setPlannedQuantity(plannedQuantity);
        compensation.setCompletedQuantity(BigDecimal.ZERO);
        compensation.setDefectQuantity(BigDecimal.ZERO);
        compensation.setTaskType(taskType);
        compensation.setSourceTaskId(source.getTaskId());
        compensation.setSourceNcrId(sourceNcrId);
        compensation.setStatus(BusinessStatus.Production.WAIT_START);
        compensation.setRemark(remark);
        compensation.setVersion(0);
        compensation.setCreatedBy(operatorId);
        compensation.setCreatedTime(now);
        compensation.setUpdatedBy(operatorId);
        compensation.setUpdatedTime(now);
        compensation.setDeletedFlag(0);
        if (baseMapper.insert(compensation) == 0 || compensation.getTaskId() == null) {
            throw new BusinessException(ResultCode.FAILED.getCode(), "补偿生产任务生成失败");
        }
        cloneTaskOperators(source.getTaskId(), compensation, operatorId, now);
        insertStatusLog(compensation.getTaskId(), null, BusinessStatus.Production.WAIT_START,
                "GENERATE_COMPENSATION_TASK", operatorId, remark, now);
        insertOperationLog(compensation.getTaskId(), "GENERATE_COMPENSATION_TASK", operatorId,
                "任务类型=" + taskType + "，来源任务ID=" + source.getTaskId()
                        + (sourceNcrId == null ? "" : "，来源不合格单ID=" + sourceNcrId), now);
        return compensation;
    }

    private void reopenPlanAndOrderForCompensation(Long planId, Long operatorId,
                                                    String remark, LocalDateTime now) {
        MfgProductionPlan plan = mfgProductionPlanMapper.selectOne(
                new LambdaQueryWrapper<MfgProductionPlan>()
                        .eq(MfgProductionPlan::getPlanId, planId)
                        .eq(MfgProductionPlan::getDeletedFlag, 0)
                        .last("FOR UPDATE"));
        if (plan == null || BusinessStatus.Document.CLOSED.equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "来源生产计划不存在或已关闭，不能生成补偿任务");
        }
        if (BusinessStatus.Production.FINISHED.equals(plan.getStatus())) {
            MfgProductionPlan reopen = new MfgProductionPlan();
            reopen.setPlanId(plan.getPlanId());
            reopen.setVersion(plan.getVersion());
            reopen.setStatus(BusinessStatus.Document.RELEASED);
            reopen.setUpdatedBy(operatorId);
            reopen.setUpdatedTime(now);
            if (mfgProductionPlanMapper.updateById(reopen) == 0) {
                throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(),
                        "生产计划补偿重开失败，请刷新后重试");
            }
            insertStatusLog("MFG_PRODUCTION_PLAN", plan.getPlanId(), plan.getStatus(),
                    BusinessStatus.Document.RELEASED, "REOPEN_PLAN_FOR_COMPENSATION",
                    operatorId, remark, now);
        } else if (!List.of(BusinessStatus.Document.RELEASED, BusinessStatus.Production.RUNNING)
                .contains(plan.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "来源生产计划当前状态不允许生成补偿任务");
        }

        MfgProductionOrder order = mfgProductionOrderMapper.selectOne(
                new LambdaQueryWrapper<MfgProductionOrder>()
                        .eq(MfgProductionOrder::getProductionOrderId, plan.getProductionOrderId())
                        .eq(MfgProductionOrder::getDeletedFlag, 0)
                        .last("FOR UPDATE"));
        if (order == null || BusinessStatus.Document.CLOSED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "来源生产订单不存在或已关闭，不能生成补偿任务");
        }
        if (BusinessStatus.Production.FINISHED.equals(order.getStatus())) {
            MfgProductionOrder reopen = new MfgProductionOrder();
            reopen.setProductionOrderId(order.getProductionOrderId());
            reopen.setVersion(order.getVersion());
            reopen.setStatus(BusinessStatus.Document.RELEASED);
            reopen.setUpdatedBy(operatorId);
            reopen.setUpdatedTime(now);
            if (mfgProductionOrderMapper.updateById(reopen) == 0) {
                throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(),
                        "生产订单补偿重开失败，请刷新后重试");
            }
            insertStatusLog("MFG_PRODUCTION_ORDER", order.getProductionOrderId(), order.getStatus(),
                    BusinessStatus.Document.RELEASED, "REOPEN_ORDER_FOR_COMPENSATION",
                    operatorId, remark, now);
        } else if (!List.of(BusinessStatus.Document.RELEASED, BusinessStatus.Production.RUNNING)
                .contains(order.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                    "来源生产订单当前状态不允许生成补偿任务");
        }
    }

    private void cloneTaskOperators(Long sourceTaskId, MfgTask compensation, Long operatorId, LocalDateTime now) {
        List<MfgTaskOperator> operators = mfgTaskOperatorMapper.selectList(new LambdaQueryWrapper<MfgTaskOperator>()
                .eq(MfgTaskOperator::getTaskId, sourceTaskId)
                .eq(MfgTaskOperator::getDeletedFlag, 0));
        for (MfgTaskOperator sourceOperator : operators) {
            MfgTaskOperator assignment = new MfgTaskOperator();
            assignment.setTaskId(compensation.getTaskId());
            assignment.setEmployeeId(sourceOperator.getEmployeeId());
            assignment.setRoleInTask(sourceOperator.getRoleInTask());
            assignment.setAssignedTime(now);
            assignment.setCreatedBy(operatorId);
            assignment.setCreatedTime(now);
            assignment.setDeletedFlag(0);
            if (mfgTaskOperatorMapper.insert(assignment) == 0) {
                throw new BusinessException(ResultCode.FAILED.getCode(), "补偿任务人员复制失败");
            }
            if (responsibilityNotificationService != null) {
                responsibilityNotificationService.publishTaskAssigned(
                        compensation.getTaskId(), assignment.getEmployeeId(), operatorId);
            }
        }
        if (operators.isEmpty() && responsibilityNotificationService != null) {
            MfgDispatchOrder dispatch = mfgDispatchOrderMapper.selectById(compensation.getDispatchId());
            if (dispatch != null) {
                responsibilityNotificationService.publishTaskUnassigned(compensation, dispatch.getTeamId(),
                        responsibilityNotificationService.resolveTeamLeader(compensation.getTaskId()));
            }
        }
    }

    private MfgTask sourceTaskForUpdate(Long taskId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getTaskId, taskId)
                .eq(MfgTask::getDeletedFlag, 0)
                .last("FOR UPDATE"));
    }

    private MfgTask existingCompensation(Long sourceTaskId, Long sourceNcrId, String taskType) {
        LambdaQueryWrapper<MfgTask> query = new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getSourceTaskId, sourceTaskId)
                .eq(MfgTask::getTaskType, taskType)
                .eq(MfgTask::getDeletedFlag, 0);
        if (sourceNcrId == null) query.isNull(MfgTask::getSourceNcrId);
        else query.eq(MfgTask::getSourceNcrId, sourceNcrId);
        List<MfgTask> matches = baseMapper.selectList(query);
        if (matches.size() != 1) {
            throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(),
                    "提前关闭任务缺少唯一补偿任务，请联系管理员处理");
        }
        return matches.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawTaskStart(Long taskId, Long operatorId, String remark) {
        if (taskId == null || operatorId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "生产任务和操作人不能为空");
        }
        remark = StringUtils.hasText(remark) ? remark.trim() : "用户主动撤回，未填写补充原因";
        MfgTask existing = getTaskById(taskId);
        boolean started = existing != null && (BusinessStatus.Production.RUNNING.equals(existing.getStatus())
                || BusinessStatus.Production.PAUSED.equals(existing.getStatus()));
        if (!started || positive(existing.getCompletedQuantity()) || positive(existing.getDefectQuantity())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "只有未产生数量的已开工任务可以撤回");
        }
        long reports = mfgWorkReportMapper.selectCount(new LambdaQueryWrapper<com.neuedu.ems.entity.mfg.MfgWorkReport>()
                .eq(com.neuedu.ems.entity.mfg.MfgWorkReport::getTaskId, taskId)
                .eq(com.neuedu.ems.entity.mfg.MfgWorkReport::getDeletedFlag, 0)
                .ne(com.neuedu.ems.entity.mfg.MfgWorkReport::getAuditStatus, BusinessStatus.Document.WITHDRAWN));
        if (reports > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "任务已有有效报工，不能撤回开工");
        }
        MfgTask update = new MfgTask();
        update.setTaskId(taskId);
        update.setVersion(existing.getVersion());
        update.setStatus(BusinessStatus.Production.WAIT_START);
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(LocalDateTime.now());
        if (baseMapper.update(update, taskInState(taskId, existing.getStatus())) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "生产任务已被其他用户修改，请刷新后重试");
        }
        LocalDateTime now = LocalDateTime.now();
        insertStatusLog(taskId, existing.getStatus(), BusinessStatus.Production.WAIT_START,
                "WITHDRAW_TASK_START", operatorId, remark, now);
        insertOperationLog(taskId, "WITHDRAW_TASK_START", operatorId, remark, now);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    /**
     * 构建带当前状态的更新条件，避免并发请求重复流转同一任务。
     */
    private LambdaUpdateWrapper<MfgTask> taskInState(Long taskId, String currentStatus) {
        LambdaUpdateWrapper<MfgTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MfgTask::getTaskId, taskId)
                .eq(MfgTask::getDeletedFlag, 0)
                .eq(MfgTask::getStatus, currentStatus);
        return updateWrapper;
    }

    /**
     * 构建生产任务查询条件，默认过滤已删除记录。
     * @param keyword 关键字（匹配任务编号）
     * @param status 任务状态
     * @param dispatchId 派工单ID
     * @param workOrderId 工单ID
     * @param operationId 工序ID
     * @param stationId 工位ID
     * @param employeeId 已分配员工ID
     * @param sort 创建时间排序
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<MfgTask> buildTaskQuery(String keyword, String status,
                                                       Long dispatchId, Long workOrderId, Long operationId, Long stationId, Long employeeId, String sort) {
        return buildTaskQuery(keyword, status, dispatchId, workOrderId, operationId, stationId, employeeId, sort, true);
    }

    private LambdaQueryWrapper<MfgTask> buildTaskQuery(String keyword, String status,
                                                       Long dispatchId, Long workOrderId, Long operationId,
                                                       Long stationId, Long employeeId, String sort,
                                                       boolean applyScope) {
        LambdaQueryWrapper<MfgTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgTask::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), MfgTask::getTaskNo, keyword)
                .eq(StringUtils.hasText(status), MfgTask::getStatus, status)
                .eq(dispatchId != null, MfgTask::getDispatchId, dispatchId)
                .eq(workOrderId != null, MfgTask::getWorkOrderId, workOrderId)
                .eq(operationId != null, MfgTask::getOperationId, operationId)
                .eq(stationId != null, MfgTask::getStationId, stationId)
                .inSql(employeeId != null, MfgTask::getTaskId,
                        "SELECT task_id FROM mfg_task_operator WHERE employee_id = " + employeeId + " AND deleted_flag = 0");
        if (workOrderId == null && dispatchId == null) {
            queryWrapper.inSql(MfgTask::getWorkOrderId,
                    "SELECT work_order_id FROM mfg_work_order WHERE deleted_flag = 0 "
                            + "AND status IN ('RUNNING', 'FINISHED', 'CLOSED')");
        }
        if (applyScope) {
            applyDataScope(queryWrapper);
        }
        if ("oldest".equalsIgnoreCase(sort)) {
            queryWrapper.orderByAsc(MfgTask::getCreatedTime);
        } else {
            queryWrapper.orderByDesc(MfgTask::getCreatedTime);
        }
        return queryWrapper;
    }

    private void applyDataScope(LambdaQueryWrapper<MfgTask> queryWrapper) {
        if (dataScopeService != null) {
            dataScopeService.applyTaskScope(queryWrapper);
        }
    }

    private void insertStatusLog(Long bizId, String oldStatus, String newStatus, String reason,
                                 Long operatorId, String remark, LocalDateTime now) {
        insertStatusLog("MFG_TASK", bizId, oldStatus, newStatus, reason, operatorId, remark, now);
    }

    private void insertStatusLog(String bizType, Long bizId, String oldStatus, String newStatus,
                                 String reason, Long operatorId, String remark, LocalDateTime now) {
        SysStatusChangeLog log = new SysStatusChangeLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setChangeReason(reason);
        log.setChangedBy(operatorId);
        log.setChangedTime(now);
        log.setRemark(remark);
        log.setCreatedBy(operatorId);
        log.setCreatedTime(now);
        log.setUpdatedBy(operatorId);
        log.setUpdatedTime(now);
        log.setDeletedFlag(0);
        if (sysStatusChangeLogMapper.insert(log) == 0) throw new BusinessException(ResultCode.FAILED);
    }

    private void insertOperationLog(Long bizId, String type, Long operatorId, String remark, LocalDateTime now) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(operatorId);
        log.setOperationType(type);
        log.setModuleCode("mfg_task");
        log.setBizId(bizId);
        log.setOperationTime(now);
        log.setRemark(remark);
        log.setCreatedBy(operatorId);
        log.setCreatedTime(now);
        log.setUpdatedBy(operatorId);
        log.setUpdatedTime(now);
        log.setDeletedFlag(0);
        if (sysOperationLogMapper.insert(log) == 0) throw new BusinessException(ResultCode.FAILED);
    }
}
