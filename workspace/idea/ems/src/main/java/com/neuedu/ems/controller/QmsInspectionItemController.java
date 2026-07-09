package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.QmsInspectionItem;
import com.neuedu.ems.service.QmsInspectionItemService;
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
 * 检验项目管理控制器。
 * 提供检验项目的增删改查、分页查询和导出功能。
 */
@RestController
@RequestMapping("/api/qms/inspection-item")
public class QmsInspectionItemController {

    @Autowired
    private QmsInspectionItemService qmsInspectionItemService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询检验项目列表，支持关键字搜索和类型筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配项目编码或项目名称），可选
     * @param itemType 项目类型（尺寸、性能、外观、重量），可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String itemType) {
        Page<QmsInspectionItem> result = qmsInspectionItemService.pageItems(current, size, keyword, itemType);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据检验项目ID查询检验项目详情。
     * @param itemId 检验项目ID
     * @return 检验项目实体信息
     */
    @GetMapping("/{itemId}")
    public ResultData getItem(@PathVariable("itemId") Long itemId) {
        QmsInspectionItem item = qmsInspectionItemService.getItemById(itemId);
        return ResultData.success(ResultCode.SUCCESS, item);
    }

    /**
     * 新增检验项目。
     * @param item 检验项目实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody QmsInspectionItem item) {
        boolean flag = qmsInspectionItemService.saveItem(item);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新检验项目信息。
     * @param item 检验项目实体（JSON格式，必须包含itemId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody QmsInspectionItem item) {
        boolean flag = qmsInspectionItemService.updateItem(item);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除检验项目（软删除，将删除标志设为1）。
     * @param itemId 检验项目ID
     * @return 操作结果
     */
    @DeleteMapping("/{itemId}")
    public ResultData delete(@PathVariable("itemId") Long itemId) {
        boolean flag = qmsInspectionItemService.removeItemById(itemId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出检验项目列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配项目编码或项目名称），可选
     * @param itemType 项目类型，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String itemType) {
        List<QmsInspectionItem> items = qmsInspectionItemService.listItems(keyword, itemType);
        excelExportUtil.exportExcel(response, items, QmsInspectionItem.class, "检验项目列表", "检验项目列表");
    }
}
