package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.MfgWorkOrderMaterial;
import com.neuedu.ems.mapper.MfgWorkOrderMaterialMapper;
import com.neuedu.ems.service.MfgWorkOrderMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工单物料需求业务逻辑实现类。
 * 实现工单物料需求的分页查询、条件查询、新增、更新和删除功能。
 */
@Service
public class MfgWorkOrderMaterialServiceImpl extends ServiceImpl<MfgWorkOrderMaterialMapper, MfgWorkOrderMaterial> implements MfgWorkOrderMaterialService {

    /**
     * 分页查询工单物料需求，支持工单ID、物料ID和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param workOrderId 工单ID，可选
     * @param materialId 物料ID，可选
     * @param status 物料齐套状态，可选
     * @return 分页结果
     */
    @Override
    public Page<MfgWorkOrderMaterial> pageWorkOrderMaterials(Integer current, Integer size, Long workOrderId, Long materialId, String status) {
        Page<MfgWorkOrderMaterial> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildWorkOrderMaterialQuery(workOrderId, materialId, status));
    }

    /**
     * 根据工单ID查询未删除的工单物料需求列表。
     * @param workOrderId 工单ID
     * @return 工单物料需求列表
     */
    @Override
    public List<MfgWorkOrderMaterial> listWorkOrderMaterialsByWorkOrderId(Long workOrderId) {
        return baseMapper.selectList(buildWorkOrderMaterialQuery(workOrderId, null, null));
    }

    /**
     * 根据条件查询工单物料需求列表，支持工单ID、物料ID和状态筛选。
     * @param workOrderId 工单ID，可选
     * @param materialId 物料ID，可选
     * @param status 物料齐套状态，可选
     * @return 工单物料需求列表
     */
    @Override
    public List<MfgWorkOrderMaterial> listWorkOrderMaterials(Long workOrderId, Long materialId, String status) {
        return baseMapper.selectList(buildWorkOrderMaterialQuery(workOrderId, materialId, status));
    }

    /**
     * 根据需求ID查询工单物料需求详情。
     * @param requirementId 需求ID
     * @return 工单物料需求实体
     */
    @Override
    public MfgWorkOrderMaterial getWorkOrderMaterialById(Long requirementId) {
        LambdaQueryWrapper<MfgWorkOrderMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgWorkOrderMaterial::getRequirementId, requirementId)
                .eq(MfgWorkOrderMaterial::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增工单物料需求。
     * @param workOrderMaterial 工单物料需求实体
     * @return 操作是否成功
     */
    @Override
    public boolean saveWorkOrderMaterial(MfgWorkOrderMaterial workOrderMaterial) {
        if (workOrderMaterial.getDeletedFlag() == null) {
            workOrderMaterial.setDeletedFlag(0);
        }
        return baseMapper.insert(workOrderMaterial) > 0;
    }

    /**
     * 更新工单物料需求信息。
     * @param workOrderMaterial 工单物料需求实体（必须包含requirementId）
     * @return 操作是否成功
     */
    @Override
    public boolean updateWorkOrderMaterial(MfgWorkOrderMaterial workOrderMaterial) {
        if (workOrderMaterial.getRequirementId() == null) {
            return false;
        }
        workOrderMaterial.setDeletedFlag(null);
        return baseMapper.updateById(workOrderMaterial) > 0;
    }

    /**
     * 删除工单物料需求（软删除，将deletedFlag设为1）。
     * @param requirementId 需求ID
     * @return 操作是否成功
     */
    @Override
    public boolean removeWorkOrderMaterialById(Long requirementId) {
        MfgWorkOrderMaterial workOrderMaterial = new MfgWorkOrderMaterial();
        workOrderMaterial.setRequirementId(requirementId);
        workOrderMaterial.setDeletedFlag(1);
        return baseMapper.updateById(workOrderMaterial) > 0;
    }

    /**
     * 构建工单物料需求查询条件，默认过滤已删除记录。
     * @param workOrderId 工单ID，可选
     * @param materialId 物料ID，可选
     * @param status 物料齐套状态，可选
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<MfgWorkOrderMaterial> buildWorkOrderMaterialQuery(Long workOrderId, Long materialId, String status) {
        LambdaQueryWrapper<MfgWorkOrderMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgWorkOrderMaterial::getDeletedFlag, 0)
                .eq(workOrderId != null, MfgWorkOrderMaterial::getWorkOrderId, workOrderId)
                .eq(materialId != null, MfgWorkOrderMaterial::getMaterialId, materialId)
                .eq(StringUtils.hasText(status), MfgWorkOrderMaterial::getStatus, status)
                .orderByDesc(MfgWorkOrderMaterial::getCreatedTime);
        return queryWrapper;
    }
}
