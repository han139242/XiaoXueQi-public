package com.neuedu.ems.service.factory;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.factory.FactoryStationOperation;

import java.util.List;

/**
 * 工位工序关联 Service 接口。
 */
public interface FactoryStationOperationService extends IService<FactoryStationOperation> {

    Page<FactoryStationOperation> pageStationOperations(Integer current, Integer size, Long stationId,
                                                        Long operationId, Integer isDefault, String status);

    FactoryStationOperation getStationOperationById(Long id);

    boolean saveStationOperation(FactoryStationOperation stationOperation);

    boolean updateStationOperation(FactoryStationOperation stationOperation);

    boolean removeStationOperationById(Long id);

    List<FactoryStationOperation> listStationOperations(Long stationId, Long operationId,
                                                        Integer isDefault, String status);

    List<FactoryStationOperation> listOperationsByStationId(Long stationId);
}
