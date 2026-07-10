package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.WmsInventory;

import java.util.List;

/**
 * 库存余额管理 Service 接口。
 */
public interface WmsInventoryService extends IService<WmsInventory> {

    /**
     * 分页查询库存余额列表，支持仓库、库位、物料和批次筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param warehouseId 仓库ID
     * @param locationId 库位ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 分页结果
     */
    Page<WmsInventory> pageInventories(Integer current, Integer size, Long warehouseId, Long locationId, Long materialId, Long batchId);

    /**
     * 查询库存余额列表，支持仓库、库位、物料和批次筛选。
     * @param warehouseId 仓库ID
     * @param locationId 库位ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 库存余额列表
     */
    List<WmsInventory> listInventories(Long warehouseId, Long locationId, Long materialId, Long batchId);

    /**
     * 根据库存余额ID查询详情。
     * @param inventoryId 库存余额ID
     * @return 库存余额实体
     */
    WmsInventory getInventoryById(Long inventoryId);
}
