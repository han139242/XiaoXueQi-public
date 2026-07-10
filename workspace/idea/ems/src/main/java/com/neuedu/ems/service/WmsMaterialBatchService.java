package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.WmsMaterialBatch;

import java.util.List;

/**
 * 物料批次管理 Service 接口。
 */
public interface WmsMaterialBatchService extends IService<WmsMaterialBatch> {

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
    Page<WmsMaterialBatch> pageMaterialBatches(Integer current, Integer size, String keyword, Long materialId,
                                               Long supplierId, String qualityStatus, String status);

    /**
     * 查询物料批次列表，支持批次号、物料、供应商、质量状态和状态筛选。
     * @param keyword 关键字（匹配批次号或供应商批次号）
     * @param materialId 物料ID
     * @param supplierId 供应商ID
     * @param qualityStatus 质量状态
     * @param status 批次状态
     * @return 物料批次列表
     */
    List<WmsMaterialBatch> listMaterialBatches(String keyword, Long materialId, Long supplierId, String qualityStatus, String status);

    /**
     * 根据物料批次ID查询详情。
     * @param batchId 物料批次ID
     * @return 物料批次实体
     */
    WmsMaterialBatch getMaterialBatchById(Long batchId);

    /**
     * 新增物料批次。
     * @param materialBatch 物料批次实体
     * @return 是否成功
     */
    boolean saveMaterialBatch(WmsMaterialBatch materialBatch);

    /**
     * 更新物料批次信息。
     * @param materialBatch 物料批次实体
     * @return 是否成功
     */
    boolean updateMaterialBatch(WmsMaterialBatch materialBatch);

    /**
     * 根据ID删除物料批次（软删除）。
     * @param batchId 物料批次ID
     * @return 是否成功
     */
    boolean removeMaterialBatchById(Long batchId);
}
