package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.MfgProductionPlan;

import java.util.List;

/**
 * 生产计划管理 Service 接口。
 */
public interface MfgProductionPlanService extends IService<MfgProductionPlan> {

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
    Page<MfgProductionPlan> pagePlans(Integer current, Integer size, String keyword, String status, Long productionOrderId, Long lineId);

    /**
     * 根据生产计划ID查询生产计划信息。
     * @param planId 生产计划ID
     * @return 生产计划实体
     */
    MfgProductionPlan getPlanById(Long planId);

    /**
     * 新增生产计划。
     * @param plan 生产计划实体
     * @return 是否成功
     */
    boolean savePlan(MfgProductionPlan plan);

    /**
     * 更新生产计划信息。
     * @param plan 生产计划实体
     * @return 是否成功
     */
    boolean updatePlan(MfgProductionPlan plan);

    /**
     * 根据ID删除生产计划（软删除）。
     * @param planId 生产计划ID
     * @return 是否成功
     */
    boolean removePlanById(Long planId);

    /**
     * 查询生产计划列表，支持关键字搜索、状态筛选、生产订单筛选和产线筛选。
     * @param keyword 关键字（匹配计划编号）
     * @param status 计划状态
     * @param productionOrderId 生产订单ID
     * @param lineId 产线ID
     * @return 生产计划列表
     */
    List<MfgProductionPlan> listPlans(String keyword, String status, Long productionOrderId, Long lineId);
}
