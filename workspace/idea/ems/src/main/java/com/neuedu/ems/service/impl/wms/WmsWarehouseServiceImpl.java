package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.wms.WmsWarehouse;
import com.neuedu.ems.mapper.wms.WmsWarehouseMapper;
import com.neuedu.ems.service.wms.WmsWarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 仓库管理 Service 实现类。
 */
@Service
public class WmsWarehouseServiceImpl extends ServiceImpl<WmsWarehouseMapper, WmsWarehouse> implements WmsWarehouseService {

    /**
     * 分页查询仓库列表，支持关键字搜索和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配仓库编码或仓库名称）
     * @param status 仓库状态
     * @return 分页结果
     */
    @Override
    public Page<WmsWarehouse> pageWarehouses(Integer current, Integer size, String keyword, String status) {
        Page<WmsWarehouse> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildWarehouseQuery(keyword, status));
    }

    /**
     * 根据仓库ID查询仓库信息。
     * @param warehouseId 仓库ID
     * @return 仓库实体
     */
    @Override
    public WmsWarehouse getWarehouseById(Long warehouseId) {
        QueryWrapper<WmsWarehouse> wrapper = new QueryWrapper<>();
        wrapper.eq("warehouse_id", warehouseId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增仓库，自动设置删除标志为0。
     * @param warehouse 仓库实体
     * @return 是否成功
     */
    @Override
    public boolean saveWarehouse(WmsWarehouse warehouse) {
        if (warehouse.getDeletedFlag() == null) {
            warehouse.setDeletedFlag(0);
        }
        return baseMapper.insert(warehouse) > 0;
    }

    /**
     * 更新仓库信息，不允许修改删除标志。
     * @param warehouse 仓库实体
     * @return 是否成功
     */
    @Override
    public boolean updateWarehouse(WmsWarehouse warehouse) {
        if (warehouse.getWarehouseId() == null) {
            return false;
        }
        warehouse.setDeletedFlag(null);
        return baseMapper.updateById(warehouse) > 0;
    }

    /**
     * 根据ID删除仓库（软删除，将删除标志设为1）。
     * @param warehouseId 仓库ID
     * @return 是否成功
     */
    @Override
    public boolean removeWarehouseById(Long warehouseId) {
        WmsWarehouse warehouse = new WmsWarehouse();
        warehouse.setWarehouseId(warehouseId);
        warehouse.setDeletedFlag(1);
        return baseMapper.updateById(warehouse) > 0;
    }

    /**
     * 查询仓库列表，支持关键字搜索和状态筛选。
     * @param keyword 关键字（匹配仓库编码或仓库名称）
     * @param status 仓库状态
     * @return 仓库列表
     */
    @Override
    public List<WmsWarehouse> listWarehouses(String keyword, String status) {
        return baseMapper.selectList(buildWarehouseQuery(keyword, status));
    }

    /**
     * 构建仓库查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @return 查询条件包装器
     */
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
