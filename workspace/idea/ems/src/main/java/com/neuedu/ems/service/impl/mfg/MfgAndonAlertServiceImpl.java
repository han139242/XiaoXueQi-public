package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgAndonAlert;
import com.neuedu.ems.mapper.mfg.MfgAndonAlertMapper;
import com.neuedu.ems.service.mfg.MfgAndonAlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安灯告警管理 Service 实现类。
 */
@Service
public class MfgAndonAlertServiceImpl extends ServiceImpl<MfgAndonAlertMapper, MfgAndonAlert> implements MfgAndonAlertService {

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
    @Override
    public Page<MfgAndonAlert> pageAndonAlerts(Integer current, Integer size, String keyword, String alertType,
                                               String status, Long lineId, Long stationId) {
        Page<MfgAndonAlert> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildAndonAlertQuery(keyword, alertType, status, lineId, stationId));
    }

    /**
     * 查询安灯告警列表，支持告警编号、类型、状态、产线和工位筛选。
     * @param keyword 关键字（匹配告警编号）
     * @param alertType 告警类型
     * @param status 告警状态
     * @param lineId 产线ID
     * @param stationId 工位ID
     * @return 安灯告警列表
     */
    @Override
    public List<MfgAndonAlert> listAndonAlerts(String keyword, String alertType, String status, Long lineId, Long stationId) {
        return baseMapper.selectList(buildAndonAlertQuery(keyword, alertType, status, lineId, stationId));
    }

    /**
     * 根据安灯告警ID查询未删除的安灯告警详情。
     * @param alertId 安灯告警ID
     * @return 安灯告警实体
     */
    @Override
    public MfgAndonAlert getAndonAlertById(Long alertId) {
        LambdaQueryWrapper<MfgAndonAlert> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgAndonAlert::getAlertId, alertId)
                .eq(MfgAndonAlert::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增安灯告警，自动设置删除标志为0。
     * @param andonAlert 安灯告警实体
     * @return 是否成功
     */
    @Override
    public boolean saveAndonAlert(MfgAndonAlert andonAlert) {
        if (andonAlert.getDeletedFlag() == null) {
            andonAlert.setDeletedFlag(0);
        }
        return baseMapper.insert(andonAlert) > 0;
    }

    /**
     * 触发安灯告警，自动设置触发时间和默认状态。
     * @param andonAlert 安灯告警实体
     * @return 新增后的安灯告警实体
     */
    @Override
    @Transactional
    public MfgAndonAlert triggerAndonAlert(MfgAndonAlert andonAlert) {
        if (andonAlert.getTriggerTime() == null) {
            andonAlert.setTriggerTime(LocalDateTime.now());
        }
        if (!StringUtils.hasText(andonAlert.getStatus())) {
            andonAlert.setStatus("ACTIVE");
        }
        if (andonAlert.getDeletedFlag() == null) {
            andonAlert.setDeletedFlag(0);
        }
        baseMapper.insert(andonAlert);
        return andonAlert;
    }

    /**
     * 更新安灯告警信息，不允许修改删除标志。
     * @param andonAlert 安灯告警实体
     * @return 是否成功
     */
    @Override
    public boolean updateAndonAlert(MfgAndonAlert andonAlert) {
        if (andonAlert.getAlertId() == null) {
            return false;
        }
        andonAlert.setDeletedFlag(null);
        return baseMapper.updateById(andonAlert) > 0;
    }

    /**
     * 根据ID删除安灯告警（软删除，将删除标志设为1）。
     * @param alertId 安灯告警ID
     * @return 是否成功
     */
    @Override
    public boolean removeAndonAlertById(Long alertId) {
        MfgAndonAlert andonAlert = new MfgAndonAlert();
        andonAlert.setAlertId(alertId);
        andonAlert.setDeletedFlag(1);
        return baseMapper.updateById(andonAlert) > 0;
    }

    /**
     * 关闭安灯告警，记录解除时间并将状态改为CLOSED。
     * @param alertId 安灯告警ID
     * @param handleResult 处理结果
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean closeAndonAlert(Long alertId, String handleResult) {
        MfgAndonAlert andonAlert = new MfgAndonAlert();
        andonAlert.setAlertId(alertId);
        andonAlert.setResolvedTime(LocalDateTime.now());
        andonAlert.setStatus("CLOSED");
        if (StringUtils.hasText(handleResult)) {
            andonAlert.setRemark(handleResult);
        }
        return baseMapper.updateById(andonAlert) > 0;
    }

    /**
     * 构建安灯告警查询条件，默认过滤已删除记录。
     * @param keyword 关键字（匹配告警编号）
     * @param alertType 告警类型
     * @param status 告警状态
     * @param lineId 产线ID
     * @param stationId 工位ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<MfgAndonAlert> buildAndonAlertQuery(String keyword, String alertType,
                                                                   String status, Long lineId, Long stationId) {
        LambdaQueryWrapper<MfgAndonAlert> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgAndonAlert::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), MfgAndonAlert::getAlertNo, keyword)
                .eq(StringUtils.hasText(alertType), MfgAndonAlert::getAlertType, alertType)
                .eq(StringUtils.hasText(status), MfgAndonAlert::getStatus, status)
                .eq(lineId != null, MfgAndonAlert::getLineId, lineId)
                .eq(stationId != null, MfgAndonAlert::getStationId, stationId)
                .orderByDesc(MfgAndonAlert::getCreatedTime);
        return queryWrapper;
    }
}
