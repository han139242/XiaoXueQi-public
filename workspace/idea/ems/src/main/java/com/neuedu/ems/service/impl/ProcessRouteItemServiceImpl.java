package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.ProcessRouteItem;
import com.neuedu.ems.mapper.ProcessRouteItemMapper;
import com.neuedu.ems.service.ProcessRouteItemService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工艺路线明细 Service 实现类。
 */
@Service
public class ProcessRouteItemServiceImpl
        extends ServiceImpl<ProcessRouteItemMapper, ProcessRouteItem>
        implements ProcessRouteItemService {

    @Override
    public Page<ProcessRouteItem> pageRouteItems(Integer current, Integer size, Long routeId, Long operationId) {
        Page<ProcessRouteItem> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildRouteItemQuery(routeId, operationId));
    }

    @Override
    public ProcessRouteItem getRouteItemById(Long routeItemId) {
        QueryWrapper<ProcessRouteItem> wrapper = new QueryWrapper<>();
        wrapper.eq("route_item_id", routeItemId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveRouteItem(ProcessRouteItem item) {
        if (item.getDeletedFlag() == null) {
            item.setDeletedFlag(0);
        }
        return baseMapper.insert(item) > 0;
    }

    @Override
    public boolean updateRouteItem(ProcessRouteItem item) {
        if (item.getRouteItemId() == null) {
            return false;
        }
        item.setDeletedFlag(null);
        return baseMapper.updateById(item) > 0;
    }

    @Override
    public boolean removeRouteItemById(Long routeItemId) {
        ProcessRouteItem item = new ProcessRouteItem();
        item.setRouteItemId(routeItemId);
        item.setDeletedFlag(1);
        return baseMapper.updateById(item) > 0;
    }

    @Override
    public List<ProcessRouteItem> listRouteItems(Long routeId, Long operationId) {
        return baseMapper.selectList(buildRouteItemQuery(routeId, operationId));
    }

    private QueryWrapper<ProcessRouteItem> buildRouteItemQuery(Long routeId, Long operationId) {
        QueryWrapper<ProcessRouteItem> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (routeId != null) {
            wrapper.eq("route_id", routeId);
        }
        if (operationId != null) {
            wrapper.eq("operation_id", operationId);
        }
        wrapper.orderByAsc("sequence_no", "route_item_id");
        return wrapper;
    }
}
