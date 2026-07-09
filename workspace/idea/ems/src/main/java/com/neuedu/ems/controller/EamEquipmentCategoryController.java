package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.EamEquipmentCategory;
import com.neuedu.ems.service.EamEquipmentCategoryService;
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
 * 设备分类管理控制器。
 * 提供设备分类的增删改查、分页查询、树形结构查询和导出功能。
 */
@RestController
@RequestMapping("/api/eam/equipment-category")
public class EamEquipmentCategoryController {

    @Autowired
    private EamEquipmentCategoryService eamEquipmentCategoryService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询设备分类列表，支持关键字搜索和状态筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配分类编码或分类名称），可选
     * @param status 分类状态，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<EamEquipmentCategory> result = eamEquipmentCategoryService.pageCategories(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据设备分类ID查询设备分类详情。
     * @param categoryId 设备分类ID
     * @return 设备分类实体信息
     */
    @GetMapping("/{categoryId}")
    public ResultData getCategory(@PathVariable("categoryId") Long categoryId) {
        EamEquipmentCategory category = eamEquipmentCategoryService.getCategoryById(categoryId);
        return ResultData.success(ResultCode.SUCCESS, category);
    }

    /**
     * 新增设备分类。
     * @param category 设备分类实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody EamEquipmentCategory category) {
        boolean flag = eamEquipmentCategoryService.saveCategory(category);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新设备分类信息。
     * @param category 设备分类实体（JSON格式，必须包含categoryId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody EamEquipmentCategory category) {
        boolean flag = eamEquipmentCategoryService.updateCategory(category);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除设备分类（软删除，将删除标志设为1）。
     * @param categoryId 设备分类ID
     * @return 操作结果
     */
    @DeleteMapping("/{categoryId}")
    public ResultData delete(@PathVariable("categoryId") Long categoryId) {
        boolean flag = eamEquipmentCategoryService.removeCategoryById(categoryId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 根据父级ID查询子分类列表，用于构建树形结构。
     * @param parentId 父级分类ID，如果为空则查询根节点
     * @return 子分类列表
     */
    @GetMapping("/children")
    public ResultData listChildren(@RequestParam(required = false) Long parentId) {
        List<EamEquipmentCategory> categories = eamEquipmentCategoryService.listCategoriesByParentId(parentId);
        return ResultData.success(ResultCode.SUCCESS, categories);
    }

    /**
     * 导出设备分类列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配分类编码或分类名称），可选
     * @param status 分类状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<EamEquipmentCategory> categories = eamEquipmentCategoryService.listCategories(keyword, status);
        excelExportUtil.exportExcel(response, categories, EamEquipmentCategory.class, "设备分类列表", "设备分类列表");
    }
}
