package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.MfgAndonAlert;
import com.neuedu.ems.service.MfgAndonAlertService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 安灯告警管理控制器。
 */
@RestController
@RequestMapping("/api/mfg/andon-alert")
public class MfgAndonAlertController {

    @Autowired
    private MfgAndonAlertService mfgAndonAlertService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询安灯告警列表，支持告警编号、类型、状态、产线和工位筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配告警编号），可选
     * @param alertType 告警类型，可选
     * @param status 告警状态，可选
     * @param lineId 产线ID，可选
     * @param stationId 工位ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String alertType,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long lineId,
                           @RequestParam(required = false) Long stationId) {
        Page<MfgAndonAlert> result = mfgAndonAlertService.pageAndonAlerts(current, size, keyword, alertType, status, lineId, stationId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据安灯告警ID查询详情。
     * @param alertId 安灯告警ID
     * @return 安灯告警实体信息
     */
    @GetMapping("/{alertId}")
    public ResultData getAndonAlert(@PathVariable("alertId") Long alertId) {
        MfgAndonAlert andonAlert = mfgAndonAlertService.getAndonAlertById(alertId);
        return ResultData.success(ResultCode.SUCCESS, andonAlert);
    }

    /**
     * 新增安灯告警。
     * @param andonAlert 安灯告警实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody MfgAndonAlert andonAlert) {
        boolean flag = mfgAndonAlertService.saveAndonAlert(andonAlert);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 触发安灯告警，自动设置触发时间和默认状态。
     * @param andonAlert 安灯告警实体（JSON格式，字段以mfg_andon_alert表为准）
     * @return 新增告警ID
     */
    @PostMapping("/trigger")
    public ResultData trigger(@RequestBody MfgAndonAlert andonAlert) {
        MfgAndonAlert saved = mfgAndonAlertService.triggerAndonAlert(andonAlert);
        return ResultData.success(ResultCode.SUCCESS, Map.of("alertId", saved.getAlertId()));
    }

    /**
     * 更新安灯告警信息。
     * @param andonAlert 安灯告警实体（JSON格式，必须包含alertId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgAndonAlert andonAlert) {
        boolean flag = mfgAndonAlertService.updateAndonAlert(andonAlert);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 关闭安灯告警，记录解除时间并将状态改为CLOSED。
     * @param alertId 安灯告警ID
     * @param request 请求体，支持handleResult参数
     * @return 操作结果
     */
    @PutMapping("/{alertId}/close")
    public ResultData close(@PathVariable("alertId") Long alertId,
                            @RequestBody(required = false) Map<String, Object> request) {
        boolean flag = mfgAndonAlertService.closeAndonAlert(alertId, getString(request, "handleResult"));
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除安灯告警（软删除，将删除标志设为1）。
     * @param alertId 安灯告警ID
     * @return 操作结果
     */
    @DeleteMapping("/{alertId}")
    public ResultData delete(@PathVariable("alertId") Long alertId) {
        boolean flag = mfgAndonAlertService.removeAndonAlertById(alertId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出安灯告警列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配告警编号），可选
     * @param alertType 告警类型，可选
     * @param status 告警状态，可选
     * @param lineId 产线ID，可选
     * @param stationId 工位ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String alertType,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) Long lineId,
                      @RequestParam(required = false) Long stationId) {
        List<MfgAndonAlert> andonAlerts = mfgAndonAlertService.listAndonAlerts(keyword, alertType, status, lineId, stationId);
        excelExportUtil.exportExcel(response, andonAlerts, MfgAndonAlert.class, "安灯告警列表", "安灯告警列表");
    }

    /**
     * 从请求体中读取String类型参数。
     * @param request 请求体Map
     * @param key 参数名
     * @return String类型参数值
     */
    private String getString(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : value.toString();
    }
}
