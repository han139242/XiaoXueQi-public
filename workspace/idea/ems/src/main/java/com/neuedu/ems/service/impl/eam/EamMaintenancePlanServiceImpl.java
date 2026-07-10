package com.neuedu.ems.service.impl.eam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.eam.EamMaintenancePlan;
import com.neuedu.ems.mapper.eam.EamMaintenancePlanMapper;
import com.neuedu.ems.service.eam.EamMaintenancePlanService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 保养计划 Service 实现类。
 */
@Service
public class EamMaintenancePlanServiceImpl extends ServiceImpl<EamMaintenancePlanMapper, EamMaintenancePlan>
        implements EamMaintenancePlanService {

    /**
     * 分页查询保养计划，默认过滤已删除记录。
     * @param current 当前页码
     * @param size 每页记录数
     * @param equipmentId 设备ID
     * @param maintenanceType 保养类型
     * @param cycleType 周期类型
     * @param responsibleId 负责人ID
     * @param status 状态
     * @return 分页结果
     */
    @Override
    public Page<EamMaintenancePlan> pageMaintenancePlans(Integer current, Integer size, Long equipmentId,
                                                         String maintenanceType, String cycleType,
                                                         Long responsibleId, String status) {
        Page<EamMaintenancePlan> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildMaintenancePlanQuery(equipmentId, maintenanceType, cycleType, responsibleId, status));
    }

    /**
     * 查询保养计划列表，默认过滤已删除记录。
     * @param equipmentId 设备ID
     * @param maintenanceType 保养类型
     * @param cycleType 周期类型
     * @param responsibleId 负责人ID
     * @param status 状态
     * @return 保养计划列表
     */
    @Override
    public List<EamMaintenancePlan> listMaintenancePlans(Long equipmentId, String maintenanceType, String cycleType,
                                                         Long responsibleId, String status) {
        return baseMapper.selectList(buildMaintenancePlanQuery(equipmentId, maintenanceType, cycleType, responsibleId, status));
    }

    /**
     * 根据保养计划ID查询未删除的计划详情。
     * @param planId 保养计划ID
     * @return 保养计划实体
     */
    @Override
    public EamMaintenancePlan getMaintenancePlanById(Long planId) {
        LambdaQueryWrapper<EamMaintenancePlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamMaintenancePlan::getPlanId, planId)
                .eq(EamMaintenancePlan::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增保养计划，自动设置删除标志为0。
     * @param maintenancePlan 保养计划实体
     * @return 是否成功
     */
    @Override
    public boolean saveMaintenancePlan(EamMaintenancePlan maintenancePlan) {
        if (maintenancePlan.getDeletedFlag() == null) {
            maintenancePlan.setDeletedFlag(0);
        }
        return baseMapper.insert(maintenancePlan) > 0;
    }

    /**
     * 更新保养计划，不允许通过普通更新修改删除标志。
     * @param maintenancePlan 保养计划实体
     * @return 是否成功
     */
    @Override
    public boolean updateMaintenancePlan(EamMaintenancePlan maintenancePlan) {
        if (maintenancePlan.getPlanId() == null) {
            return false;
        }
        maintenancePlan.setDeletedFlag(null);
        return baseMapper.updateById(maintenancePlan) > 0;
    }

    /**
     * 根据ID软删除保养计划。
     * @param planId 保养计划ID
     * @return 是否成功
     */
    @Override
    public boolean removeMaintenancePlanById(Long planId) {
        EamMaintenancePlan maintenancePlan = new EamMaintenancePlan();
        maintenancePlan.setPlanId(planId);
        maintenancePlan.setDeletedFlag(1);
        return baseMapper.updateById(maintenancePlan) > 0;
    }

    /**
     * 构建保养计划查询条件。
     * @param equipmentId 设备ID
     * @param maintenanceType 保养类型
     * @param cycleType 周期类型
     * @param responsibleId 负责人ID
     * @param status 状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<EamMaintenancePlan> buildMaintenancePlanQuery(Long equipmentId, String maintenanceType,
                                                                             String cycleType, Long responsibleId,
                                                                             String status) {
        LambdaQueryWrapper<EamMaintenancePlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamMaintenancePlan::getDeletedFlag, 0)
                .eq(equipmentId != null, EamMaintenancePlan::getEquipmentId, equipmentId)
                .eq(StringUtils.hasText(maintenanceType), EamMaintenancePlan::getMaintenanceType, maintenanceType)
                .eq(StringUtils.hasText(cycleType), EamMaintenancePlan::getCycleType, cycleType)
                .eq(responsibleId != null, EamMaintenancePlan::getResponsibleId, responsibleId)
                .eq(StringUtils.hasText(status), EamMaintenancePlan::getStatus, status)
                .orderByDesc(EamMaintenancePlan::getPlanId);
        return queryWrapper;
    }
}
