package com.neuedu.ems.controller.trace;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.trace.TraceMaterialUsage;
import com.neuedu.ems.service.trace.TraceMaterialUsageService;
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
 * 物料使用追溯管理控制器。
 */
@RestController
@RequestMapping("/api/trace/material-usage")
public class TraceMaterialUsageController {

    @Autowired
    private TraceMaterialUsageService traceMaterialUsageService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询物料使用追溯，支持产品批次、条码、工单、报工、物料和批次筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param productLotId 产品批次ID，可选
     * @param barcodeId 条码ID，可选
     * @param workOrderId 工单ID，可选
     * @param reportId 报工ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @return 物料使用追溯分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long productLotId,
                           @RequestParam(required = false) Long barcodeId,
                           @RequestParam(required = false) Long workOrderId,
                           @RequestParam(required = false) Long reportId,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) Long batchId) {
        Page<TraceMaterialUsage> result = traceMaterialUsageService.pageMaterialUsages(
                current, size, productLotId, barcodeId, workOrderId, reportId, materialId, batchId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据追溯ID查询物料使用追溯详情。
     * @param usageId 追溯ID
     * @return 物料使用追溯实体信息
     */
    @GetMapping("/{usageId}")
    public ResultData getMaterialUsage(@PathVariable("usageId") Long usageId) {
        TraceMaterialUsage materialUsage = traceMaterialUsageService.getMaterialUsageById(usageId);
        return ResultData.success(ResultCode.SUCCESS, materialUsage);
    }

    /**
     * 新增物料使用追溯记录。
     * @param materialUsage 追溯实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody TraceMaterialUsage materialUsage) {
        boolean flag = traceMaterialUsageService.saveMaterialUsage(materialUsage);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新物料使用追溯记录。
     * @param materialUsage 追溯实体，JSON格式，必须包含usageId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody TraceMaterialUsage materialUsage) {
        boolean flag = traceMaterialUsageService.updateMaterialUsage(materialUsage);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除物料使用追溯记录，采用软删除。
     * @param usageId 追溯ID
     * @return 操作结果
     */
    @DeleteMapping("/{usageId}")
    public ResultData delete(@PathVariable("usageId") Long usageId) {
        boolean flag = traceMaterialUsageService.removeMaterialUsageById(usageId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出物料使用追溯列表为Excel文件。
     * @param response HTTP响应对象
     * @param productLotId 产品批次ID，可选
     * @param barcodeId 条码ID，可选
     * @param workOrderId 工单ID，可选
     * @param reportId 报工ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long productLotId,
                      @RequestParam(required = false) Long barcodeId,
                      @RequestParam(required = false) Long workOrderId,
                      @RequestParam(required = false) Long reportId,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) Long batchId) {
        List<TraceMaterialUsage> materialUsages = traceMaterialUsageService.listMaterialUsages(
                productLotId, barcodeId, workOrderId, reportId, materialId, batchId);
        excelExportUtil.exportExcel(response, materialUsages, TraceMaterialUsage.class, "物料使用追溯列表", "物料使用追溯列表");
    }

    /**
     * 按原料批次查询物料使用追溯记录。
     * @param batchId 原料批次ID
     * @return 物料使用追溯列表
     */
    @GetMapping("/batch/{batchId}")
    public ResultData listByBatchId(@PathVariable("batchId") Long batchId) {
        List<TraceMaterialUsage> materialUsages = traceMaterialUsageService.listByBatchId(batchId);
        return ResultData.success(ResultCode.SUCCESS, materialUsages);
    }
}
