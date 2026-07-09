package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.FactoryStation;
import com.neuedu.ems.mapper.FactoryStationMapper;
import com.neuedu.ems.service.FactoryStationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工位 Service 实现类。
 */
@Service
public class FactoryStationServiceImpl
        extends ServiceImpl<FactoryStationMapper, FactoryStation>
        implements FactoryStationService {

    @Override
    public Page<FactoryStation> pageStations(Integer current, Integer size, String keyword, Long lineId,
                                             String stationType, String status) {
        Page<FactoryStation> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildStationQuery(keyword, lineId, stationType, status));
    }

    @Override
    public FactoryStation getStationById(Long stationId) {
        QueryWrapper<FactoryStation> wrapper = new QueryWrapper<>();
        wrapper.eq("station_id", stationId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveStation(FactoryStation station) {
        if (station.getDeletedFlag() == null) {
            station.setDeletedFlag(0);
        }
        return baseMapper.insert(station) > 0;
    }

    @Override
    public boolean updateStation(FactoryStation station) {
        if (station.getStationId() == null) {
            return false;
        }
        station.setDeletedFlag(null);
        return baseMapper.updateById(station) > 0;
    }

    @Override
    public boolean removeStationById(Long stationId) {
        FactoryStation station = new FactoryStation();
        station.setStationId(stationId);
        station.setDeletedFlag(1);
        return baseMapper.updateById(station) > 0;
    }

    @Override
    public List<FactoryStation> listStations(String keyword, Long lineId, String stationType, String status) {
        return baseMapper.selectList(buildStationQuery(keyword, lineId, stationType, status));
    }

    private QueryWrapper<FactoryStation> buildStationQuery(String keyword, Long lineId,
                                                           String stationType, String status) {
        QueryWrapper<FactoryStation> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("station_code", keyword).or().like("station_name", keyword));
        }
        if (lineId != null) {
            wrapper.eq("line_id", lineId);
        }
        if (StringUtils.hasText(stationType)) {
            wrapper.eq("station_type", stationType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("station_id");
        return wrapper;
    }
}
