package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.WmsWarehouse;

import java.util.List;

/**
 * 仓库管理 Service 接口。
 */
public interface WmsWarehouseService extends IService<WmsWarehouse> {

    Page<WmsWarehouse> pageWarehouses(Integer current, Integer size, String keyword, String status);

    WmsWarehouse getWarehouseById(Long warehouseId);

    boolean saveWarehouse(WmsWarehouse warehouse);

    boolean updateWarehouse(WmsWarehouse warehouse);

    boolean removeWarehouseById(Long warehouseId);

    List<WmsWarehouse> listWarehouses(String keyword, String status);
}
