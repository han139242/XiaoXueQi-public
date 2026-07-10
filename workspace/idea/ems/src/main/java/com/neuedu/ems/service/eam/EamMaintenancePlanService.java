package com.neuedu.ems.service.eam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.eam.EamMaintenancePlan;

import java.util.List;

/**
 * 保养计划 Service 接口。
 */
public interface EamMaintenancePlanService extends IService<EamMaintenancePlan> {

    /**
     * 分页查询保养计划，支持设备、保养类型、周期类型、负责人和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param equipmentId 设备ID
     * @param maintenanceType 保养类型
     * @param cycleType 周期类型
     * @param responsibleId 负责人ID
     * @param status 状态
     * @return 分页结果
     */
    Page<EamMaintenancePlan> pageMaintenancePlans(Integer current, Integer size, Long equipmentId, String maintenanceType,
                                                  String cycleType, Long responsibleId, String status);

    /**
     * 查询保养计划列表，支持设备、保养类型、周期类型、负责人和状态筛选。
     * @param equipmentId 设备ID
     * @param maintenanceType 保养类型
     * @param cycleType 周期类型
     * @param responsibleId 负责人ID
     * @param status 状态
     * @return 保养计划列表
     */
    List<EamMaintenancePlan> listMaintenancePlans(Long equipmentId, String maintenanceType, String cycleType,
                                                  Long responsibleId, String status);

    /**
     * 根据保养计划ID查询未删除的计划详情。
     * @param planId 保养计划ID
     * @return 保养计划实体
     */
    EamMaintenancePlan getMaintenancePlanById(Long planId);

    /**
     * 新增保养计划。
     * @param maintenancePlan 保养计划实体
     * @return 是否成功
     */
    boolean saveMaintenancePlan(EamMaintenancePlan maintenancePlan);

    /**
     * 更新保养计划。
     * @param maintenancePlan 保养计划实体，必须包含planId
     * @return 是否成功
     */
    boolean updateMaintenancePlan(EamMaintenancePlan maintenancePlan);

    /**
     * 根据ID软删除保养计划。
     * @param planId 保养计划ID
     * @return 是否成功
     */
    boolean removeMaintenancePlanById(Long planId);
}
