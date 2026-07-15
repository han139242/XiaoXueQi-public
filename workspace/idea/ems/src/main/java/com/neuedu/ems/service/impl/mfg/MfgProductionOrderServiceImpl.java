package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkOrderMaterial;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMaterialMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.trace.TraceMaterialUsageMapper;
import com.neuedu.ems.mapper.trace.TraceProductLotMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundOrderMapper;
import com.neuedu.ems.mapper.wms.WmsReturnOrderMapper;
import com.neuedu.ems.service.mfg.MfgProductionOrderService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 生产订单管理 Service 实现类。
 */
@Service
public class MfgProductionOrderServiceImpl extends ServiceImpl<MfgProductionOrderMapper, MfgProductionOrder> implements MfgProductionOrderService {

    private static final DateTimeFormatter ORDER_NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private MfgProductionPlanMapper productionPlanMapper;

    @Autowired
    private MfgWorkOrderMapper workOrderMapper;

    @Autowired
    private MfgWorkOrderMaterialMapper workOrderMaterialMapper;

    @Autowired
    private MfgDispatchOrderMapper dispatchOrderMapper;

    @Autowired
    private MfgTaskMapper taskMapper;

    @Autowired
    private MfgWorkReportMapper workReportMapper;

    @Autowired
    private WmsOutboundOrderMapper outboundOrderMapper;

    @Autowired
    private WmsReturnOrderMapper returnOrderMapper;

    @Autowired
    private QmsInspectionOrderMapper inspectionOrderMapper;

    @Autowired
    private TraceProductLotMapper productLotMapper;

    @Autowired
    private TraceMaterialUsageMapper materialUsageMapper;

    @Autowired
    private MfgResponsibilityNotificationService responsibilityNotificationService;

