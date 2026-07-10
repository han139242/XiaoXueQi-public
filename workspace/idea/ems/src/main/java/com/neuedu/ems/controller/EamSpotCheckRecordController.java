package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.EamSpotCheckRecord;
import com.neuedu.ems.service.EamSpotCheckRecordService;
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
 * 点检记录管理控制器。
 */
@RestController
@RequestMapping("/api/eam/spot-check-record")
public class EamSpotCheckRecordController {

    @Autowired
    private EamSpotCheckRecordService eamSpotCheckRecordService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询点检记录，支持计划、设备、点检人和点检结果筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param planId 点检计划ID，可选
     * @param equipmentId 设备ID，可选
     * @param checkerId 点检人ID，可选
     * @param checkResult 点检结果，可选
     * @return 点检记录分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long planId,
                           @RequestParam(required = false) Long equipmentId,
                           @RequestParam(required = false) Long checkerId,
                           @RequestParam(required = false) String checkResult) {
        Page<EamSpotCheckRecord> result = eamSpotCheckRecordService.pageSpotCheckRecords(
                current, size, planId, equipmentId, checkerId, checkResult);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据点检记录ID查询点检记录详情。
     * @param recordId 点检记录ID
     * @return 点检记录实体信息
     */
    @GetMapping("/{recordId}")
    public ResultData getSpotCheckRecord(@PathVariable("recordId") Long recordId) {
        EamSpotCheckRecord spotCheckRecord = eamSpotCheckRecordService.getSpotCheckRecordById(recordId);
        return ResultData.success(ResultCode.SUCCESS, spotCheckRecord);
    }

    /**
     * 新增点检记录。
     * @param spotCheckRecord 点检记录实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody EamSpotCheckRecord spotCheckRecord) {
        boolean flag = eamSpotCheckRecordService.saveSpotCheckRecord(spotCheckRecord);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新点检记录。
     * @param spotCheckRecord 点检记录实体，JSON格式，必须包含recordId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody EamSpotCheckRecord spotCheckRecord) {
        boolean flag = eamSpotCheckRecordService.updateSpotCheckRecord(spotCheckRecord);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除点检记录，采用软删除。
     * @param recordId 点检记录ID
     * @return 操作结果
     */
    @DeleteMapping("/{recordId}")
    public ResultData delete(@PathVariable("recordId") Long recordId) {
        boolean flag = eamSpotCheckRecordService.removeSpotCheckRecordById(recordId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出点检记录列表为Excel文件。
     * @param response HTTP响应对象
     * @param planId 点检计划ID，可选
     * @param equipmentId 设备ID，可选
     * @param checkerId 点检人ID，可选
     * @param checkResult 点检结果，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long planId,
                      @RequestParam(required = false) Long equipmentId,
                      @RequestParam(required = false) Long checkerId,
                      @RequestParam(required = false) String checkResult) {
        List<EamSpotCheckRecord> spotCheckRecords = eamSpotCheckRecordService.listSpotCheckRecords(
                planId, equipmentId, checkerId, checkResult);
        excelExportUtil.exportExcel(response, spotCheckRecords, EamSpotCheckRecord.class, "点检记录", "点检记录");
    }
}
