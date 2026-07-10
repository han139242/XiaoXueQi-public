package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.WmsInventory;
import com.neuedu.ems.mapper.WmsInventoryMapper;
import com.neuedu.ems.service.WmsInventoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 库存余额管理 Service 实现类。
 */
@Service
public class WmsInventoryServiceImpl extends ServiceImpl<WmsInventoryMapper, WmsInventory> implements WmsInventoryService {

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
    @Override
    public Page<WmsInventory> pageInventories(Integer current, Integer size, Long warehouseId, Long locationId, Long materialId, Long batchId) {
        Page<WmsInventory> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildInventoryQuery(warehouseId, locationId, materialId, batchId));
    }

    /**
     * 查询库存余额列表，支持仓库、库位、物料和批次筛选。
     * @param warehouseId 仓库ID
     * @param locationId 库位ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 库存余额列表
     */
    @Override
    public List<WmsInventory> listInventories(Long warehouseId, Long locationId, Long materialId, Long batchId) {
        return baseMapper.selectList(buildInventoryQuery(warehouseId, locationId, materialId, batchId));
    }

    /**
     * 根据库存余额ID查询未删除的库存余额详情。
     * @param inventoryId 库存余额ID
     * @return 库存余额实体
     */
    @Override
    public WmsInventory getInventoryById(Long inventoryId) {
        LambdaQueryWrapper<WmsInventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInventory::getInventoryId, inventoryId)
                .eq(WmsInventory::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 构建库存余额查询条件，默认过滤已删除记录。
     * @param warehouseId 仓库ID
     * @param locationId 库位ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsInventory> buildInventoryQuery(Long warehouseId, Long locationId, Long materialId, Long batchId) {
        LambdaQueryWrapper<WmsInventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInventory::getDeletedFlag, 0)
                .eq(warehouseId != null, WmsInventory::getWarehouseId, warehouseId)
                .eq(locationId != null, WmsInventory::getLocationId, locationId)
                .eq(materialId != null, WmsInventory::getMaterialId, materialId)
                .eq(batchId != null, WmsInventory::getBatchId, batchId)
                .orderByDesc(WmsInventory::getCreatedTime);
        return queryWrapper;
    }
}
