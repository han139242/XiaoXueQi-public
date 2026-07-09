package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.WmsWarehouse;
import com.neuedu.ems.mapper.WmsWarehouseMapper;
import com.neuedu.ems.service.WmsWarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 仓库管理 Service 实现类。
 */
@Service
public class WmsWarehouseServiceImpl extends ServiceImpl<WmsWarehouseMapper, WmsWarehouse> implements WmsWarehouseService {

    @Override
    public Page<WmsWarehouse> pageWarehouses(Integer current, Integer size, String keyword, String status) {
        Page<WmsWarehouse> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildWarehouseQuery(keyword, status));
    }

    @Override
    public WmsWarehouse getWarehouseById(Long warehouseId) {
        QueryWrapper<WmsWarehouse> wrapper = new QueryWrapper<>();
        wrapper.eq("warehouse_id", warehouseId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveWarehouse(WmsWarehouse warehouse) {
        if (warehouse.getDeletedFlag() == null) {
            warehouse.setDeletedFlag(0);
        }
        return baseMapper.insert(warehouse) > 0;
    }

    @Override
    public boolean updateWarehouse(WmsWarehouse warehouse) {
        if (warehouse.getWarehouseId() == null) {
            return false;
        }
        warehouse.setDeletedFlag(null);
        return baseMapper.updateById(warehouse) > 0;
    }

    @Override
    public boolean removeWarehouseById(Long warehouseId) {
        WmsWarehouse warehouse = new WmsWarehouse();
        warehouse.setWarehouseId(warehouseId);
        warehouse.setDeletedFlag(1);
        return baseMapper.updateById(warehouse) > 0;
    }

    @Override
    public List<WmsWarehouse> listWarehouses(String keyword, String status) {
        return baseMapper.selectList(buildWarehouseQuery(keyword, status));
    }

    private QueryWrapper<WmsWarehouse> buildWarehouseQuery(String keyword, String status) {
        QueryWrapper<WmsWarehouse> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("warehouse_code", keyword)
                    .or().like("warehouse_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("warehouse_id");
        return wrapper;
    }
}
