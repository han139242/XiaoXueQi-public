package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.MfgReportMaterial;
import com.neuedu.ems.service.MfgReportMaterialService;
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
 * 报工用料消耗管理控制器。
 */
@RestController
@RequestMapping("/api/mfg/report-material")
public class MfgReportMaterialController {

    @Autowired
    private MfgReportMaterialService mfgReportMaterialService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询报工用料消耗列表，支持报工、物料和批次筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param reportId 报工ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @return 报工用料分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long reportId,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) Long batchId) {
        Page<MfgReportMaterial> result = mfgReportMaterialService.pageReportMaterials(current, size, reportId, materialId, batchId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据报工ID查询用料明细列表。
     * @param reportId 报工ID
     * @return 报工用料明细列表
     */
    @GetMapping("/report/{reportId}")
    public ResultData listByReport(@PathVariable("reportId") Long reportId) {
        List<MfgReportMaterial> materials = mfgReportMaterialService.listReportMaterialsByReportId(reportId);
        return ResultData.success(ResultCode.SUCCESS, materials);
    }

    /**
     * 根据报工用料ID查询详情。
     * @param id 报工用料ID
     * @return 报工用料实体
     */
    @GetMapping("/{id}")
    public ResultData getReportMaterial(@PathVariable("id") Long id) {
        MfgReportMaterial material = mfgReportMaterialService.getReportMaterialById(id);
        return ResultData.success(ResultCode.SUCCESS, material);
    }

    /**
     * 新增报工用料。
     * @param reportMaterial 报工用料实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody MfgReportMaterial reportMaterial) {
        boolean flag = mfgReportMaterialService.saveReportMaterial(reportMaterial);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 修改报工用料。
     * @param reportMaterial 报工用料实体，JSON格式，必须包含id
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgReportMaterial reportMaterial) {
        boolean flag = mfgReportMaterialService.updateReportMaterial(reportMaterial);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除报工用料。
     * @param id 报工用料ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = mfgReportMaterialService.removeReportMaterialById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出报工用料列表为Excel文件。
     * @param response HTTP响应对象
     * @param reportId 报工ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long reportId,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) Long batchId) {
        List<MfgReportMaterial> materials = mfgReportMaterialService.listReportMaterials(reportId, materialId, batchId);
        excelExportUtil.exportExcel(response, materials, MfgReportMaterial.class, "报工用料列表", "报工用料列表");
    }
}
