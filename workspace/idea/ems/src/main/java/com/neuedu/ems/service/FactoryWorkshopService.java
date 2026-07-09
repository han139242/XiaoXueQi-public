package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.FactoryWorkshop;

import java.util.List;

/**
 * 车间 Service 接口。
 */
public interface FactoryWorkshopService extends IService<FactoryWorkshop> {

    Page<FactoryWorkshop> pageWorkshops(Integer current, Integer size, String keyword, String status);

    FactoryWorkshop getWorkshopById(Long workshopId);

    boolean saveWorkshop(FactoryWorkshop workshop);

    boolean updateWorkshop(FactoryWorkshop workshop);

    boolean removeWorkshopById(Long workshopId);

    List<FactoryWorkshop> listWorkshops(String keyword, String status);
}
