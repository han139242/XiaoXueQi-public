package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.QmsInspectionScheme;
import com.neuedu.ems.entity.QmsInspectionSchemeItem;
import com.neuedu.ems.service.QmsInspectionSchemeService;
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

@RestController
@RequestMapping("/api/qms/inspection-scheme")
public class QmsInspectionSchemeController {

    @Autowired
    private QmsInspectionSchemeService qmsInspectionSchemeService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询检验方案列表，支持方案编码、名称、产品、工序、检验类型和状态筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param schemeCode 方案编码，可选
     * @param schemeName 方案名称，可选
     * @param productId 产品ID，可选
     * @param operationId 工序ID，可选
     * @param inspectionType 检验类型，可选
     * @param status 方案状态，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String schemeCode,
                           @RequestParam(required = false) String schemeName,
                           @RequestParam(required = false) Long productId,
                           @RequestParam(required = false) Long operationId,
                           @RequestParam(required = false) String inspectionType,
                           @RequestParam(required = false) String status) {
        Page<QmsInspectionScheme> page = qmsInspectionSchemeService.pageSchemes(
                current, size, schemeCode, schemeName, productId, operationId, inspectionType, status);
        return ResultData.success(ResultCode.SUCCESS, page);
    }

    /**
     * 查询检验方案列表，支持方案编码、名称、产品、工序、检验类型和状态筛选。
     * @param schemeCode 方案编码，可选
     * @param schemeName 方案名称，可选
     * @param productId 产品ID，可选
     * @param operationId 工序ID，可选
     * @param inspectionType 检验类型，可选
     * @param status 方案状态，可选
     * @return 检验方案列表
     */
    @GetMapping("/list")
    public ResultData listAll(@RequestParam(required = false) String schemeCode,
                              @RequestParam(required = false) String schemeName,
                              @RequestParam(required = false) Long productId,
                              @RequestParam(required = false) Long operationId,
                              @RequestParam(required = false) String inspectionType,
                              @RequestParam(required = false) String status) {
        List<QmsInspectionScheme> list = qmsInspectionSchemeService.listSchemes(
                schemeCode, schemeName, productId, operationId, inspectionType, status);
        return ResultData.success(ResultCode.SUCCESS, list);
    }

    /**
     * 根据检验方案ID查询方案明细列表。
     * @param schemeId 检验方案ID
     * @return 检验方案明细列表
     */
    @GetMapping("/{schemeId}/items")
    public ResultData listItems(@PathVariable("schemeId") Long schemeId) {
        List<QmsInspectionSchemeItem> items = qmsInspectionSchemeService.listSchemeItems(schemeId);
        return ResultData.success(ResultCode.SUCCESS, items);
    }

    /**
     * 根据检验方案ID查询检验方案详情。
     * @param schemeId 检验方案ID
     * @return 检验方案实体信息
     */
    @GetMapping("/{schemeId}")
    public ResultData getScheme(@PathVariable("schemeId") Long schemeId) {
        QmsInspectionScheme scheme = qmsInspectionSchemeService.getSchemeById(schemeId);
        return ResultData.success(ResultCode.SUCCESS, scheme);
    }

    /**
     * 新增检验方案。
     * @param scheme 检验方案实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody QmsInspectionScheme scheme) {
        boolean flag = qmsInspectionSchemeService.saveScheme(scheme);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新检验方案信息。
     * @param scheme 检验方案实体（JSON格式，必须包含schemeId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody QmsInspectionScheme scheme) {
        boolean flag = qmsInspectionSchemeService.updateScheme(scheme);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除检验方案（软删除，将删除标志设为1）。
     * @param schemeId 检验方案ID
     * @return 操作结果
     */
    @DeleteMapping("/{schemeId}")
    public ResultData delete(@PathVariable("schemeId") Long schemeId) {
        boolean flag = qmsInspectionSchemeService.removeSchemeById(schemeId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 批量删除检验方案（软删除，将删除标志设为1）。
     * @param schemeIds 检验方案ID列表
     * @return 操作结果
     */
    @DeleteMapping("/batch")
    public ResultData deleteBatch(@RequestBody List<Long> schemeIds) {
        boolean flag = qmsInspectionSchemeService.removeSchemesByIds(schemeIds);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出检验方案列表为Excel文件。
     * @param response HTTP响应对象
     * @param schemeCode 方案编码，可选
     * @param schemeName 方案名称，可选
     * @param productId 产品ID，可选
     * @param operationId 工序ID，可选
     * @param inspectionType 检验类型，可选
     * @param status 方案状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String schemeCode,
                      @RequestParam(required = false) String schemeName,
                      @RequestParam(required = false) Long productId,
                      @RequestParam(required = false) Long operationId,
                      @RequestParam(required = false) String inspectionType,
                      @RequestParam(required = false) String status) {
        List<QmsInspectionScheme> list = qmsInspectionSchemeService.listSchemes(
                schemeCode, schemeName, productId, operationId, inspectionType, status);
        excelExportUtil.exportExcel(response, list, QmsInspectionScheme.class, "检验方案列表", "检验方案列表");
    }
}
