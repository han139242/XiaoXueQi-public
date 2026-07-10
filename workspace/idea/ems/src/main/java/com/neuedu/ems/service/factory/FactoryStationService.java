package com.neuedu.ems.service.factory;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.factory.FactoryStation;

import java.util.List;

/**
 * 工位 Service 接口。
 */
public interface FactoryStationService extends IService<FactoryStation> {

    Page<FactoryStation> pageStations(Integer current, Integer size, String keyword, Long lineId,
                                      String stationType, String status);

    FactoryStation getStationById(Long stationId);

    boolean saveStation(FactoryStation station);

    boolean updateStation(FactoryStation station);

    boolean removeStationById(Long stationId);

    List<FactoryStation> listStations(String keyword, Long lineId, String stationType, String status);
}
