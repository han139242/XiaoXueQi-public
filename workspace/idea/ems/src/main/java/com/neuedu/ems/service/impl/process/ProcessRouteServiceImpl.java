package com.neuedu.ems.service.impl.process;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.process.ProcessRoute;
import com.neuedu.ems.mapper.process.ProcessRouteMapper;
import com.neuedu.ems.service.process.ProcessRouteService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工艺路线 Service 实现类。
 */
@Service
public class ProcessRouteServiceImpl extends ServiceImpl<ProcessRouteMapper, ProcessRoute> implements ProcessRouteService {

    @Override
    public Page<ProcessRoute> pageRoutes(Integer current, Integer size, String keyword, Long productId, String status) {
        Page<ProcessRoute> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildRouteQuery(keyword, productId, status));
    }

    @Override
    public ProcessRoute getRouteById(Long routeId) {
        QueryWrapper<ProcessRoute> wrapper = new QueryWrapper<>();
        wrapper.eq("route_id", routeId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveRoute(ProcessRoute route) {
        if (route.getDeletedFlag() == null) {
            route.setDeletedFlag(0);
        }
        return baseMapper.insert(route) > 0;
    }

    @Override
    public boolean updateRoute(ProcessRoute route) {
        if (route.getRouteId() == null) {
            return false;
        }
        route.setDeletedFlag(null);
        return baseMapper.updateById(route) > 0;
    }

    @Override
    public boolean removeRouteById(Long routeId) {
        ProcessRoute route = new ProcessRoute();
        route.setRouteId(routeId);
        route.setDeletedFlag(1);
        return baseMapper.updateById(route) > 0;
    }

    @Override
    public List<ProcessRoute> listRoutes(String keyword, Long productId, String status) {
        return baseMapper.selectList(buildRouteQuery(keyword, productId, status));
    }

    private QueryWrapper<ProcessRoute> buildRouteQuery(String keyword, Long productId, String status) {
        QueryWrapper<ProcessRoute> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("route_code", keyword).or().like("route_name", keyword));
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("route_id");
        return wrapper;
    }
}
