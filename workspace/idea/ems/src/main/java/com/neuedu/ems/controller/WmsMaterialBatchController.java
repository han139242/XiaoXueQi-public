package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.WmsMaterialBatch;
import com.neuedu.ems.service.WmsMaterialBatchService;
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
 * 物料批次管理控制器。
 */
@RestController
@RequestMapping("/api/wms/material-batch")
public class WmsMaterialBatchController {

    @Autowired
    private WmsMaterialBatchService wmsMaterialBatchService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询物料批次列表，支持批次号、物料、供应商、质量状态和状态筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配批次号或供应商批次号），可选
     * @param materialId 物料ID，可选
     * @param supplierId 供应商ID，可选
     * @param qualityStatus 质量状态，可选
     * @param status 批次状态，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) Long supplierId,
                           @RequestParam(required = false) String qualityStatus,
                           @RequestParam(required = false) String status) {
        Page<WmsMaterialBatch> result = wmsMaterialBatchService.pageMaterialBatches(current, size, keyword, materialId, supplierId, qualityStatus, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据物料批次ID查询详情。
     * @param batchId 物料批次ID
     * @return 物料批次实体信息
     */
    @GetMapping("/{batchId}")
    public ResultData getMaterialBatch(@PathVariable("batchId") Long batchId) {
        WmsMaterialBatch materialBatch = wmsMaterialBatchService.getMaterialBatchById(batchId);
        return ResultData.success(ResultCode.SUCCESS, materialBatch);
    }

    /**
     * 新增物料批次。
     * @param materialBatch 物料批次实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody WmsMaterialBatch materialBatch) {
        boolean flag = wmsMaterialBatchService.saveMaterialBatch(materialBatch);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新物料批次信息。
     * @param materialBatch 物料批次实体（JSON格式，必须包含batchId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsMaterialBatch materialBatch) {
        boolean flag = wmsMaterialBatchService.updateMaterialBatch(materialBatch);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除物料批次（软删除，将删除标志设为1）。
     * @param batchId 物料批次ID
     * @return 操作结果
     */
    @DeleteMapping("/{batchId}")
    public ResultData delete(@PathVariable("batchId") Long batchId) {
        boolean flag = wmsMaterialBatchService.removeMaterialBatchById(batchId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出物料批次列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配批次号或供应商批次号），可选
     * @param materialId 物料ID，可选
     * @param supplierId 供应商ID，可选
     * @param qualityStatus 质量状态，可选
     * @param status 批次状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) Long supplierId,
                      @RequestParam(required = false) String qualityStatus,
                      @RequestParam(required = false) String status) {
        List<WmsMaterialBatch> batches = wmsMaterialBatchService.listMaterialBatches(keyword, materialId, supplierId, qualityStatus, status);
        excelExportUtil.exportExcel(response, batches, WmsMaterialBatch.class, "物料批次列表", "物料批次列表");
    }
}
