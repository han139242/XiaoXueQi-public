package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.mfg.MfgWorkOrderService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 生产工单管理 Service 实现类。
 */
@Service
public class MfgWorkOrderServiceImpl extends ServiceImpl<MfgWorkOrderMapper, MfgWorkOrder> implements MfgWorkOrderService {

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private MfgProductionPlanMapper productionPlanMapper;

    @Autowired
    private MfgProductionOrderMapper productionOrderMapper;

    @Autowired
    private MfgTaskMapper mfgTaskMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 分页查询生产工单列表，支持关键字搜索、状态筛选、生产计划筛选和产品筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配工单编号）
     * @param status 工单状态
     * @param planId 生产计划ID
     * @param productId 产品ID
     * @return 分页结果
     */
    @Override
    public Page<MfgWorkOrder> pageWorkOrders(Integer current, Integer size, String keyword, String status, Long planId, Long productId) {
        Page<MfgWorkOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildWorkOrderQuery(keyword, status, planId, productId));
    }

    /**
     * 根据生产工单ID查询生产工单信息。
     * @param workOrderId 生产工单ID
     * @return 生产工单实体
     */
    @Override
    public MfgWorkOrder getWorkOrderById(Long workOrderId) {
        QueryWrapper<MfgWorkOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("work_order_id", workOrderId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增生产工单，自动设置删除标志为0。
     * @param workOrder 生产工单实体
     * @return 是否成功
     */
    @Override
    public boolean saveWorkOrder(MfgWorkOrder workOrder) {
        if (workOrder == null || !StringUtils.hasText(workOrder.getWorkOrderNo()) || workOrder.getPlanId() == null
                || workOrder.getProductId() == null || workOrder.getBomId() == null || workOrder.getRouteId() == null
                || workOrder.getPlannedQuantity() == null || workOrder.getPlannedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        workOrder.setStatus(BusinessStatus.Document.DRAFT);
        workOrder.setCompletedQuantity(BigDecimal.ZERO);
        if (workOrder.getDeletedFlag() == null) {
            workOrder.setDeletedFlag(0);
        }
        return baseMapper.insert(workOrder) > 0;
    }

    /**
     * 更新生产工单信息，不允许修改删除标志。
     * @param workOrder 生产工单实体
     * @return 是否成功
     */
    @Override
    public boolean updateWorkOrder(MfgWorkOrder workOrder) {
        if (workOrder.getWorkOrderId() == null) {
            return false;
        }
        MfgWorkOrder existing = getWorkOrderById(workOrder.getWorkOrderId());
        if (existing == null || !BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            return false;
        }
        workOrder.setDeletedFlag(null);
        workOrder.setStatus(null);
        workOrder.setCompletedQuantity(null);
        if (workOrder.getVersion() == null) {
            workOrder.setVersion(existing.getVersion());
        }
        return baseMapper.updateById(workOrder) > 0;
    }

    /**
     * 根据ID删除生产工单（软删除，将删除标志设为1）。
     * @param workOrderId 生产工单ID
     * @return 是否成功
     */
    @Override
    public boolean removeWorkOrderById(Long workOrderId) {
        MfgWorkOrder existing = getWorkOrderById(workOrderId);
        if (existing == null || !BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            return false;
        }
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(workOrderId);
        workOrder.setVersion(existing.getVersion());
        workOrder.setDeletedFlag(1);
        return baseMapper.updateById(workOrder) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgWorkOrder generateWorkOrder(Long planId, MfgWorkOrder request, Long operatorId) {
        if (planId == null || request == null || operatorId == null || request.getBomId() == null
                || request.getRouteId() == null) {
            throw business(ResultCode.PARAM_ERROR, "生产计划、BOM、工艺路线和操作人不能为空");
        }
        MfgProductionPlan plan = activePlan(planId);
        if (plan == null) {
            throw business(ResultCode.NOT_FOUND, "生产计划不存在或已删除");
        }
        if (!BusinessStatus.Document.RELEASED.equals(plan.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有 RELEASED 状态的生产计划可以生成工单");
        }
        if (hasActiveWorkOrder(planId)) {
            throw business(ResultCode.BUSINESS_ERROR, "该生产计划已生成工单，不能重复生成");
        }
        MfgProductionOrder order = activeOrder(plan.getProductionOrderId());
        if (order == null || order.getProductId() == null) {
            throw business(ResultCode.BUSINESS_ERROR, "生产计划未关联有效产品订单");
        }
        if (request.getProductId() != null && !request.getProductId().equals(order.getProductId())) {
            throw business(ResultCode.BUSINESS_ERROR, "工单产品必须与生产订单产品一致");
        }
        BigDecimal quantity = request.getPlannedQuantity() == null ? plan.getPlannedQuantity() : request.getPlannedQuantity();
        if (quantity == null || plan.getPlannedQuantity() == null || quantity.compareTo(plan.getPlannedQuantity()) != 0) {
            throw business(ResultCode.BUSINESS_ERROR, "单工单生成模式下，工单数量必须等于计划数量");
        }
        LocalDateTime now = LocalDateTime.now();
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderNo(String.format("WO-%s-%03d-1", now.format(NUMBER_DATE), planId));
        workOrder.setPlanId(planId);
        workOrder.setProductId(order.getProductId());
        workOrder.setBomId(request.getBomId());
        workOrder.setRouteId(request.getRouteId());
        workOrder.setPlannedQuantity(quantity);
        workOrder.setCompletedQuantity(BigDecimal.ZERO);
        workOrder.setStatus(BusinessStatus.Document.DRAFT);
        workOrder.setRemark(request.getRemark());
        workOrder.setVersion(0);
        workOrder.setCreatedBy(operatorId);
        workOrder.setCreatedTime(now);
        workOrder.setUpdatedBy(operatorId);
        workOrder.setUpdatedTime(now);
        workOrder.setDeletedFlag(0);
        if (baseMapper.insert(workOrder) == 0 || workOrder.getWorkOrderId() == null) {
            throw new BusinessException(ResultCode.FAILED);
        }
        insertStatusLog(workOrder.getWorkOrderId(), null, BusinessStatus.Document.DRAFT,
                "GENERATE_WORK_ORDER", operatorId, request.getRemark(), now);
        insertOperationLog(workOrder.getWorkOrderId(), "GENERATE_WORK_ORDER", operatorId, request.getRemark(), now);
        return workOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgWorkOrder releaseWorkOrder(Long workOrderId, Long operatorId, String remark) {
        MfgWorkOrder existing = getWorkOrderById(workOrderId);
        if (existing == null) {
            throw business(ResultCode.NOT_FOUND, "生产工单不存在或已删除");
        }
        if (!List.of(BusinessStatus.Document.DRAFT, BusinessStatus.Document.APPROVED).contains(existing.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有 DRAFT 或 APPROVED 状态的生产工单可以下达");
        }
        if (operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "操作人不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        MfgWorkOrder update = new MfgWorkOrder();
        update.setWorkOrderId(workOrderId);
        update.setStatus(BusinessStatus.Document.RELEASED);
        update.setVersion(existing.getVersion());
        update.setRemark(StringUtils.hasText(remark) ? remark : existing.getRemark());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产工单已被其他用户修改，请刷新后重试");
        }
        insertStatusLog(workOrderId, existing.getStatus(), BusinessStatus.Document.RELEASED,
                "RELEASE_WORK_ORDER", operatorId, remark, now);
        insertOperationLog(workOrderId, "RELEASE_WORK_ORDER", operatorId, remark, now);
        return getWorkOrderById(workOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgWorkOrder startWorkOrder(Long workOrderId, Long operatorId) {
        MfgWorkOrder existing = getWorkOrderById(workOrderId);
        if (existing == null) {
            throw business(ResultCode.NOT_FOUND, "生产工单不存在或已删除");
        }
        if (!BusinessStatus.Document.RELEASED.equals(existing.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有待开始的已下达工单可以开始生产");
        }
        if (operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "操作人不能为空");
        }
        long taskCount = mfgTaskMapper.selectCount(new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getWorkOrderId, workOrderId)
                .eq(MfgTask::getDeletedFlag, 0));
        if (taskCount == 0) {
            throw business(ResultCode.BUSINESS_ERROR, "工单尚未生成工序任务，不能开始生产");
        }
        long nonWaitingTaskCount = mfgTaskMapper.selectCount(new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getWorkOrderId, workOrderId)
                .eq(MfgTask::getDeletedFlag, 0)
                .ne(MfgTask::getStatus, BusinessStatus.Production.WAIT_START));
        if (nonWaitingTaskCount > 0) {
            throw business(ResultCode.BUSINESS_ERROR, "工单存在非待开始工序任务，请先修正任务状态");
        }
        LocalDateTime now = LocalDateTime.now();
        MfgWorkOrder update = new MfgWorkOrder();
        update.setWorkOrderId(workOrderId);
        update.setStatus(BusinessStatus.Production.RUNNING);
        update.setVersion(existing.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产工单已被其他用户修改，请刷新后重试");
        }
        insertStatusLog(workOrderId, existing.getStatus(), BusinessStatus.Production.RUNNING,
                "START_WORK_ORDER", operatorId, null, now);
        insertOperationLog(workOrderId, "START_WORK_ORDER", operatorId, null, now);
        return getWorkOrderById(workOrderId);
    }

    /**
     * 查询生产工单列表，支持关键字搜索、状态筛选、生产计划筛选和产品筛选。
     * @param keyword 关键字（匹配工单编号）
     * @param status 工单状态
     * @param planId 生产计划ID
     * @param productId 产品ID
     * @return 生产工单列表
     */
    @Override
    public List<MfgWorkOrder> listWorkOrders(String keyword, String status, Long planId, Long productId) {
        return baseMapper.selectList(buildWorkOrderQuery(keyword, status, planId, productId));
    }

    /**
     * 构建生产工单查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @param planId 生产计划ID
     * @param productId 产品ID
     * @return 查询条件包装器
     */
    private QueryWrapper<MfgWorkOrder> buildWorkOrderQuery(String keyword, String status, Long planId, Long productId) {
        QueryWrapper<MfgWorkOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.like("work_order_no", keyword);
        }
        if (StringUtils.hasText(status)) {
            if (BusinessStatus.Production.WAIT_START.equals(status)) {
                wrapper.in("status", BusinessStatus.Document.DRAFT, BusinessStatus.Document.APPROVED,
                        BusinessStatus.Document.RELEASED, BusinessStatus.Production.WAIT_START);
            } else {
                wrapper.eq("status", status);
            }
        }
        if (planId != null) {
            wrapper.eq("plan_id", planId);
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        wrapper.orderByDesc("work_order_id");
        return wrapper;
    }

    private MfgProductionPlan activePlan(Long planId) {
        QueryWrapper<MfgProductionPlan> query = new QueryWrapper<>();
        query.eq("plan_id", planId)
                .eq("deleted_flag", 0)
                .last("FOR UPDATE");
        return productionPlanMapper.selectOne(query);
    }

    private MfgProductionOrder activeOrder(Long productionOrderId) {
        QueryWrapper<MfgProductionOrder> query = new QueryWrapper<>();
        query.eq("production_order_id", productionOrderId).eq("deleted_flag", 0);
        return productionOrderMapper.selectOne(query);
    }

    private boolean hasActiveWorkOrder(Long planId) {
        QueryWrapper<MfgWorkOrder> query = new QueryWrapper<>();
        query.eq("plan_id", planId).eq("deleted_flag", 0).ne("status", BusinessStatus.Document.CLOSED);
        return baseMapper.selectCount(query) > 0;
    }

    private void insertStatusLog(Long bizId, String oldStatus, String newStatus, String reason,
                                 Long operatorId, String remark, LocalDateTime now) {
        SysStatusChangeLog log = new SysStatusChangeLog();
        log.setBizType("MFG_WORK_ORDER");
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
        log.setModuleCode("mfg_work_order");
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
