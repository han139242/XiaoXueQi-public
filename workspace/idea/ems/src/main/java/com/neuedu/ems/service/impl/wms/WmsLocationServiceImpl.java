package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.wms.WmsLocation;
import com.neuedu.ems.mapper.wms.WmsLocationMapper;
import com.neuedu.ems.service.wms.WmsLocationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 库位管理 Service 实现类。
 */
@Service
public class WmsLocationServiceImpl extends ServiceImpl<WmsLocationMapper, WmsLocation> implements WmsLocationService {

    /**
     * 分页查询库位列表，支持关键字搜索和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配库位编码或库位名称）
     * @param status 库位状态
     * @return 分页结果
     */
    @Override
    public Page<WmsLocation> pageLocations(Integer current, Integer size, String keyword, String status) {
        Page<WmsLocation> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildLocationQuery(keyword, status));
    }

    /**
     * 根据库位ID查询库位信息。
     * @param locationId 库位ID
     * @return 库位实体
     */
    @Override
    public WmsLocation getLocationById(Long locationId) {
        QueryWrapper<WmsLocation> wrapper = new QueryWrapper<>();
        wrapper.eq("location_id", locationId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增库位，自动设置删除标志为0。
     * @param location 库位实体
     * @return 是否成功
     */
    @Override
    public boolean saveLocation(WmsLocation location) {
        if (location.getDeletedFlag() == null) {
            location.setDeletedFlag(0);
        }
        return baseMapper.insert(location) > 0;
    }

    /**
     * 更新库位信息，不允许修改删除标志。
     * @param location 库位实体
     * @return 是否成功
     */
    @Override
    public boolean updateLocation(WmsLocation location) {
        if (location.getLocationId() == null) {
            return false;
        }
        location.setDeletedFlag(null);
        return baseMapper.updateById(location) > 0;
    }

    /**
     * 根据ID删除库位（软删除，将删除标志设为1）。
     * @param locationId 库位ID
     * @return 是否成功
     */
    @Override
    public boolean removeLocationById(Long locationId) {
        WmsLocation location = new WmsLocation();
        location.setLocationId(locationId);
        location.setDeletedFlag(1);
        return baseMapper.updateById(location) > 0;
    }

    /**
     * 查询库位列表，支持关键字搜索和状态筛选。
     * @param keyword 关键字（匹配库位编码或库位名称）
     * @param status 库位状态
     * @return 库位列表
     */
    @Override
    public List<WmsLocation> listLocations(String keyword, String status) {
        return baseMapper.selectList(buildLocationQuery(keyword, status));
    }

    /**
     * 构建库位查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @return 查询条件包装器
     */
    private QueryWrapper<WmsLocation> buildLocationQuery(String keyword, String status) {
        QueryWrapper<WmsLocation> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("location_code", keyword)
                    .or().like("location_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("location_id");
        return wrapper;
    }
}
