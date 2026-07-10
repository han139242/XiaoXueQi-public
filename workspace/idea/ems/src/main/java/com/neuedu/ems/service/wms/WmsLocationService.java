package com.neuedu.ems.service.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.wms.WmsLocation;

import java.util.List;

/**
 * 库位管理 Service 接口。
 */
public interface WmsLocationService extends IService<WmsLocation> {

    /**
     * 分页查询库位列表，支持关键字搜索和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配库位编码或库位名称）
     * @param status 库位状态
     * @return 分页结果
     */
    Page<WmsLocation> pageLocations(Integer current, Integer size, String keyword, String status);

    /**
     * 根据库位ID查询库位信息。
     * @param locationId 库位ID
     * @return 库位实体
     */
    WmsLocation getLocationById(Long locationId);

    /**
     * 新增库位。
     * @param location 库位实体
     * @return 是否成功
     */
    boolean saveLocation(WmsLocation location);

    /**
     * 更新库位信息。
     * @param location 库位实体
     * @return 是否成功
     */
    boolean updateLocation(WmsLocation location);

    /**
     * 根据ID删除库位（软删除）。
     * @param locationId 库位ID
     * @return 是否成功
     */
    boolean removeLocationById(Long locationId);

    /**
     * 查询库位列表，支持关键字搜索和状态筛选。
     * @param keyword 关键字（匹配库位编码或库位名称）
     * @param status 库位状态
     * @return 库位列表
     */
    List<WmsLocation> listLocations(String keyword, String status);
}
