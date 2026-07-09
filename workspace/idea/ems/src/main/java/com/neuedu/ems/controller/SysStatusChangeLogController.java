package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.SysStatusChangeLog;
import com.neuedu.ems.service.SysStatusChangeLogService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sys/status-change-log")
public class SysStatusChangeLogController {

    @Autowired
    private SysStatusChangeLogService sysStatusChangeLogService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询状态变更日志列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String bizType,
                           @RequestParam(required = false) Long bizId,
                           @RequestParam(required = false) String oldStatus,
                           @RequestParam(required = false) String newStatus,
                           @RequestParam(required = false) Long changedBy,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<SysStatusChangeLog> result = sysStatusChangeLogService.pageStatusChangeLogs(
                current, size, bizType, bizId, oldStatus, newStatus, changedBy, startTime, endTime);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询状态变更日志。
     */
    @GetMapping("/{logId}")
    public ResultData getStatusChangeLog(@PathVariable("logId") Long id) {
        SysStatusChangeLog statusChangeLog = sysStatusChangeLogService.getStatusChangeLogById(id);
        return ResultData.success(ResultCode.SUCCESS, statusChangeLog);
    }

    /**
     * 导出状态变更日志 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String bizType,
                      @RequestParam(required = false) Long bizId,
                      @RequestParam(required = false) String oldStatus,
                      @RequestParam(required = false) String newStatus,
                      @RequestParam(required = false) Long changedBy,
                      @RequestParam(required = false)
                      @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                      @RequestParam(required = false)
                      @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<SysStatusChangeLog> logs = sysStatusChangeLogService.listStatusChangeLogs(
                bizType, bizId, oldStatus, newStatus, changedBy, startTime, endTime);
        excelExportUtil.exportExcel(response, logs, SysStatusChangeLog.class, "状态变更日志列表", "状态变更日志列表");
    }
}
