package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.MdmMaterialCategory;
import com.neuedu.ems.service.MdmMaterialCategoryService;
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
@RequestMapping("/api/mdm/material-category")
public class MdmMaterialCategoryController {

    @Autowired
    private MdmMaterialCategoryService mdmMaterialCategoryService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询物料分类列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<MdmMaterialCategory> result = mdmMaterialCategoryService.pageCategories(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 查询物料分类树。
     */
    @GetMapping("/tree")
    public ResultData tree(@RequestParam(required = false) String status) {
        List<MdmMaterialCategory> result = mdmMaterialCategoryService.treeCategories(status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询物料分类。
     */
    @GetMapping("/{categoryId}")
    public ResultData getCategory(@PathVariable("categoryId") Long id) {
        MdmMaterialCategory category = mdmMaterialCategoryService.getCategoryById(id);
        return ResultData.success(ResultCode.SUCCESS, category);
    }

    /**
     * 新增物料分类。
     */
    @PostMapping
    public ResultData save(@RequestBody MdmMaterialCategory category) {
        boolean flag = mdmMaterialCategoryService.saveCategory(category);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新物料分类。
     */
    @PutMapping
    public ResultData update(@RequestBody MdmMaterialCategory category) {
        boolean flag = mdmMaterialCategoryService.updateCategory(category);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除物料分类，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = mdmMaterialCategoryService.removeCategoryById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出物料分类 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<MdmMaterialCategory> categories = mdmMaterialCategoryService.listCategories(keyword, status);
        excelExportUtil.exportExcel(response, categories, MdmMaterialCategory.class, "物料分类列表", "物料分类列表");
    }
}
