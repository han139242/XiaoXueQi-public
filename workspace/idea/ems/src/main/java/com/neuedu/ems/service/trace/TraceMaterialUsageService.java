package com.neuedu.ems.service.trace;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.trace.TraceMaterialUsage;

import java.util.List;

/**
 * 物料使用追溯 Service 接口。
 */
public interface TraceMaterialUsageService extends IService<TraceMaterialUsage> {

    /**
     * 分页查询物料使用追溯，支持产品批次、条码、工单、报工、物料和批次筛选。
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
    Page<TraceMaterialUsage> pageMaterialUsages(Integer current, Integer size, Long productLotId, Long barcodeId,
                                                Long workOrderId, Long reportId, Long materialId, Long batchId);

    /**
     * 查询物料使用追溯列表，支持产品批次、条码、工单、报工、物料和批次筛选。
     * @param productLotId 产品批次ID
     * @param barcodeId 条码ID
     * @param workOrderId 工单ID
     * @param reportId 报工ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 物料使用追溯列表
     */
    List<TraceMaterialUsage> listMaterialUsages(Long productLotId, Long barcodeId, Long workOrderId,
                                                Long reportId, Long materialId, Long batchId);

    /**
     * 根据物料使用追溯ID查询未删除的记录详情。
     * @param usageId 追溯ID
     * @return 追溯实体
     */
    TraceMaterialUsage getMaterialUsageById(Long usageId);

    /**
     * 新增物料使用追溯记录。
     * @param materialUsage 追溯记录实体
     * @return 是否成功
     */
    boolean saveMaterialUsage(TraceMaterialUsage materialUsage);

    /**
     * 更新物料使用追溯记录。
     * @param materialUsage 追溯记录实体，必须包含usageId
     * @return 是否成功
     */
    boolean updateMaterialUsage(TraceMaterialUsage materialUsage);

    /**
     * 根据ID软删除物料使用追溯记录。
     * @param usageId 追溯ID
     * @return 是否成功
     */
    boolean removeMaterialUsageById(Long usageId);

    /**
     * 按原料批次查询物料使用追溯记录。
     * @param batchId 原料批次ID
     * @return 物料使用追溯列表
     */
    List<TraceMaterialUsage> listByBatchId(Long batchId);
}
