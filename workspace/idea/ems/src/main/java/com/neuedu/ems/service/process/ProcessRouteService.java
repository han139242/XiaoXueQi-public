package com.neuedu.ems.service.process;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.process.ProcessRoute;

import java.util.List;

/**
 * 工艺路线 Service 接口。
 */
public interface ProcessRouteService extends IService<ProcessRoute> {

    Page<ProcessRoute> pageRoutes(Integer current, Integer size, String keyword, Long productId, String status);

    ProcessRoute getRouteById(Long routeId);

    boolean saveRoute(ProcessRoute route);

    boolean updateRoute(ProcessRoute route);

    boolean removeRouteById(Long routeId);

    List<ProcessRoute> listRoutes(String keyword, Long productId, String status);
}
