package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.WmsMaterialBatch;
import com.neuedu.ems.mapper.WmsMaterialBatchMapper;
import com.neuedu.ems.service.WmsMaterialBatchService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 物料批次管理 Service 实现类。
 */
@Service
public class WmsMaterialBatchServiceImpl extends ServiceImpl<WmsMaterialBatchMapper, WmsMaterialBatch> implements WmsMaterialBatchService {

    /**
     * 分页查询物料批次列表，支持批次号、物料、供应商、质量状态和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配批次号或供应商批次号）
     * @param materialId 物料ID
     * @param supplierId 供应商ID
     * @param qualityStatus 质量状态
     * @param status 批次状态
     * @return 分页结果
     */
    @Override
    public Page<WmsMaterialBatch> pageMaterialBatches(Integer current, Integer size, String keyword, Long materialId,
                                                      Long supplierId, String qualityStatus, String status) {
        Page<WmsMaterialBatch> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildMaterialBatchQuery(keyword, materialId, supplierId, qualityStatus, status));
    }

    /**
     * 查询物料批次列表，支持批次号、物料、供应商、质量状态和状态筛选。
     * @param keyword 关键字（匹配批次号或供应商批次号）
     * @param materialId 物料ID
     * @param supplierId 供应商ID
     * @param qualityStatus 质量状态
     * @param status 批次状态
     * @return 物料批次列表
     */
    @Override
    public List<WmsMaterialBatch> listMaterialBatches(String keyword, Long materialId, Long supplierId, String qualityStatus, String status) {
        return baseMapper.selectList(buildMaterialBatchQuery(keyword, materialId, supplierId, qualityStatus, status));
    }

    /**
     * 根据物料批次ID查询未删除的物料批次详情。
     * @param batchId 物料批次ID
     * @return 物料批次实体
     */
    @Override
    public WmsMaterialBatch getMaterialBatchById(Long batchId) {
        LambdaQueryWrapper<WmsMaterialBatch> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsMaterialBatch::getBatchId, batchId)
                .eq(WmsMaterialBatch::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增物料批次，自动设置删除标志为0。
     * @param materialBatch 物料批次实体
     * @return 是否成功
     */
    @Override
    public boolean saveMaterialBatch(WmsMaterialBatch materialBatch) {
        if (materialBatch.getDeletedFlag() == null) {
            materialBatch.setDeletedFlag(0);
        }
        return baseMapper.insert(materialBatch) > 0;
    }

    /**
     * 更新物料批次信息，不允许修改删除标志。
     * @param materialBatch 物料批次实体
     * @return 是否成功
     */
    @Override
    public boolean updateMaterialBatch(WmsMaterialBatch materialBatch) {
        if (materialBatch.getBatchId() == null) {
            return false;
        }
        materialBatch.setDeletedFlag(null);
        return baseMapper.updateById(materialBatch) > 0;
    }

    /**
     * 根据ID删除物料批次（软删除，将删除标志设为1）。
     * @param batchId 物料批次ID
     * @return 是否成功
     */
    @Override
    public boolean removeMaterialBatchById(Long batchId) {
        WmsMaterialBatch materialBatch = new WmsMaterialBatch();
        materialBatch.setBatchId(batchId);
        materialBatch.setDeletedFlag(1);
        return baseMapper.updateById(materialBatch) > 0;
    }

    /**
     * 构建物料批次查询条件，默认过滤已删除记录。
     * @param keyword 关键字（匹配批次号或供应商批次号）
     * @param materialId 物料ID
     * @param supplierId 供应商ID
     * @param qualityStatus 质量状态
     * @param status 批次状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsMaterialBatch> buildMaterialBatchQuery(String keyword, Long materialId, Long supplierId,
                                                                        String qualityStatus, String status) {
        LambdaQueryWrapper<WmsMaterialBatch> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsMaterialBatch::getDeletedFlag, 0)
                .and(StringUtils.hasText(keyword), item -> item.like(WmsMaterialBatch::getBatchNo, keyword)
                        .or().like(WmsMaterialBatch::getSupplierBatchNo, keyword))
                .eq(materialId != null, WmsMaterialBatch::getMaterialId, materialId)
                .eq(supplierId != null, WmsMaterialBatch::getSupplierId, supplierId)
                .eq(StringUtils.hasText(qualityStatus), WmsMaterialBatch::getQualityStatus, qualityStatus)
                .eq(StringUtils.hasText(status), WmsMaterialBatch::getStatus, status)
                .orderByDesc(WmsMaterialBatch::getCreatedTime);
        return queryWrapper;
    }
}
