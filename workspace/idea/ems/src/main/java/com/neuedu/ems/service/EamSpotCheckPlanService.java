package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.EamSpotCheckPlan;

import java.util.List;

/**
 * 点检计划 Service 接口。
 */
public interface EamSpotCheckPlanService extends IService<EamSpotCheckPlan> {

    /**
     * 分页查询点检计划，支持设备、点检周期、负责岗位和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param equipmentId 设备ID
     * @param checkCycle 点检周期
     * @param responsibleRole 负责岗位
     * @param status 状态
     * @return 分页结果
     */
    Page<EamSpotCheckPlan> pageSpotCheckPlans(Integer current, Integer size, Long equipmentId,
                                              String checkCycle, String responsibleRole, String status);

    /**
     * 查询点检计划列表，支持设备、点检周期、负责岗位和状态筛选。
     * @param equipmentId 设备ID
     * @param checkCycle 点检周期
     * @param responsibleRole 负责岗位
     * @param status 状态
     * @return 点检计划列表
     */
    List<EamSpotCheckPlan> listSpotCheckPlans(Long equipmentId, String checkCycle, String responsibleRole, String status);

    /**
     * 根据点检计划ID查询未删除的计划详情。
     * @param planId 点检计划ID
     * @return 点检计划实体
     */
    EamSpotCheckPlan getSpotCheckPlanById(Long planId);

    /**
     * 新增点检计划。
     * @param spotCheckPlan 点检计划实体
     * @return 是否成功
     */
    boolean saveSpotCheckPlan(EamSpotCheckPlan spotCheckPlan);

    /**
     * 更新点检计划。
     * @param spotCheckPlan 点检计划实体，必须包含planId
     * @return 是否成功
     */
    boolean updateSpotCheckPlan(EamSpotCheckPlan spotCheckPlan);

    /**
     * 根据ID软删除点检计划。
     * @param planId 点检计划ID
     * @return 是否成功
     */
    boolean removeSpotCheckPlanById(Long planId);
}
