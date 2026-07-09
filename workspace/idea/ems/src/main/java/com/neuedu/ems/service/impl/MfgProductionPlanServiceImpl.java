package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.MfgProductionPlan;
import com.neuedu.ems.mapper.MfgProductionPlanMapper;
import com.neuedu.ems.service.MfgProductionPlanService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 生产计划管理 Service 实现类。
 */
@Service
public class MfgProductionPlanServiceImpl extends ServiceImpl<MfgProductionPlanMapper, MfgProductionPlan> implements MfgProductionPlanService {

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
        if (plan.getDeletedFlag() == null) {
            plan.setDeletedFlag(0);
        }
        return baseMapper.insert(plan) > 0;
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
        plan.setDeletedFlag(null);
        return baseMapper.updateById(plan) > 0;
    }

    /**
     * 根据ID删除生产计划（软删除，将删除标志设为1）。
     * @param planId 生产计划ID
     * @return 是否成功
     */
    @Override
    public boolean removePlanById(Long planId) {
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanId(planId);
        plan.setDeletedFlag(1);
        return baseMapper.updateById(plan) > 0;
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
}
