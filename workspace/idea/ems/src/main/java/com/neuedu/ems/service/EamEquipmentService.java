package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.EamEquipment;

import java.util.List;

/**
 * 设备管理 Service 接口。
 */
public interface EamEquipmentService extends IService<EamEquipment> {

    /**
     * 分页查询设备列表，支持关键字搜索、状态筛选和分类筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配设备编码或设备名称）
     * @param status 设备状态
     * @param categoryId 设备分类ID
     * @return 分页结果
     */
    Page<EamEquipment> pageEquipments(Integer current, Integer size, String keyword, String status, Long categoryId);

    /**
     * 根据设备ID查询设备信息。
     * @param equipmentId 设备ID
     * @return 设备实体
     */
    EamEquipment getEquipmentById(Long equipmentId);

    /**
     * 新增设备。
     * @param equipment 设备实体
     * @return 是否成功
     */
    boolean saveEquipment(EamEquipment equipment);

    /**
     * 更新设备信息。
     * @param equipment 设备实体
     * @return 是否成功
     */
    boolean updateEquipment(EamEquipment equipment);

    /**
     * 根据ID删除设备（软删除）。
     * @param equipmentId 设备ID
     * @return 是否成功
     */
    boolean removeEquipmentById(Long equipmentId);

    /**
     * 查询设备列表，支持关键字搜索、状态筛选和分类筛选。
     * @param keyword 关键字（匹配设备编码或设备名称）
     * @param status 设备状态
     * @param categoryId 设备分类ID
     * @return 设备列表
     */
    List<EamEquipment> listEquipments(String keyword, String status, Long categoryId);
}
