package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.ProcessRouteItem;

import java.util.List;

/**
 * 工艺路线明细 Service 接口。
 */
public interface ProcessRouteItemService extends IService<ProcessRouteItem> {

    Page<ProcessRouteItem> pageRouteItems(Integer current, Integer size, Long routeId, Long operationId);

    ProcessRouteItem getRouteItemById(Long routeItemId);

    boolean saveRouteItem(ProcessRouteItem item);

    boolean updateRouteItem(ProcessRouteItem item);

    boolean removeRouteItemById(Long routeItemId);

    List<ProcessRouteItem> listRouteItems(Long routeId, Long operationId);
}
