package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.WmsWarehouse;

import java.util.List;

/**
 * 仓库管理 Service 接口。
 */
public interface WmsWarehouseService extends IService<WmsWarehouse> {

    /**
     * 分页查询仓库列表，支持关键字搜索和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配仓库编码或仓库名称）
     * @param status 仓库状态
     * @return 分页结果
     */
    Page<WmsWarehouse> pageWarehouses(Integer current, Integer size, String keyword, String status);

    /**
     * 根据仓库ID查询仓库信息。
     * @param warehouseId 仓库ID
     * @return 仓库实体
     */
    WmsWarehouse getWarehouseById(Long warehouseId);

    /**
     * 新增仓库。
     * @param warehouse 仓库实体
     * @return 是否成功
     */
    boolean saveWarehouse(WmsWarehouse warehouse);

    /**
     * 更新仓库信息。
     * @param warehouse 仓库实体
     * @return 是否成功
     */
    boolean updateWarehouse(WmsWarehouse warehouse);

    /**
     * 根据ID删除仓库（软删除）。
     * @param warehouseId 仓库ID
     * @return 是否成功
     */
    boolean removeWarehouseById(Long warehouseId);

    /**
     * 查询仓库列表，支持关键字搜索和状态筛选。
     * @param keyword 关键字（匹配仓库编码或仓库名称）
     * @param status 仓库状态
     * @return 仓库列表
     */
    List<WmsWarehouse> listWarehouses(String keyword, String status);
}
