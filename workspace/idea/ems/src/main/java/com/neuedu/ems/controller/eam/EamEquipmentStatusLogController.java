package com.neuedu.ems.controller.eam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.eam.EamEquipmentStatusLog;
import com.neuedu.ems.service.eam.EamEquipmentStatusLogService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备状态日志管理控制器。
 */
@RestController
@RequestMapping("/api/eam/equipment-status-log")
public class EamEquipmentStatusLogController {

    @Autowired
    private EamEquipmentStatusLogService eamEquipmentStatusLogService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询设备状态日志，支持设备、运行状态、停机类型、告警等级和工位筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param equipmentId 设备ID，可选
     * @param runningStatus 运行状态，可选
     * @param downtimeType 停机类型，可选
     * @param alertLevel 告警等级，可选
     * @param stationId 工位ID，可选
     * @return 状态日志分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long equipmentId,
                           @RequestParam(required = false) String runningStatus,
                           @RequestParam(required = false) String downtimeType,
                           @RequestParam(required = false) String alertLevel,
                           @RequestParam(required = false) Long stationId) {
        Page<EamEquipmentStatusLog> result = eamEquipmentStatusLogService.pageStatusLogs(
                current, size, equipmentId, runningStatus, downtimeType, alertLevel, stationId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据状态日志ID查询设备状态日志详情。
     * @param statusLogId 状态日志ID
     * @return 状态日志实体信息
     */
    @GetMapping("/{statusLogId}")
    public ResultData getStatusLog(@PathVariable("statusLogId") Long statusLogId) {
        EamEquipmentStatusLog statusLog = eamEquipmentStatusLogService.getStatusLogById(statusLogId);
        return ResultData.success(ResultCode.SUCCESS, statusLog);
    }

    /**
     * 导出设备状态日志列表为Excel文件。
     * @param response HTTP响应对象
     * @param equipmentId 设备ID，可选
     * @param runningStatus 运行状态，可选
     * @param downtimeType 停机类型，可选
     * @param alertLevel 告警等级，可选
     * @param stationId 工位ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long equipmentId,
                      @RequestParam(required = false) String runningStatus,
                      @RequestParam(required = false) String downtimeType,
                      @RequestParam(required = false) String alertLevel,
                      @RequestParam(required = false) Long stationId) {
        List<EamEquipmentStatusLog> statusLogs = eamEquipmentStatusLogService.listStatusLogs(
                equipmentId, runningStatus, downtimeType, alertLevel, stationId);
        excelExportUtil.exportExcel(response, statusLogs, EamEquipmentStatusLog.class, "设备状态日志", "设备状态日志");
    }
}
