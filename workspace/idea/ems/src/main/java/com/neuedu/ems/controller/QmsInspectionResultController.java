package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.QmsInspectionResult;
import com.neuedu.ems.service.QmsInspectionResultService;
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
 * 检验结果管理控制器。
 */
@RestController
@RequestMapping("/api/qms/inspection-result")
public class QmsInspectionResultController {

    @Autowired
    private QmsInspectionResultService qmsInspectionResultService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询检验结果列表，支持检验单、检验项目、结果和图片筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param inspectionId 检验单ID，可选
     * @param itemId 检验项目ID，可选
     * @param result 检验结果，可选
     * @param photoFileId 图片附件ID，可选
     * @return 检验结果分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long inspectionId,
                           @RequestParam(required = false) Long itemId,
                           @RequestParam(required = false) String result,
                           @RequestParam(required = false) Long photoFileId) {
        Page<QmsInspectionResult> page = qmsInspectionResultService.pageInspectionResults(current, size, inspectionId, itemId, result, photoFileId);
        return ResultData.success(ResultCode.SUCCESS, page);
    }

    /**
     * 根据检验单ID查询检验结果列表。
     * @param inspectionId 检验单ID
     * @return 检验结果列表
     */
    @GetMapping("/inspection/{inspectionId}")
    public ResultData listByInspection(@PathVariable("inspectionId") Long inspectionId) {
        List<QmsInspectionResult> results = qmsInspectionResultService.listInspectionResultsByInspectionId(inspectionId);
        return ResultData.success(ResultCode.SUCCESS, results);
    }

    /**
     * 根据检验结果ID查询详情。
     * @param resultId 检验结果ID
     * @return 检验结果实体
     */
    @GetMapping("/{resultId}")
    public ResultData getInspectionResult(@PathVariable("resultId") Long resultId) {
        QmsInspectionResult result = qmsInspectionResultService.getInspectionResultById(resultId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 新增检验结果。
     * @param inspectionResult 检验结果实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody QmsInspectionResult inspectionResult) {
        boolean flag = qmsInspectionResultService.saveInspectionResult(inspectionResult);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 批量保存指定检验单的检验结果。
     * @param request 批量保存请求体，包含inspectionId和results
     * @return 操作结果
     */
    @PostMapping("/batch")
    public ResultData saveBatch(@RequestBody InspectionResultBatchRequest request) {
        boolean flag = qmsInspectionResultService.saveInspectionResultsBatch(request.getInspectionId(), request.getResults());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 修改检验结果。
     * @param inspectionResult 检验结果实体，JSON格式，必须包含resultId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody QmsInspectionResult inspectionResult) {
        boolean flag = qmsInspectionResultService.updateInspectionResult(inspectionResult);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除检验结果。
     * @param resultId 检验结果ID
     * @return 操作结果
     */
    @DeleteMapping("/{resultId}")
    public ResultData delete(@PathVariable("resultId") Long resultId) {
        boolean flag = qmsInspectionResultService.removeInspectionResultById(resultId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出检验结果列表为Excel文件。
     * @param response HTTP响应对象
     * @param inspectionId 检验单ID，可选
     * @param itemId 检验项目ID，可选
     * @param result 检验结果，可选
     * @param photoFileId 图片附件ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long inspectionId,
                      @RequestParam(required = false) Long itemId,
                      @RequestParam(required = false) String result,
                      @RequestParam(required = false) Long photoFileId) {
        List<QmsInspectionResult> results = qmsInspectionResultService.listInspectionResults(inspectionId, itemId, result, photoFileId);
        excelExportUtil.exportExcel(response, results, QmsInspectionResult.class, "检验结果列表", "检验结果列表");
    }

    /**
     * 检验结果批量保存请求体。
     */
    public static class InspectionResultBatchRequest {

        private Long inspectionId;
        private List<QmsInspectionResult> results;

        /**
         * 获取检验单ID。
         * @return 检验单ID
         */
        public Long getInspectionId() {
            return inspectionId;
        }

        /**
         * 设置检验单ID。
         * @param inspectionId 检验单ID
         */
        public void setInspectionId(Long inspectionId) {
            this.inspectionId = inspectionId;
        }

        /**
         * 获取检验结果列表。
         * @return 检验结果列表
         */
        public List<QmsInspectionResult> getResults() {
            return results;
        }

        /**
         * 设置检验结果列表。
         * @param results 检验结果列表
         */
        public void setResults(List<QmsInspectionResult> results) {
            this.results = results;
        }
    }
}
