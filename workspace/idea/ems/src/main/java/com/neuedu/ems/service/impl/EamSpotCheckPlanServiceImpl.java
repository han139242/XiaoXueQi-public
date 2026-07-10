package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.EamSpotCheckPlan;
import com.neuedu.ems.mapper.EamSpotCheckPlanMapper;
import com.neuedu.ems.service.EamSpotCheckPlanService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 点检计划 Service 实现类。
 */
@Service
public class EamSpotCheckPlanServiceImpl extends ServiceImpl<EamSpotCheckPlanMapper, EamSpotCheckPlan>
        implements EamSpotCheckPlanService {

    /**
     * 分页查询点检计划，默认过滤已删除记录。
     * @param current 当前页码
     * @param size 每页记录数
     * @param equipmentId 设备ID
     * @param checkCycle 点检周期
     * @param responsibleRole 负责岗位
     * @param status 状态
     * @return 分页结果
     */
    @Override
    public Page<EamSpotCheckPlan> pageSpotCheckPlans(Integer current, Integer size, Long equipmentId,
                                                     String checkCycle, String responsibleRole, String status) {
        Page<EamSpotCheckPlan> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildSpotCheckPlanQuery(equipmentId, checkCycle, responsibleRole, status));
    }

    /**
     * 查询点检计划列表，默认过滤已删除记录。
     * @param equipmentId 设备ID
     * @param checkCycle 点检周期
     * @param responsibleRole 负责岗位
     * @param status 状态
     * @return 点检计划列表
     */
    @Override
    public List<EamSpotCheckPlan> listSpotCheckPlans(Long equipmentId, String checkCycle,
                                                     String responsibleRole, String status) {
        return baseMapper.selectList(buildSpotCheckPlanQuery(equipmentId, checkCycle, responsibleRole, status));
    }

    /**
     * 根据点检计划ID查询未删除的计划详情。
     * @param planId 点检计划ID
     * @return 点检计划实体
     */
    @Override
    public EamSpotCheckPlan getSpotCheckPlanById(Long planId) {
        LambdaQueryWrapper<EamSpotCheckPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamSpotCheckPlan::getPlanId, planId)
                .eq(EamSpotCheckPlan::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增点检计划，自动设置删除标志为0。
     * @param spotCheckPlan 点检计划实体
     * @return 是否成功
     */
    @Override
    public boolean saveSpotCheckPlan(EamSpotCheckPlan spotCheckPlan) {
        if (spotCheckPlan.getDeletedFlag() == null) {
            spotCheckPlan.setDeletedFlag(0);
        }
        return baseMapper.insert(spotCheckPlan) > 0;
    }

    /**
     * 更新点检计划，不允许通过普通更新修改删除标志。
     * @param spotCheckPlan 点检计划实体
     * @return 是否成功
     */
    @Override
    public boolean updateSpotCheckPlan(EamSpotCheckPlan spotCheckPlan) {
        if (spotCheckPlan.getPlanId() == null) {
            return false;
        }
        spotCheckPlan.setDeletedFlag(null);
        return baseMapper.updateById(spotCheckPlan) > 0;
    }

    /**
     * 根据ID软删除点检计划。
     * @param planId 点检计划ID
     * @return 是否成功
     */
    @Override
    public boolean removeSpotCheckPlanById(Long planId) {
        EamSpotCheckPlan spotCheckPlan = new EamSpotCheckPlan();
        spotCheckPlan.setPlanId(planId);
        spotCheckPlan.setDeletedFlag(1);
        return baseMapper.updateById(spotCheckPlan) > 0;
    }

    /**
     * 构建点检计划查询条件。
     * @param equipmentId 设备ID
     * @param checkCycle 点检周期
     * @param responsibleRole 负责岗位
     * @param status 状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<EamSpotCheckPlan> buildSpotCheckPlanQuery(Long equipmentId, String checkCycle,
                                                                         String responsibleRole, String status) {
        LambdaQueryWrapper<EamSpotCheckPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamSpotCheckPlan::getDeletedFlag, 0)
                .eq(equipmentId != null, EamSpotCheckPlan::getEquipmentId, equipmentId)
                .eq(StringUtils.hasText(checkCycle), EamSpotCheckPlan::getCheckCycle, checkCycle)
                .eq(StringUtils.hasText(responsibleRole), EamSpotCheckPlan::getResponsibleRole, responsibleRole)
                .eq(StringUtils.hasText(status), EamSpotCheckPlan::getStatus, status)
                .orderByDesc(EamSpotCheckPlan::getPlanId);
        return queryWrapper;
    }
}
