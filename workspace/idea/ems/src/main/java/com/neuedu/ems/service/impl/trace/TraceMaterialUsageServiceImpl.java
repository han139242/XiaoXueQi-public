package com.neuedu.ems.service.impl.trace;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.trace.TraceMaterialUsage;
import com.neuedu.ems.mapper.trace.TraceMaterialUsageMapper;
import com.neuedu.ems.service.trace.TraceMaterialUsageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 物料使用追溯 Service 实现类。
 */
@Service
public class TraceMaterialUsageServiceImpl extends ServiceImpl<TraceMaterialUsageMapper, TraceMaterialUsage>
        implements TraceMaterialUsageService {

    /**
     * 分页查询物料使用追溯，默认过滤已删除记录。
     * @param current 当前页码
     * @param size 每页记录数
     * @param productLotId 产品批次ID
     * @param barcodeId 条码ID
     * @param workOrderId 工单ID
     * @param reportId 报工ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 分页结果
     */
    @Override
    public Page<TraceMaterialUsage> pageMaterialUsages(Integer current, Integer size, Long productLotId, Long barcodeId,
                                                       Long workOrderId, Long reportId, Long materialId, Long batchId) {
        Page<TraceMaterialUsage> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildMaterialUsageQuery(productLotId, barcodeId, workOrderId, reportId, materialId, batchId));
    }

    /**
     * 查询物料使用追溯列表，默认过滤已删除记录。
     * @param productLotId 产品批次ID
     * @param barcodeId 条码ID
     * @param workOrderId 工单ID
     * @param reportId 报工ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 物料使用追溯列表
     */
    @Override
    public List<TraceMaterialUsage> listMaterialUsages(Long productLotId, Long barcodeId, Long workOrderId,
                                                       Long reportId, Long materialId, Long batchId) {
        return baseMapper.selectList(buildMaterialUsageQuery(productLotId, barcodeId, workOrderId, reportId, materialId, batchId));
    }

    /**
     * 根据物料使用追溯ID查询未删除的记录详情。
     * @param usageId 追溯ID
     * @return 追溯实体
     */
    @Override
    public TraceMaterialUsage getMaterialUsageById(Long usageId) {
        LambdaQueryWrapper<TraceMaterialUsage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TraceMaterialUsage::getUsageId, usageId)
                .eq(TraceMaterialUsage::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增物料使用追溯记录，自动设置删除标志为0。
     * @param materialUsage 追溯记录实体
     * @return 是否成功
     */
    @Override
    public boolean saveMaterialUsage(TraceMaterialUsage materialUsage) {
        if (materialUsage.getCreatedTime() == null) {
            materialUsage.setCreatedTime(java.time.LocalDateTime.now());
        }
        materialUsage.setUpdatedTime(materialUsage.getCreatedTime());
        if (materialUsage.getDeletedFlag() == null) {
            materialUsage.setDeletedFlag(0);
        }
        return baseMapper.insert(materialUsage) > 0;
    }

    /**
     * 更新物料使用追溯记录，不允许通过普通更新修改删除标志。
     * @param materialUsage 追溯记录实体
     * @return 是否成功
     */
    @Override
    public boolean updateMaterialUsage(TraceMaterialUsage materialUsage) {
        if (materialUsage.getUsageId() == null) {
            return false;
        }
        materialUsage.setUpdatedTime(java.time.LocalDateTime.now());
        materialUsage.setDeletedFlag(null);
        return baseMapper.updateById(materialUsage) > 0;
    }

    /**
     * 根据ID软删除物料使用追溯记录。
     * @param usageId 追溯ID
     * @return 是否成功
     */
    @Override
    public boolean removeMaterialUsageById(Long usageId) {
        TraceMaterialUsage materialUsage = new TraceMaterialUsage();
        materialUsage.setUsageId(usageId);
        materialUsage.setDeletedFlag(1);
        return baseMapper.updateById(materialUsage) > 0;
    }

    /**
     * 按原料批次查询物料使用追溯记录。
     * @param batchId 原料批次ID
     * @return 物料使用追溯列表
     */
    @Override
    public List<TraceMaterialUsage> listByBatchId(Long batchId) {
        LambdaQueryWrapper<TraceMaterialUsage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TraceMaterialUsage::getBatchId, batchId)
                .eq(TraceMaterialUsage::getDeletedFlag, 0)
                .orderByDesc(TraceMaterialUsage::getUsageId);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 构建物料使用追溯查询条件。
     * @param productLotId 产品批次ID
     * @param barcodeId 条码ID
     * @param workOrderId 工单ID
     * @param reportId 报工ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<TraceMaterialUsage> buildMaterialUsageQuery(Long productLotId, Long barcodeId,
                                                                           Long workOrderId, Long reportId,
                                                                           Long materialId, Long batchId) {
        LambdaQueryWrapper<TraceMaterialUsage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TraceMaterialUsage::getDeletedFlag, 0)
                .eq(productLotId != null, TraceMaterialUsage::getProductLotId, productLotId)
                .eq(barcodeId != null, TraceMaterialUsage::getBarcodeId, barcodeId)
                .eq(workOrderId != null, TraceMaterialUsage::getWorkOrderId, workOrderId)
                .eq(reportId != null, TraceMaterialUsage::getReportId, reportId)
                .eq(materialId != null, TraceMaterialUsage::getMaterialId, materialId)
                .eq(batchId != null, TraceMaterialUsage::getBatchId, batchId)
                .orderByDesc(TraceMaterialUsage::getUsageId);
        return queryWrapper;
    }
}
