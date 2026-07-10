package com.neuedu.ems.service.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.wms.WmsStockMovement;

import java.util.List;

/**
 * 库存流水管理 Service 接口。
 */
public interface WmsStockMovementService extends IService<WmsStockMovement> {

    /**
     * 分页查询库存流水列表，支持流水号、变动类型、来源单据、物料、批次和方向筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配流水号）
     * @param movementType 变动类型
     * @param sourceOrderType 来源单据类型
     * @param sourceOrderId 来源单据ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param direction 方向
     * @return 分页结果
     */
    Page<WmsStockMovement> pageStockMovements(Integer current, Integer size, String keyword, String movementType,
                                              String sourceOrderType, Long sourceOrderId, Long materialId, Long batchId, String direction);

    /**
     * 查询库存流水列表，支持流水号、变动类型、来源单据、物料、批次和方向筛选。
     * @param keyword 关键字（匹配流水号）
     * @param movementType 变动类型
     * @param sourceOrderType 来源单据类型
     * @param sourceOrderId 来源单据ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param direction 方向
     * @return 库存流水列表
     */
    List<WmsStockMovement> listStockMovements(String keyword, String movementType, String sourceOrderType,
                                              Long sourceOrderId, Long materialId, Long batchId, String direction);

    /**
     * 根据库存流水ID查询详情。
     * @param movementId 库存流水ID
     * @return 库存流水实体
     */
    WmsStockMovement getStockMovementById(Long movementId);
}
