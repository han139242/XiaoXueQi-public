package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.SysStatusChangeLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 状态变更日志 Service 接口。
 */
public interface SysStatusChangeLogService extends IService<SysStatusChangeLog> {

    Page<SysStatusChangeLog> pageStatusChangeLogs(Integer current, Integer size, String bizType, Long bizId,
                                                  String oldStatus, String newStatus, Long changedBy,
                                                  LocalDateTime startTime, LocalDateTime endTime);

    SysStatusChangeLog getStatusChangeLogById(Long logId);

    List<SysStatusChangeLog> listStatusChangeLogs(String bizType, Long bizId, String oldStatus, String newStatus,
                                                  Long changedBy, LocalDateTime startTime, LocalDateTime endTime);
}
