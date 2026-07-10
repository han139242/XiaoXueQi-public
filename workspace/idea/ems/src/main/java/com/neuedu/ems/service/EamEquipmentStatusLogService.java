package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.EamEquipmentStatusLog;

import java.util.List;

/**
 * 设备状态日志 Service 接口。
 */
public interface EamEquipmentStatusLogService extends IService<EamEquipmentStatusLog> {

    /**
     * 分页查询设备状态日志，支持设备、运行状态、停机类型、告警等级和工位筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param equipmentId 设备ID
     * @param runningStatus 运行状态
     * @param downtimeType 停机类型
     * @param alertLevel 告警等级
     * @param stationId 工位ID
     * @return 分页结果
     */
    Page<EamEquipmentStatusLog> pageStatusLogs(Integer current, Integer size, Long equipmentId, String runningStatus,
                                               String downtimeType, String alertLevel, Long stationId);

    /**
     * 查询设备状态日志列表，支持设备、运行状态、停机类型、告警等级和工位筛选。
     * @param equipmentId 设备ID
     * @param runningStatus 运行状态
     * @param downtimeType 停机类型
     * @param alertLevel 告警等级
     * @param stationId 工位ID
     * @return 状态日志列表
     */
    List<EamEquipmentStatusLog> listStatusLogs(Long equipmentId, String runningStatus, String downtimeType,
                                               String alertLevel, Long stationId);

    /**
     * 根据状态日志ID查询未删除的日志详情。
     * @param statusLogId 状态日志ID
     * @return 状态日志实体
     */
    EamEquipmentStatusLog getStatusLogById(Long statusLogId);
}
