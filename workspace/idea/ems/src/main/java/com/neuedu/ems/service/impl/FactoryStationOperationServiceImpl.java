package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.FactoryStationOperation;
import com.neuedu.ems.mapper.FactoryStationOperationMapper;
import com.neuedu.ems.service.FactoryStationOperationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工位工序关联 Service 实现类。
 */
@Service
public class FactoryStationOperationServiceImpl
        extends ServiceImpl<FactoryStationOperationMapper, FactoryStationOperation>
        implements FactoryStationOperationService {

    @Override
    public Page<FactoryStationOperation> pageStationOperations(Integer current, Integer size, Long stationId,
                                                               Long operationId, Integer isDefault, String status) {
        Page<FactoryStationOperation> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildStationOperationQuery(stationId, operationId, isDefault, status));
    }

    @Override
    public FactoryStationOperation getStationOperationById(Long id) {
        QueryWrapper<FactoryStationOperation> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveStationOperation(FactoryStationOperation stationOperation) {
        if (stationOperation.getDeletedFlag() == null) {
            stationOperation.setDeletedFlag(0);
        }
        return baseMapper.insert(stationOperation) > 0;
    }

    @Override
    public boolean updateStationOperation(FactoryStationOperation stationOperation) {
        if (stationOperation.getId() == null) {
            return false;
        }
        stationOperation.setDeletedFlag(null);
        return baseMapper.updateById(stationOperation) > 0;
    }

    @Override
    public boolean removeStationOperationById(Long id) {
        FactoryStationOperation stationOperation = new FactoryStationOperation();
        stationOperation.setId(id);
        stationOperation.setDeletedFlag(1);
        return baseMapper.updateById(stationOperation) > 0;
    }

    @Override
    public List<FactoryStationOperation> listStationOperations(Long stationId, Long operationId,
                                                               Integer isDefault, String status) {
        return baseMapper.selectList(buildStationOperationQuery(stationId, operationId, isDefault, status));
    }

    @Override
    public List<FactoryStationOperation> listOperationsByStationId(Long stationId) {
        return baseMapper.selectList(buildStationOperationQuery(stationId, null, null, null));
    }

    private QueryWrapper<FactoryStationOperation> buildStationOperationQuery(Long stationId, Long operationId,
                                                                             Integer isDefault, String status) {
        QueryWrapper<FactoryStationOperation> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (stationId != null) {
            wrapper.eq("station_id", stationId);
        }
        if (operationId != null) {
            wrapper.eq("operation_id", operationId);
        }
        if (isDefault != null) {
            wrapper.eq("is_default", isDefault);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("id");
        return wrapper;
    }
}
