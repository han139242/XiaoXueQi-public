package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.service.sys.SysOperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志 Service 实现类。
 */
@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog> implements SysOperationLogService {

    @Override
    public Page<SysOperationLog> pageOperationLogs(Integer current, Integer size, Long userId, String operationType,
                                                   String moduleCode, Long bizId, LocalDateTime startTime,
                                                   LocalDateTime endTime) {
        Page<SysOperationLog> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildOperationLogQuery(userId, operationType, moduleCode, bizId, startTime, endTime));
    }

    @Override
    public SysOperationLog getOperationLogById(Long logId) {
        QueryWrapper<SysOperationLog> wrapper = new QueryWrapper<>();
        wrapper.eq("log_id", logId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public List<SysOperationLog> listOperationLogs(Long userId, String operationType, String moduleCode, Long bizId,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectList(buildOperationLogQuery(userId, operationType, moduleCode, bizId, startTime, endTime));
    }

    private QueryWrapper<SysOperationLog> buildOperationLogQuery(Long userId, String operationType, String moduleCode,
                                                                 Long bizId, LocalDateTime startTime,
                                                                 LocalDateTime endTime) {
        QueryWrapper<SysOperationLog> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq("operation_type", operationType);
        }
        if (StringUtils.hasText(moduleCode)) {
            wrapper.eq("module_code", moduleCode);
        }
        if (bizId != null) {
            wrapper.eq("biz_id", bizId);
        }
        if (startTime != null) {
            wrapper.ge("operation_time", startTime);
        }
        if (endTime != null) {
            wrapper.le("operation_time", endTime);
        }
        wrapper.orderByDesc("operation_time", "log_id");
        return wrapper;
    }
}
