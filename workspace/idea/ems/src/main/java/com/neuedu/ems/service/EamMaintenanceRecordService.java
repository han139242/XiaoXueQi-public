package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.EamMaintenanceRecord;

import java.util.List;

/**
 * 保养记录 Service 接口。
 */
public interface EamMaintenanceRecordService extends IService<EamMaintenanceRecord> {

    /**
     * 分页查询保养记录，支持计划、设备和保养人筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param planId 保养计划ID
     * @param equipmentId 设备ID
     * @param maintainerId 保养人ID
     * @return 分页结果
     */
    Page<EamMaintenanceRecord> pageMaintenanceRecords(Integer current, Integer size, Long planId,
                                                      Long equipmentId, Long maintainerId);

    /**
     * 查询保养记录列表，支持计划、设备和保养人筛选。
     * @param planId 保养计划ID
     * @param equipmentId 设备ID
     * @param maintainerId 保养人ID
     * @return 保养记录列表
     */
    List<EamMaintenanceRecord> listMaintenanceRecords(Long planId, Long equipmentId, Long maintainerId);

    /**
     * 根据保养记录ID查询未删除的记录详情。
     * @param recordId 保养记录ID
     * @return 保养记录实体
     */
    EamMaintenanceRecord getMaintenanceRecordById(Long recordId);

    /**
     * 新增保养记录。
     * @param maintenanceRecord 保养记录实体
     * @return 是否成功
     */
    boolean saveMaintenanceRecord(EamMaintenanceRecord maintenanceRecord);

    /**
     * 更新保养记录。
     * @param maintenanceRecord 保养记录实体，必须包含recordId
     * @return 是否成功
     */
    boolean updateMaintenanceRecord(EamMaintenanceRecord maintenanceRecord);

    /**
     * 根据ID软删除保养记录。
     * @param recordId 保养记录ID
     * @return 是否成功
     */
    boolean removeMaintenanceRecordById(Long recordId);
}
