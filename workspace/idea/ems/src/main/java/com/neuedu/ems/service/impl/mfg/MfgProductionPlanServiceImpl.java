package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.mfg.MfgProductionPlanService;
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
 * 生产计划管理 Service 实现类。
 */
@Service
public class MfgProductionPlanServiceImpl extends ServiceImpl<MfgProductionPlanMapper, MfgProductionPlan> implements MfgProductionPlanService {

    @Autowired
    private MfgProductionOrderService productionOrderService;

    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired
    private MfgProductionOrderMapper productionOrderMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 分页查询生产计划列表，支持关键字搜索、状态筛选、生产订单筛选和产线筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配计划编号）
     * @param status 计划状态
     * @param productionOrderId 生产订单ID
     * @param lineId 产线ID
     * @return 分页结果
     */
    @Override
    public Page<MfgProductionPlan> pagePlans(Integer current, Integer size, String keyword, String status, Long productionOrderId, Long lineId) {
        Page<MfgProductionPlan> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildPlanQuery(keyword, status, productionOrderId, lineId));
    }

    /**
     * 根据生产计划ID查询生产计划信息。
     * @param planId 生产计划ID
     * @return 生产计划实体
     */
    @Override
    public MfgProductionPlan getPlanById(Long planId) {
        QueryWrapper<MfgProductionPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("plan_id", planId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增生产计划，自动设置删除标志为0。
     * @param plan 生产计划实体
     * @return 是否成功
     */
    @Override
    public boolean savePlan(MfgProductionPlan plan) {
        throw business(ResultCode.BUSINESS_ERROR, "生产计划必须从已下达生产订单生成，不能脱离来源手工新增");
    }

    /**
     * 更新生产计划信息，不允许修改删除标志。
     * @param plan 生产计划实体
     * @return 是否成功
     */
    @Override
    public boolean updatePlan(MfgProductionPlan plan) {
        if (plan.getPlanId() == null) {
            return false;
        }
        MfgProductionPlan existing = getPlanById(plan.getPlanId());
        if (existing == null || !BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            return false;
        }
        plan.setDeletedFlag(null);
        plan.setStatus(null);
        plan.setPlanNo(null);
        plan.setProductionOrderId(null);
        if (plan.getVersion() == null) {
            plan.setVersion(existing.getVersion());
        }
        return baseMapper.updateById(plan) > 0;
    }

    /**
     * 根据ID删除生产计划（软删除，将删除标志设为1）。
     * @param planId 生产计划ID
     * @return 是否成功
     */
    @Override
    public boolean removePlanById(Long planId) {
        MfgProductionPlan existing = getPlanById(planId);
        if (existing == null || !BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            return false;
        }
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(planId);
        plan.setVersion(existing.getVersion());
        plan.setDeletedFlag(1);
        return baseMapper.updateById(plan) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgProductionPlan generatePlan(Long productionOrderId, MfgProductionPlan request, Long operatorId) {
        if (productionOrderId == null || request == null || operatorId == null || request.getLineId() == null
                || !validTimeWindow(request) || request.getPlannedQuantity() == null
                || request.getPlannedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw business(ResultCode.PARAM_ERROR, "产线、计划时间、计划数量和操作人不能为空");
        }
        MfgProductionOrder order = activeOrder(productionOrderId);
        if (order == null) {
            throw business(ResultCode.NOT_FOUND, "生产订单不存在或已删除");
        }
        if (!BusinessStatus.Document.RELEASED.equals(order.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有 RELEASED 状态的生产订单可以生成计划");
        }
        BigDecimal planned = existingPlannedQuantity(productionOrderId);
        if (planned.add(request.getPlannedQuantity()).compareTo(order.getOrderQuantity()) > 0) {
            throw business(ResultCode.BUSINESS_ERROR, "累计计划数量不能超过生产订单数量");
        }
        LocalDateTime now = LocalDateTime.now();
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanNo("PLN-" + productionOrderId + "-" + now.format(NUMBER_TIME));
        plan.setProductionOrderId(productionOrderId);
        plan.setLineId(request.getLineId());
        plan.setPlannedStartTime(request.getPlannedStartTime());
        plan.setPlannedEndTime(request.getPlannedEndTime());
        plan.setPlannedQuantity(request.getPlannedQuantity());
        plan.setStatus(BusinessStatus.Document.RELEASED);
        plan.setRemark(request.getRemark());
        plan.setVersion(0);
        plan.setCreatedBy(operatorId);
        plan.setCreatedTime(now);
        plan.setUpdatedBy(operatorId);
        plan.setUpdatedTime(now);
        plan.setDeletedFlag(0);
        if (baseMapper.insert(plan) == 0 || plan.getPlanId() == null) {
            throw new BusinessException(ResultCode.FAILED);
        }
        insertStatusLog(plan.getPlanId(), null, BusinessStatus.Document.RELEASED,
                "GENERATE_PRODUCTION_PLAN", operatorId, request.getRemark(), now);
        insertOperationLog(plan.getPlanId(), "GENERATE_PRODUCTION_PLAN", operatorId, request.getRemark(), now);
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgProductionPlan releasePlan(Long planId, Long operatorId, String remark) {
        if (planId == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "生产计划和操作人不能为空");
        }
        MfgProductionPlan plan = baseMapper.selectOne(new QueryWrapper<MfgProductionPlan>()
                .eq("plan_id", planId)
                .eq("deleted_flag", 0)
                .last("FOR UPDATE"));
        if (plan == null) {
            throw business(ResultCode.NOT_FOUND, "生产计划不存在或已删除");
        }
        if (!BusinessStatus.Document.DRAFT.equals(plan.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有草稿生产计划可以重新生效");
        }
        if (plan.getLineId() == null || !validTimeWindow(plan) || plan.getPlannedQuantity() == null
                || plan.getPlannedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw business(ResultCode.BUSINESS_ERROR, "生产计划缺少有效产线、时间或计划数量");
        }
        MfgProductionOrder order = activeOrder(plan.getProductionOrderId());
        if (order == null || !BusinessStatus.Document.RELEASED.equals(order.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "生产计划的来源订单未处于已下达状态");
        }
        if (existingPlannedQuantity(plan.getProductionOrderId()).compareTo(order.getOrderQuantity()) > 0) {
            throw business(ResultCode.BUSINESS_ERROR, "累计计划数量不能超过生产订单数量");
        }
        LocalDateTime now = LocalDateTime.now();
        MfgProductionPlan update = new MfgProductionPlan();
        update.setPlanId(planId);
        update.setVersion(plan.getVersion());
        update.setStatus(BusinessStatus.Document.RELEASED);
        update.setRemark(StringUtils.hasText(remark) ? remark : plan.getRemark());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产计划已被其他用户修改，请刷新后重试");
        }
        insertStatusLog(planId, plan.getStatus(), BusinessStatus.Document.RELEASED,
                "RELEASE_PRODUCTION_PLAN", operatorId, remark, now);
        insertOperationLog(planId, "RELEASE_PRODUCTION_PLAN", operatorId, remark, now);
        return getPlanById(planId);
    }

    @Override
    public void withdrawPlan(Long planId, Long operatorId, String remark) {
        productionOrderService.withdrawUnexecutedPlanChain(planId, operatorId, remark);
    }

    /**
     * 查询生产计划列表，支持关键字搜索、状态筛选、生产订单筛选和产线筛选。
     * @param keyword 关键字（匹配计划编号）
     * @param status 计划状态
     * @param productionOrderId 生产订单ID
     * @param lineId 产线ID
     * @return 生产计划列表
     */
    @Override
    public List<MfgProductionPlan> listPlans(String keyword, String status, Long productionOrderId, Long lineId) {
        return baseMapper.selectList(buildPlanQuery(keyword, status, productionOrderId, lineId));
    }

    /**
     * 构建生产计划查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @param productionOrderId 生产订单ID
     * @param lineId 产线ID
     * @return 查询条件包装器
     */
    private QueryWrapper<MfgProductionPlan> buildPlanQuery(String keyword, String status, Long productionOrderId, Long lineId) {
        QueryWrapper<MfgProductionPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.like("plan_no", keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (productionOrderId != null) {
            wrapper.eq("production_order_id", productionOrderId);
        }
        if (lineId != null) {
            wrapper.eq("line_id", lineId);
        }
        wrapper.orderByDesc("plan_id");
        return wrapper;
    }

    private boolean validTimeWindow(MfgProductionPlan plan) {
        return plan.getPlannedStartTime() != null && plan.getPlannedEndTime() != null
                && plan.getPlannedEndTime().isAfter(plan.getPlannedStartTime());
    }

    private MfgProductionOrder activeOrder(Long productionOrderId) {
        QueryWrapper<MfgProductionOrder> query = new QueryWrapper<>();
        query.eq("production_order_id", productionOrderId)
                .eq("deleted_flag", 0)
                .last("FOR UPDATE");
        return productionOrderMapper.selectOne(query);
    }

    private BigDecimal existingPlannedQuantity(Long productionOrderId) {
        QueryWrapper<MfgProductionPlan> query = new QueryWrapper<>();
        query.select("COALESCE(SUM(planned_quantity), 0) AS planned_quantity")
                .eq("production_order_id", productionOrderId)
                .eq("deleted_flag", 0)
                .ne("status", BusinessStatus.Document.CLOSED);
        MfgProductionPlan total = baseMapper.selectOne(query);
        return total == null || total.getPlannedQuantity() == null ? BigDecimal.ZERO : total.getPlannedQuantity();
    }

    private void insertStatusLog(Long bizId, String oldStatus, String newStatus, String reason,
                                 Long operatorId, String remark, LocalDateTime now) {
        SysStatusChangeLog log = new SysStatusChangeLog();
        log.setBizType("MFG_PRODUCTION_PLAN");
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
        log.setModuleCode("mfg_production_plan");
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
