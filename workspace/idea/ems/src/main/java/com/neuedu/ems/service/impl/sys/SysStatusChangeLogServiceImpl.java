package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.sys.SysStatusChangeLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 状态变更日志 Service 实现类。
 */
@Service
public class SysStatusChangeLogServiceImpl extends ServiceImpl<SysStatusChangeLogMapper, SysStatusChangeLog> implements SysStatusChangeLogService {

    @Override
    public Page<SysStatusChangeLog> pageStatusChangeLogs(Integer current, Integer size, String bizType, Long bizId,
                                                         String oldStatus, String newStatus, Long changedBy,
                                                         LocalDateTime startTime, LocalDateTime endTime) {
        Page<SysStatusChangeLog> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildStatusChangeLogQuery(
                bizType, bizId, oldStatus, newStatus, changedBy, startTime, endTime));
    }

    @Override
    public SysStatusChangeLog getStatusChangeLogById(Long logId) {
        QueryWrapper<SysStatusChangeLog> wrapper = new QueryWrapper<>();
        wrapper.eq("log_id", logId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public List<SysStatusChangeLog> listStatusChangeLogs(String bizType, Long bizId, String oldStatus, String newStatus,
                                                         Long changedBy, LocalDateTime startTime,
                                                         LocalDateTime endTime) {
        return baseMapper.selectList(buildStatusChangeLogQuery(
                bizType, bizId, oldStatus, newStatus, changedBy, startTime, endTime));
    }

    private QueryWrapper<SysStatusChangeLog> buildStatusChangeLogQuery(String bizType, Long bizId, String oldStatus,
                                                                       String newStatus, Long changedBy,
                                                                       LocalDateTime startTime,
                                                                       LocalDateTime endTime) {
        QueryWrapper<SysStatusChangeLog> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(bizType)) {
            wrapper.eq("biz_type", bizType);
        }
        if (bizId != null) {
            wrapper.eq("biz_id", bizId);
        }
        if (StringUtils.hasText(oldStatus)) {
            wrapper.eq("old_status", oldStatus);
        }
        if (StringUtils.hasText(newStatus)) {
            wrapper.eq("new_status", newStatus);
        }
        if (changedBy != null) {
            wrapper.eq("changed_by", changedBy);
        }
        if (startTime != null) {
            wrapper.ge("changed_time", startTime);
        }
        if (endTime != null) {
            wrapper.le("changed_time", endTime);
        }
        wrapper.orderByDesc("changed_time", "log_id");
        return wrapper;
    }
}
