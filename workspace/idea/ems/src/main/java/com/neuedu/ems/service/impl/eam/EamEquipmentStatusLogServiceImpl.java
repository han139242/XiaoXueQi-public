package com.neuedu.ems.service.impl.eam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.eam.EamEquipmentStatusLog;
import com.neuedu.ems.mapper.eam.EamEquipmentStatusLogMapper;
import com.neuedu.ems.service.eam.EamEquipmentStatusLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 设备状态日志 Service 实现类。
 */
@Service
public class EamEquipmentStatusLogServiceImpl extends ServiceImpl<EamEquipmentStatusLogMapper, EamEquipmentStatusLog>
        implements EamEquipmentStatusLogService {

    /**
     * 分页查询设备状态日志，默认过滤已删除记录。
     * @param current 当前页码
     * @param size 每页记录数
     * @param equipmentId 设备ID
     * @param runningStatus 运行状态
     * @param downtimeType 停机类型
     * @param alertLevel 告警等级
     * @param stationId 工位ID
     * @return 分页结果
     */
    @Override
    public Page<EamEquipmentStatusLog> pageStatusLogs(Integer current, Integer size, Long equipmentId,
                                                      String runningStatus, String downtimeType,
                                                      String alertLevel, Long stationId) {
        Page<EamEquipmentStatusLog> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildStatusLogQuery(equipmentId, runningStatus, downtimeType, alertLevel, stationId));
    }

    /**
     * 查询设备状态日志列表，默认过滤已删除记录。
     * @param equipmentId 设备ID
     * @param runningStatus 运行状态
     * @param downtimeType 停机类型
     * @param alertLevel 告警等级
     * @param stationId 工位ID
     * @return 状态日志列表
     */
    @Override
    public List<EamEquipmentStatusLog> listStatusLogs(Long equipmentId, String runningStatus, String downtimeType,
                                                      String alertLevel, Long stationId) {
        return baseMapper.selectList(buildStatusLogQuery(equipmentId, runningStatus, downtimeType, alertLevel, stationId));
    }

    /**
     * 根据状态日志ID查询未删除的日志详情。
     * @param statusLogId 状态日志ID
     * @return 状态日志实体
     */
    @Override
    public EamEquipmentStatusLog getStatusLogById(Long statusLogId) {
        LambdaQueryWrapper<EamEquipmentStatusLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamEquipmentStatusLog::getStatusLogId, statusLogId)
                .eq(EamEquipmentStatusLog::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 构建设备状态日志查询条件。
     * @param equipmentId 设备ID
     * @param runningStatus 运行状态
     * @param downtimeType 停机类型
     * @param alertLevel 告警等级
     * @param stationId 工位ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<EamEquipmentStatusLog> buildStatusLogQuery(Long equipmentId, String runningStatus,
                                                                          String downtimeType, String alertLevel,
                                                                          Long stationId) {
        LambdaQueryWrapper<EamEquipmentStatusLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamEquipmentStatusLog::getDeletedFlag, 0)
                .eq(equipmentId != null, EamEquipmentStatusLog::getEquipmentId, equipmentId)
                .eq(StringUtils.hasText(runningStatus), EamEquipmentStatusLog::getRunningStatus, runningStatus)
                .eq(StringUtils.hasText(downtimeType), EamEquipmentStatusLog::getDowntimeType, downtimeType)
                .eq(StringUtils.hasText(alertLevel), EamEquipmentStatusLog::getAlertLevel, alertLevel)
                .eq(stationId != null, EamEquipmentStatusLog::getStationId, stationId)
                .orderByDesc(EamEquipmentStatusLog::getCollectedTime)
                .orderByDesc(EamEquipmentStatusLog::getStatusLogId);
        return queryWrapper;
    }
}
