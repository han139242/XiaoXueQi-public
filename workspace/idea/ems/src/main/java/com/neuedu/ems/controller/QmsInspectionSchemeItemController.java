package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.QmsInspectionSchemeItem;
import com.neuedu.ems.service.QmsInspectionSchemeItemService;
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
@RequestMapping("/api/qms/inspection-scheme-item")
public class QmsInspectionSchemeItemController {

    @Autowired
    private QmsInspectionSchemeItemService qmsInspectionSchemeItemService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询检验方案明细列表，支持方案、检验项目和检验标准筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param schemeId 检验方案ID，可选
     * @param itemId 检验项目ID，可选
     * @param standardId 检验标准ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long schemeId,
                           @RequestParam(required = false) Long itemId,
                           @RequestParam(required = false) Long standardId) {
        Page<QmsInspectionSchemeItem> page = qmsInspectionSchemeItemService.pageSchemeItems(current, size, schemeId, itemId, standardId);
        return ResultData.success(ResultCode.SUCCESS, page);
    }

    /**
     * 查询检验方案明细列表，支持方案、检验项目和检验标准筛选。
     * @param schemeId 检验方案ID，可选
     * @param itemId 检验项目ID，可选
     * @param standardId 检验标准ID，可选
     * @return 检验方案明细列表
     */
    @GetMapping("/list")
    public ResultData listAll(@RequestParam(required = false) Long schemeId,
                              @RequestParam(required = false) Long itemId,
                              @RequestParam(required = false) Long standardId) {
        List<QmsInspectionSchemeItem> list = qmsInspectionSchemeItemService.listSchemeItems(schemeId, itemId, standardId);
        return ResultData.success(ResultCode.SUCCESS, list);
    }

    /**
     * 根据方案明细ID查询检验方案明细详情。
     * @param id 方案明细ID
     * @return 检验方案明细实体信息
     */
    @GetMapping("/{id}")
    public ResultData getSchemeItem(@PathVariable("id") Long id) {
        QmsInspectionSchemeItem schemeItem = qmsInspectionSchemeItemService.getSchemeItemById(id);
        return ResultData.success(ResultCode.SUCCESS, schemeItem);
    }

    /**
     * 新增检验方案明细。
     * @param schemeItem 检验方案明细实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody QmsInspectionSchemeItem schemeItem) {
        boolean flag = qmsInspectionSchemeItemService.saveSchemeItem(schemeItem);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新检验方案明细信息。
     * @param schemeItem 检验方案明细实体（JSON格式，必须包含id）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody QmsInspectionSchemeItem schemeItem) {
        boolean flag = qmsInspectionSchemeItemService.updateSchemeItem(schemeItem);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除检验方案明细（软删除，将删除标志设为1）。
     * @param id 方案明细ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = qmsInspectionSchemeItemService.removeSchemeItemById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 批量删除检验方案明细（软删除，将删除标志设为1）。
     * @param ids 方案明细ID列表
     * @return 操作结果
     */
    @DeleteMapping("/batch")
    public ResultData deleteBatch(@RequestBody List<Long> ids) {
        boolean flag = qmsInspectionSchemeItemService.removeSchemeItemsByIds(ids);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出检验方案明细列表为Excel文件。
     * @param response HTTP响应对象
     * @param schemeId 检验方案ID，可选
     * @param itemId 检验项目ID，可选
     * @param standardId 检验标准ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long schemeId,
                      @RequestParam(required = false) Long itemId,
                      @RequestParam(required = false) Long standardId) {
        List<QmsInspectionSchemeItem> list = qmsInspectionSchemeItemService.listSchemeItems(schemeId, itemId, standardId);
        excelExportUtil.exportExcel(response, list, QmsInspectionSchemeItem.class, "检验方案明细列表", "检验方案明细列表");
    }
}
