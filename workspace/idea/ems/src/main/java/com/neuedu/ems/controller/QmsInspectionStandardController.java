package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.QmsInspectionStandard;
import com.neuedu.ems.service.QmsInspectionStandardService;
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
@RequestMapping("/api/qms/inspection-standard")
public class QmsInspectionStandardController {

    @Autowired
    private QmsInspectionStandardService qmsInspectionStandardService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询检验标准列表，支持检验项目、产品和工序筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param itemId 检验项目ID，可选
     * @param productId 产品ID，可选
     * @param operationId 工序ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long itemId,
                           @RequestParam(required = false) Long productId,
                           @RequestParam(required = false) Long operationId) {
        Page<QmsInspectionStandard> page = qmsInspectionStandardService.pageStandards(current, size, itemId, productId, operationId);
        return ResultData.success(ResultCode.SUCCESS, page);
    }

    /**
     * 根据检验标准ID查询检验标准详情。
     * @param standardId 检验标准ID
     * @return 检验标准实体信息
     */
    @GetMapping("/{standardId}")
    public ResultData getStandard(@PathVariable("standardId") Long standardId) {
        QmsInspectionStandard standard = qmsInspectionStandardService.getStandardById(standardId);
        return ResultData.success(ResultCode.SUCCESS, standard);
    }

    /**
     * 新增检验标准。
     * @param standard 检验标准实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody QmsInspectionStandard standard) {
        boolean flag = qmsInspectionStandardService.saveStandard(standard);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新检验标准信息。
     * @param standard 检验标准实体（JSON格式，必须包含standardId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody QmsInspectionStandard standard) {
        boolean flag = qmsInspectionStandardService.updateStandard(standard);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除检验标准（软删除，将删除标志设为1）。
     * @param standardId 检验标准ID
     * @return 操作结果
     */
    @DeleteMapping("/{standardId}")
    public ResultData delete(@PathVariable("standardId") Long standardId) {
        boolean flag = qmsInspectionStandardService.removeStandardById(standardId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 批量删除检验标准（软删除，将删除标志设为1）。
     * @param standardIds 检验标准ID列表
     * @return 操作结果
     */
    @DeleteMapping("/batch")
    public ResultData deleteBatch(@RequestBody List<Long> standardIds) {
        boolean flag = qmsInspectionStandardService.removeStandardsByIds(standardIds);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出检验标准列表为Excel文件。
     * @param response HTTP响应对象
     * @param itemId 检验项目ID，可选
     * @param productId 产品ID，可选
     * @param operationId 工序ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long itemId,
                      @RequestParam(required = false) Long productId,
                      @RequestParam(required = false) Long operationId) {
        List<QmsInspectionStandard> standards = qmsInspectionStandardService.listStandards(itemId, productId, operationId);
        excelExportUtil.exportExcel(response, standards, QmsInspectionStandard.class, "检验标准列表", "检验标准列表");
    }
}