    /**
     * 分页查询生产订单列表，支持关键字搜索、状态筛选、客户筛选和产品筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配订单编号）
     * @param status 订单状态
     * @param customerId 客户ID
     * @param productId 产品ID
     * @return 分页结果
     */
    @Override
    public Page<MfgProductionOrder> pageOrders(Integer current, Integer size, String keyword, String status, Long customerId, Long productId) {
        Page<MfgProductionOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildOrderQuery(keyword, status, customerId, productId));
    }

    /**
     * 根据生产订单ID查询生产订单信息。
     * @param productionOrderId 生产订单ID
     * @return 生产订单实体
     */
    @Override
    public MfgProductionOrder getOrderById(Long productionOrderId) {
        QueryWrapper<MfgProductionOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("production_order_id", productionOrderId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增生产订单，自动设置删除标志为0。
     * @param order 生产订单实体
     * @return 是否成功
     */
    @Override
    public boolean saveOrder(MfgProductionOrder order) {
        if (order == null || order.getProductId() == null
                || order.getOrderQuantity() == null || order.getOrderQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        order.setProductionOrderId(null);
        order.setProductionOrderNo("PO-" + LocalDateTime.now().format(ORDER_NUMBER_TIME));
        order.setStatus(BusinessStatus.Document.DRAFT);
        if (order.getDeletedFlag() == null) {
            order.setDeletedFlag(0);
        }
        return baseMapper.insert(order) > 0;
    }

    /**
     * 更新生产订单信息，不允许修改删除标志。
     * @param order 生产订单实体
     * @return 是否成功
     */
    @Override
    public boolean updateOrder(MfgProductionOrder order) {
        if (order.getProductionOrderId() == null) {
            return false;
        }
        MfgProductionOrder existing = getOrderById(order.getProductionOrderId());
        if (existing == null || !BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            return false;
        }
        order.setDeletedFlag(null);
        order.setStatus(null);
        order.setProductionOrderNo(null);
        if (order.getVersion() == null) {
            order.setVersion(existing.getVersion());
        }
        return baseMapper.updateById(order) > 0;
    }

    /**
     * 根据ID删除生产订单（软删除，将删除标志设为1）。
     * @param productionOrderId 生产订单ID
     * @return 是否成功
     */
    @Override
    public boolean removeOrderById(Long productionOrderId) {
        MfgProductionOrder existing = getOrderById(productionOrderId);
        if (existing == null || !BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            return false;
        }
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(productionOrderId);
        order.setVersion(existing.getVersion());
        order.setDeletedFlag(1);
        return baseMapper.updateById(order) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawUnexecutedOrderChain(Long productionOrderId, Long operatorId, String remark) {
        if (productionOrderId == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "生产订单和操作人不能为空");
        }
        if (!StringUtils.hasText(remark)) {
            throw business(ResultCode.PARAM_ERROR, "请填写撤回原因");
        }

        MfgProductionOrder order = activeOrderForUpdate(productionOrderId);
        if (order == null) {
            throw business(ResultCode.NOT_FOUND, "生产订单不存在或已删除");
        }
        if (!BusinessStatus.Document.RELEASED.equals(order.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有已下达且尚未执行的生产订单可以撤回修改");
        }

        List<MfgProductionPlan> plans = activePlansForUpdate(productionOrderId);
        validateRemovablePlans(plans);
        List<Long> planIds = plans.stream().map(MfgProductionPlan::getPlanId).toList();
        List<MfgWorkOrder> workOrders = activeWorkOrdersForUpdate(planIds);
        validateRemovableWorkOrders(workOrders);
        List<Long> workOrderIds = workOrders.stream().map(MfgWorkOrder::getWorkOrderId).toList();
        List<MfgDispatchOrder> dispatchOrders = activeDispatchOrdersForUpdate(workOrderIds);
        List<MfgTask> tasks = activeTasksForUpdate(workOrderIds);
        validateUnexecutedTasks(tasks);
        validateUnexecutedDispatchOrders(dispatchOrders);
        rejectExistingExecutionData(workOrderIds, tasks);

        LocalDateTime now = LocalDateTime.now();
        validateUnusedMaterialRequirements(workOrderIds);
        for (MfgTask task : tasks) {
            closeTask(task, operatorId, remark, now);
        }
        for (MfgDispatchOrder dispatchOrder : dispatchOrders) {
            rejectDispatchOrder(dispatchOrder, operatorId, remark, now);
        }
        for (MfgWorkOrder workOrder : workOrders) {
            closeWorkOrder(workOrder, operatorId, remark, now);
        }
        for (MfgProductionPlan plan : plans) {
            closePlan(plan, operatorId, remark, now);
        }
        restoreOrderToDraft(order, operatorId, remark, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawUnexecutedPlanChain(Long planId, Long operatorId, String remark) {
        requireWithdrawArguments(planId, operatorId, remark, "生产计划");
        MfgProductionPlan plan = planForUpdate(planId);
        if (plan == null || !BusinessStatus.Document.RELEASED.equals(plan.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有已生效且尚未执行的生产计划可以撤回修改");
        }
        List<MfgWorkOrder> workOrders = activeWorkOrdersForUpdate(List.of(planId));
        validateRemovableWorkOrders(workOrders);
        withdrawWorkOrdersAndExecution(workOrders, operatorId, remark, LocalDateTime.now());
        restorePlanToDraft(plan, operatorId, remark, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawUnexecutedWorkOrderChain(Long workOrderId, Long operatorId, String remark) {
        requireWithdrawArguments(workOrderId, operatorId, remark, "生产工单");
        MfgWorkOrder workOrder = workOrderForUpdate(workOrderId);
        if (workOrder == null || !BusinessStatus.Document.RELEASED.equals(workOrder.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有已下达且尚未执行的生产工单可以撤回修改");
        }
        validateRemovableWorkOrders(List.of(workOrder));
        List<Long> workOrderIds = List.of(workOrderId);
        List<MfgDispatchOrder> dispatchOrders = activeDispatchOrdersForUpdate(workOrderIds);
        List<MfgTask> tasks = activeTasksForUpdate(workOrderIds);
        validateUnexecutedTasks(tasks);
        validateUnexecutedDispatchOrders(dispatchOrders);
        rejectExistingExecutionData(workOrderIds, tasks);
        validateUnusedMaterialRequirements(workOrderIds);
        LocalDateTime now = LocalDateTime.now();
        closeTasksAndDispatches(tasks, dispatchOrders, operatorId, remark, now);
        restoreWorkOrderToDraft(workOrder, operatorId, remark, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgProductionOrder releaseOrder(Long productionOrderId, Long operatorId, String remark) {
        MfgProductionOrder existing = getOrderById(productionOrderId);
        if (existing == null) {
            throw business(ResultCode.NOT_FOUND, "生产订单不存在或已删除");
        }
        if (!BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有 DRAFT 状态的生产订单可以下达");
        }
        if (operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "操作人不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        MfgProductionOrder update = new MfgProductionOrder();
        update.setProductionOrderId(productionOrderId);
        update.setStatus(BusinessStatus.Document.RELEASED);
        update.setVersion(existing.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        update.setRemark(StringUtils.hasText(remark) ? remark : existing.getRemark());
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产订单已被其他用户修改，请刷新后重试");
        }
        insertStatusLog("MFG_PRODUCTION_ORDER", productionOrderId,
                existing.getStatus(), BusinessStatus.Document.RELEASED,
                "RELEASE_PRODUCTION_ORDER", operatorId, remark, now);
        insertOperationLog("mfg_production_order", productionOrderId,
                "RELEASE_PRODUCTION_ORDER", operatorId, remark, now);
        return getOrderById(productionOrderId);
    }

    /**
     * 查询生产订单列表，支持关键字搜索、状态筛选、客户筛选和产品筛选。
     * @param keyword 关键字（匹配订单编号）
     * @param status 订单状态
     * @param customerId 客户ID
     * @param productId 产品ID
     * @return 生产订单列表
     */
    @Override
    public List<MfgProductionOrder> listOrders(String keyword, String status, Long customerId, Long productId) {
        return baseMapper.selectList(buildOrderQuery(keyword, status, customerId, productId));
    }

    /**
     * 构建生产订单查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @param customerId 客户ID
     * @param productId 产品ID
     * @return 查询条件包装器
     */
    private QueryWrapper<MfgProductionOrder> buildOrderQuery(String keyword, String status, Long customerId, Long productId) {
        QueryWrapper<MfgProductionOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.like("production_order_no", keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (customerId != null) {
            wrapper.eq("customer_id", customerId);
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        wrapper.orderByDesc("production_order_id");
        return wrapper;
    }

    private MfgProductionOrder activeOrderForUpdate(Long productionOrderId) {
        QueryWrapper<MfgProductionOrder> query = new QueryWrapper<>();
        query.eq("production_order_id", productionOrderId)
                .eq("deleted_flag", 0)
                .last("FOR UPDATE");
        return baseMapper.selectOne(query);
    }

    private MfgProductionPlan planForUpdate(Long planId) {
        QueryWrapper<MfgProductionPlan> query = new QueryWrapper<>();
        query.eq("plan_id", planId).eq("deleted_flag", 0).last("FOR UPDATE");
        return productionPlanMapper.selectOne(query);
    }

    private MfgWorkOrder workOrderForUpdate(Long workOrderId) {
        QueryWrapper<MfgWorkOrder> query = new QueryWrapper<>();
        query.eq("work_order_id", workOrderId).eq("deleted_flag", 0).last("FOR UPDATE");
        return workOrderMapper.selectOne(query);
    }

    private List<MfgProductionPlan> activePlansForUpdate(Long productionOrderId) {
        QueryWrapper<MfgProductionPlan> query = new QueryWrapper<>();
        query.eq("production_order_id", productionOrderId)
                .eq("deleted_flag", 0)
                .ne("status", BusinessStatus.Document.CLOSED)
                .last("FOR UPDATE");
        return productionPlanMapper.selectList(query);
    }

    private List<MfgWorkOrder> activeWorkOrdersForUpdate(List<Long> planIds) {
        if (planIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper<MfgWorkOrder> query = new QueryWrapper<>();
        query.in("plan_id", planIds)
                .eq("deleted_flag", 0)
                .ne("status", BusinessStatus.Document.CLOSED)
                .last("FOR UPDATE");
        return workOrderMapper.selectList(query);
    }

    private List<MfgDispatchOrder> activeDispatchOrdersForUpdate(List<Long> workOrderIds) {
        if (workOrderIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper<MfgDispatchOrder> query = new QueryWrapper<>();
        query.in("work_order_id", workOrderIds)
                .eq("deleted_flag", 0)
                .last("FOR UPDATE");
        return dispatchOrderMapper.selectList(query);
    }

    private List<MfgTask> activeTasksForUpdate(List<Long> workOrderIds) {
        if (workOrderIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper<MfgTask> query = new QueryWrapper<>();
        query.in("work_order_id", workOrderIds)
                .eq("deleted_flag", 0)
                .last("FOR UPDATE");
        return taskMapper.selectList(query);
    }

    private void validateRemovablePlans(List<MfgProductionPlan> plans) {
        for (MfgProductionPlan plan : plans) {
            String status = plan.getStatus();
            if (status == null || !(BusinessStatus.Document.DRAFT.equals(status)
                    || BusinessStatus.Document.RELEASED.equals(status)
                    || BusinessStatus.Document.CLOSED.equals(status))) {
                throw business(ResultCode.BUSINESS_ERROR, "生产计划已开始执行或完成，不能撤回生产订单");
            }
        }
    }

    private void validateRemovableWorkOrders(List<MfgWorkOrder> workOrders) {
        for (MfgWorkOrder workOrder : workOrders) {
            String status = workOrder.getStatus();
            boolean removableStatus = BusinessStatus.Document.DRAFT.equals(status)
                    || BusinessStatus.Document.APPROVED.equals(status)
                    || BusinessStatus.Document.RELEASED.equals(status)
                    || BusinessStatus.Document.CLOSED.equals(status);
            boolean hasCompletedQuantity = workOrder.getCompletedQuantity() != null
                    && workOrder.getCompletedQuantity().compareTo(BigDecimal.ZERO) > 0;
            if (!removableStatus || hasCompletedQuantity) {
                throw business(ResultCode.BUSINESS_ERROR, "生产工单已经开工或产生完成数量，不能撤回生产订单");
            }
        }
    }

    private void validateUnexecutedTasks(List<MfgTask> tasks) {
        for (MfgTask task : tasks) {
            boolean unexecutedStatus = BusinessStatus.Production.WAIT_START.equals(task.getStatus())
                    || BusinessStatus.Document.CLOSED.equals(task.getStatus());
            boolean hasQuantity = positive(task.getCompletedQuantity()) || positive(task.getDefectQuantity());
            if (!unexecutedStatus || hasQuantity) {
                throw business(ResultCode.BUSINESS_ERROR, "生产任务已经开工或产生完成/不良数量，不能撤回生产订单");
            }
        }
    }

    private void validateUnexecutedDispatchOrders(List<MfgDispatchOrder> dispatchOrders) {
        for (MfgDispatchOrder dispatchOrder : dispatchOrders) {
            String status = dispatchOrder.getStatus();
            boolean unexecutedStatus = "WAIT_DISPATCH".equals(status)
                    || BusinessStatus.Document.RELEASED.equals(status)
                    || "ACCEPTED".equals(status)
                    || BusinessStatus.Document.REJECTED.equals(status);
            if (!unexecutedStatus) {
                throw business(ResultCode.BUSINESS_ERROR, "派工单已经进入生产或完成状态，不能撤回生产订单");
            }
        }
    }

    private void requireWithdrawArguments(Long id, Long operatorId, String remark, String label) {
        if (id == null || operatorId == null || !StringUtils.hasText(remark)) {
            throw business(ResultCode.PARAM_ERROR, label + "、操作人和撤回原因不能为空");
        }
    }

    private void withdrawWorkOrdersAndExecution(List<MfgWorkOrder> workOrders, Long operatorId,
                                                String remark, LocalDateTime now) {
        List<Long> workOrderIds = workOrders.stream().map(MfgWorkOrder::getWorkOrderId).toList();
        List<MfgDispatchOrder> dispatchOrders = activeDispatchOrdersForUpdate(workOrderIds);
        List<MfgTask> tasks = activeTasksForUpdate(workOrderIds);
        validateUnexecutedTasks(tasks);
        validateUnexecutedDispatchOrders(dispatchOrders);
        rejectExistingExecutionData(workOrderIds, tasks);
        validateUnusedMaterialRequirements(workOrderIds);
        closeTasksAndDispatches(tasks, dispatchOrders, operatorId, remark, now);
        for (MfgWorkOrder workOrder : workOrders) {
            closeWorkOrder(workOrder, operatorId, remark, now);
        }
    }

    private void closeTasksAndDispatches(List<MfgTask> tasks, List<MfgDispatchOrder> dispatchOrders,
                                         Long operatorId, String remark, LocalDateTime now) {
        for (MfgTask task : tasks) {
            closeTask(task, operatorId, remark, now);
        }
        for (MfgDispatchOrder dispatchOrder : dispatchOrders) {
            rejectDispatchOrder(dispatchOrder, operatorId, remark, now);
        }
    }

    private void rejectExistingExecutionData(List<Long> workOrderIds, List<MfgTask> tasks) {
        if (workOrderIds.isEmpty()) {
            return;
        }
        List<Long> taskIds = tasks.stream().map(MfgTask::getTaskId).toList();
        if (!taskIds.isEmpty() && hasEffectiveWorkReports(taskIds)) {
            throw business(ResultCode.BUSINESS_ERROR, "生产任务已经产生报工记录，不能撤回生产订单");
        }
        if (hasOpenWarehouseOrders(outboundOrderMapper, workOrderIds)
                || hasOpenWarehouseOrders(returnOrderMapper, workOrderIds)) {
            throw business(ResultCode.BUSINESS_ERROR, "订单已经产生领料或退料单，请先在仓储流程撤回对应单据");
        }
        if (hasOpenInspectionOrders(workOrderIds)) {
            throw business(ResultCode.BUSINESS_ERROR, "订单已经产生质量检验单，不能撤回生产订单");
        }
        if (hasActiveRecords(productLotMapper, "work_order_id", workOrderIds)
                || hasActiveRecords(materialUsageMapper, "work_order_id", workOrderIds)) {
            throw business(ResultCode.BUSINESS_ERROR, "订单已经产生批次或物料追溯记录，不能撤回生产订单");
        }
    }

    private boolean hasEffectiveWorkReports(List<Long> taskIds) {
        QueryWrapper<com.neuedu.ems.entity.mfg.MfgWorkReport> query = new QueryWrapper<>();
        query.in("task_id", taskIds).eq("deleted_flag", 0)
                .ne("audit_status", BusinessStatus.Document.WITHDRAWN);
        return workReportMapper.selectCount(query) > 0;
    }

    private void validateUnusedMaterialRequirements(List<Long> workOrderIds) {
        if (workOrderIds.isEmpty()) {
            return;
        }
        QueryWrapper<MfgWorkOrderMaterial> query = new QueryWrapper<>();
        query.in("work_order_id", workOrderIds)
                .eq("deleted_flag", 0)
                .last("FOR UPDATE");
        List<MfgWorkOrderMaterial> requirements = workOrderMaterialMapper.selectList(query);
        boolean issued = requirements.stream().anyMatch(item -> item.getIssuedQuantity() != null
                && item.getIssuedQuantity().compareTo(BigDecimal.ZERO) > 0);
        if (issued) {
            throw business(ResultCode.BUSINESS_ERROR, "工单已经发料，不能撤回生产订单");
        }
    }

    private void closeTask(MfgTask existing, Long operatorId, String remark, LocalDateTime now) {
        if (BusinessStatus.Document.CLOSED.equals(existing.getStatus())) {
            return;
        }
        MfgTask update = new MfgTask();
        update.setTaskId(existing.getTaskId());
        update.setStatus(BusinessStatus.Document.CLOSED);
        update.setVersion(existing.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (taskMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产任务已被其他用户修改，请刷新后重试");
        }
        insertStatusLog("MFG_TASK", existing.getTaskId(), existing.getStatus(), BusinessStatus.Document.CLOSED,
                "WITHDRAW_UNEXECUTED_ORDER_CHAIN", operatorId, remark, now);
        insertOperationLog("mfg_task", existing.getTaskId(),
                "WITHDRAW_UNEXECUTED_ORDER_CHAIN", operatorId, remark, now);
        responsibilityNotificationService.cancelOpenTaskNotifications(existing.getTaskId(), operatorId);
    }

    private void rejectDispatchOrder(MfgDispatchOrder existing, Long operatorId, String remark, LocalDateTime now) {
        if (BusinessStatus.Document.REJECTED.equals(existing.getStatus())) {
            return;
        }
        MfgDispatchOrder update = new MfgDispatchOrder();
        update.setDispatchId(existing.getDispatchId());
        update.setStatus(BusinessStatus.Document.REJECTED);
        update.setVersion(existing.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (dispatchOrderMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "派工单已被其他用户修改，请刷新后重试");
        }
        insertStatusLog("MFG_DISPATCH_ORDER", existing.getDispatchId(), existing.getStatus(),
                BusinessStatus.Document.REJECTED, "WITHDRAW_UNEXECUTED_ORDER_CHAIN", operatorId, remark, now);
        insertOperationLog("mfg_dispatch_order", existing.getDispatchId(),
                "WITHDRAW_UNEXECUTED_ORDER_CHAIN", operatorId, remark, now);
    }

    private void closeWorkOrder(MfgWorkOrder existing, Long operatorId, String remark, LocalDateTime now) {
        MfgWorkOrder update = new MfgWorkOrder();
        update.setWorkOrderId(existing.getWorkOrderId());
        update.setStatus(BusinessStatus.Document.CLOSED);
        update.setVersion(existing.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (workOrderMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产工单已被其他用户修改，请刷新后重试");
        }
        if (!BusinessStatus.Document.CLOSED.equals(existing.getStatus())) {
            insertStatusLog("MFG_WORK_ORDER", existing.getWorkOrderId(), existing.getStatus(),
                    BusinessStatus.Document.CLOSED, "WITHDRAW_UNEXECUTED_ORDER_CHAIN", operatorId, remark, now);
        }
        insertOperationLog("mfg_work_order", existing.getWorkOrderId(),
                "WITHDRAW_UNEXECUTED_ORDER_CHAIN", operatorId, remark, now);
    }

    private void closePlan(MfgProductionPlan existing, Long operatorId, String remark, LocalDateTime now) {
        MfgProductionPlan update = new MfgProductionPlan();
        update.setPlanId(existing.getPlanId());
        update.setStatus(BusinessStatus.Document.CLOSED);
        update.setVersion(existing.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (productionPlanMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产计划已被其他用户修改，请刷新后重试");
        }
        if (!BusinessStatus.Document.CLOSED.equals(existing.getStatus())) {
            insertStatusLog("MFG_PRODUCTION_PLAN", existing.getPlanId(), existing.getStatus(),
                    BusinessStatus.Document.CLOSED, "WITHDRAW_UNEXECUTED_ORDER_CHAIN", operatorId, remark, now);
        }
        insertOperationLog("mfg_production_plan", existing.getPlanId(),
                "WITHDRAW_UNEXECUTED_ORDER_CHAIN", operatorId, remark, now);
    }

    private void restorePlanToDraft(MfgProductionPlan existing, Long operatorId, String remark, LocalDateTime now) {
        MfgProductionPlan update = new MfgProductionPlan();
        update.setPlanId(existing.getPlanId());
        update.setStatus(BusinessStatus.Document.DRAFT);
        update.setVersion(existing.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (productionPlanMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产计划已被其他用户修改，请刷新后重试");
        }
        insertStatusLog("MFG_PRODUCTION_PLAN", existing.getPlanId(), existing.getStatus(),
                BusinessStatus.Document.DRAFT, "WITHDRAW_UNEXECUTED_PLAN_CHAIN", operatorId, remark, now);
        insertOperationLog("mfg_production_plan", existing.getPlanId(),
                "WITHDRAW_UNEXECUTED_PLAN_CHAIN", operatorId, remark, now);
    }

    private void restoreWorkOrderToDraft(MfgWorkOrder existing, Long operatorId, String remark, LocalDateTime now) {
        MfgWorkOrder update = new MfgWorkOrder();
        update.setWorkOrderId(existing.getWorkOrderId());
        update.setStatus(BusinessStatus.Document.DRAFT);
        update.setVersion(existing.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (workOrderMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产工单已被其他用户修改，请刷新后重试");
        }
        insertStatusLog("MFG_WORK_ORDER", existing.getWorkOrderId(), existing.getStatus(),
                BusinessStatus.Document.DRAFT, "WITHDRAW_UNEXECUTED_WORK_ORDER_CHAIN", operatorId, remark, now);
        insertOperationLog("mfg_work_order", existing.getWorkOrderId(),
                "WITHDRAW_WORK_ORDER", operatorId, remark, now);
    }

    private void restoreOrderToDraft(MfgProductionOrder existing, Long operatorId, String remark, LocalDateTime now) {
        MfgProductionOrder update = new MfgProductionOrder();
        update.setProductionOrderId(existing.getProductionOrderId());
        update.setStatus(BusinessStatus.Document.DRAFT);
        update.setVersion(existing.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产订单已被其他用户修改，请刷新后重试");
        }
        insertStatusLog("MFG_PRODUCTION_ORDER", existing.getProductionOrderId(), existing.getStatus(),
                BusinessStatus.Document.DRAFT, "WITHDRAW_UNEXECUTED_ORDER_CHAIN", operatorId, remark, now);
        insertOperationLog("mfg_production_order", existing.getProductionOrderId(),
                "WITHDRAW_UNEXECUTED_ORDER_CHAIN", operatorId, remark, now);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private <T> boolean hasActiveRecords(BaseMapper<T> mapper, String field, List<Long> values) {
        QueryWrapper<T> query = new QueryWrapper<>();
        query.in(field, values).eq("deleted_flag", 0);
        return mapper.selectCount(query) > 0;
    }

    private <T> boolean hasOpenWarehouseOrders(BaseMapper<T> mapper, List<Long> workOrderIds) {
        QueryWrapper<T> query = new QueryWrapper<>();
        query.in("work_order_id", workOrderIds).eq("deleted_flag", 0)
                .ne("audit_status", BusinessStatus.Document.CLOSED);
        return mapper.selectCount(query) > 0;
    }

    private boolean hasOpenInspectionOrders(List<Long> workOrderIds) {
        QueryWrapper<com.neuedu.ems.entity.qms.QmsInspectionOrder> query = new QueryWrapper<>();
        query.in("work_order_id", workOrderIds).eq("deleted_flag", 0)
                .ne("status", BusinessStatus.Document.CLOSED);
        return inspectionOrderMapper.selectCount(query) > 0;
    }

    private void insertStatusLog(String bizType, Long bizId, String oldStatus, String newStatus, String reason,
                                 Long operatorId, String remark, LocalDateTime now) {
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

    private void insertOperationLog(String moduleCode, Long bizId, String type,
                                    Long operatorId, String remark, LocalDateTime now) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(operatorId);
        log.setOperationType(type);
        log.setModuleCode(moduleCode);
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

    private BusinessException business(ResultCode code, String message) {
        return new BusinessException(code.getCode(), message);
    }
}
