package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.SysOperationLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志 Service 接口。
 */
public interface SysOperationLogService extends IService<SysOperationLog> {

    Page<SysOperationLog> pageOperationLogs(Integer current, Integer size, Long userId, String operationType,
                                            String moduleCode, Long bizId, LocalDateTime startTime,
                                            LocalDateTime endTime);

    SysOperationLog getOperationLogById(Long logId);

    List<SysOperationLog> listOperationLogs(Long userId, String operationType, String moduleCode, Long bizId,
                                            LocalDateTime startTime, LocalDateTime endTime);
}
