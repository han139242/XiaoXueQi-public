package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.EamMaintenanceRecord;
import com.neuedu.ems.service.EamMaintenanceRecordService;
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

/**
 * 保养记录管理控制器。
 */
@RestController
@RequestMapping("/api/eam/maintenance-record")
public class EamMaintenanceRecordController {

    @Autowired
    private EamMaintenanceRecordService eamMaintenanceRecordService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询保养记录，支持计划、设备和保养人筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param planId 保养计划ID，可选
     * @param equipmentId 设备ID，可选
     * @param maintainerId 保养人ID，可选
     * @return 保养记录分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long planId,
                           @RequestParam(required = false) Long equipmentId,
                           @RequestParam(required = false) Long maintainerId) {
        Page<EamMaintenanceRecord> result = eamMaintenanceRecordService.pageMaintenanceRecords(
                current, size, planId, equipmentId, maintainerId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据保养记录ID查询保养记录详情。
     * @param recordId 保养记录ID
     * @return 保养记录实体信息
     */
    @GetMapping("/{recordId}")
    public ResultData getMaintenanceRecord(@PathVariable("recordId") Long recordId) {
        EamMaintenanceRecord maintenanceRecord = eamMaintenanceRecordService.getMaintenanceRecordById(recordId);
        return ResultData.success(ResultCode.SUCCESS, maintenanceRecord);
    }

    /**
     * 新增保养记录。
     * @param maintenanceRecord 保养记录实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody EamMaintenanceRecord maintenanceRecord) {
        boolean flag = eamMaintenanceRecordService.saveMaintenanceRecord(maintenanceRecord);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新保养记录。
     * @param maintenanceRecord 保养记录实体，JSON格式，必须包含recordId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody EamMaintenanceRecord maintenanceRecord) {
        boolean flag = eamMaintenanceRecordService.updateMaintenanceRecord(maintenanceRecord);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除保养记录，采用软删除。
     * @param recordId 保养记录ID
     * @return 操作结果
     */
    @DeleteMapping("/{recordId}")
    public ResultData delete(@PathVariable("recordId") Long recordId) {
        boolean flag = eamMaintenanceRecordService.removeMaintenanceRecordById(recordId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出保养记录列表为Excel文件。
     * @param response HTTP响应对象
     * @param planId 保养计划ID，可选
     * @param equipmentId 设备ID，可选
     * @param maintainerId 保养人ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long planId,
                      @RequestParam(required = false) Long equipmentId,
                      @RequestParam(required = false) Long maintainerId) {
        List<EamMaintenanceRecord> maintenanceRecords = eamMaintenanceRecordService.listMaintenanceRecords(
                planId, equipmentId, maintainerId);
        excelExportUtil.exportExcel(response, maintenanceRecords, EamMaintenanceRecord.class, "保养记录", "保养记录");
    }
}
