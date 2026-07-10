package com.neuedu.ems.service.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mfg.MfgAndonAlert;

import java.util.List;

/**
 * 安灯告警管理 Service 接口。
 */
public interface MfgAndonAlertService extends IService<MfgAndonAlert> {

    /**
     * 分页查询安灯告警列表，支持告警编号、类型、状态、产线和工位筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配告警编号）
     * @param alertType 告警类型
     * @param status 告警状态
     * @param lineId 产线ID
     * @param stationId 工位ID
     * @return 分页结果
     */
    Page<MfgAndonAlert> pageAndonAlerts(Integer current, Integer size, String keyword, String alertType,
                                        String status, Long lineId, Long stationId);

    /**
     * 查询安灯告警列表，支持告警编号、类型、状态、产线和工位筛选。
     * @param keyword 关键字（匹配告警编号）
     * @param alertType 告警类型
     * @param status 告警状态
     * @param lineId 产线ID
     * @param stationId 工位ID
     * @return 安灯告警列表
     */
    List<MfgAndonAlert> listAndonAlerts(String keyword, String alertType, String status, Long lineId, Long stationId);

    /**
     * 根据安灯告警ID查询详情。
     * @param alertId 安灯告警ID
     * @return 安灯告警实体
     */
    MfgAndonAlert getAndonAlertById(Long alertId);

    /**
     * 新增安灯告警。
     * @param andonAlert 安灯告警实体
     * @return 是否成功
     */
    boolean saveAndonAlert(MfgAndonAlert andonAlert);

    /**
     * 触发安灯告警，自动设置触发时间和默认状态。
     * @param andonAlert 安灯告警实体
     * @return 新增后的安灯告警实体
     */
    MfgAndonAlert triggerAndonAlert(MfgAndonAlert andonAlert);

    /**
     * 更新安灯告警信息。
     * @param andonAlert 安灯告警实体
     * @return 是否成功
     */
    boolean updateAndonAlert(MfgAndonAlert andonAlert);

    /**
     * 根据ID删除安灯告警（软删除）。
     * @param alertId 安灯告警ID
     * @return 是否成功
     */
    boolean removeAndonAlertById(Long alertId);

    /**
     * 关闭安灯告警，记录解除时间并将状态改为CLOSED。
     * @param alertId 安灯告警ID
     * @param handleResult 处理结果
     * @return 是否成功
     */
    boolean closeAndonAlert(Long alertId, String handleResult);
}
