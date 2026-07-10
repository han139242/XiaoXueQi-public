package com.neuedu.ems.controller.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.service.sys.SysOperationLogService;
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
@RequestMapping("/api/sys/operation-log")
public class SysOperationLogController {

    @Autowired
    private SysOperationLogService sysOperationLogService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询操作日志列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long userId,
                           @RequestParam(required = false) String operationType,
                           @RequestParam(required = false) String moduleCode,
                           @RequestParam(required = false) Long bizId,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<SysOperationLog> result = sysOperationLogService.pageOperationLogs(
                current, size, userId, operationType, moduleCode, bizId, startTime, endTime);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询操作日志。
     */
    @GetMapping("/{logId}")
    public ResultData getOperationLog(@PathVariable("logId") Long id) {
        SysOperationLog operationLog = sysOperationLogService.getOperationLogById(id);
        return ResultData.success(ResultCode.SUCCESS, operationLog);
    }

    /**
     * 导出操作日志 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long userId,
                      @RequestParam(required = false) String operationType,
                      @RequestParam(required = false) String moduleCode,
                      @RequestParam(required = false) Long bizId,
                      @RequestParam(required = false)
                      @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                      @RequestParam(required = false)
                      @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<SysOperationLog> operationLogs = sysOperationLogService.listOperationLogs(
                userId, operationType, moduleCode, bizId, startTime, endTime);
        excelExportUtil.exportExcel(response, operationLogs, SysOperationLog.class, "操作日志列表", "操作日志列表");
    }
}
