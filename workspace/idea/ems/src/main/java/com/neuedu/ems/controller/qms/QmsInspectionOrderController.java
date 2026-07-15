package com.neuedu.ems.controller.qms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.qms.QmsInspectionResult;
import com.neuedu.ems.service.qms.QmsInspectionOrderService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 检验单管理控制器。
 */
@RestController
@RequestMapping("/api/qms/inspection-order")
public class QmsInspectionOrderController {

    @Autowired
    private QmsInspectionOrderService qmsInspectionOrderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询检验单列表，支持单号、类型、方案、工单、任务、报工、批次、检验员、状态和结果筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 检验单号关键字，可选
     * @param inspectionType 检验类型，可选
     * @param schemeId 方案ID，可选
     * @param workOrderId 工单ID，可选
     * @param taskId 任务ID，可选
     * @param reportId 报工ID，可选
     * @param batchId 批次ID，可选
     * @param inspectorId 检验员ID，可选
     * @param status 状态，可选
     * @param inspectionResult 检验结果，可选
     * @return 检验单分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String inspectionType,
                           @RequestParam(required = false) Long schemeId,
                           @RequestParam(required = false) Long workOrderId,
                           @RequestParam(required = false) Long taskId,
                           @RequestParam(required = false) Long reportId,
                           @RequestParam(required = false) Long batchId,
                           @RequestParam(required = false) Long inspectorId,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String inspectionResult) {
        Page<QmsInspectionOrder> result = qmsInspectionOrderService.pageInspectionOrders(
                current, size, keyword, inspectionType, schemeId, workOrderId, taskId, reportId, batchId, inspectorId, status, inspectionResult);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据检验单ID查询详情。
     * @param inspectionId 检验单ID
     * @return 检验单实体
     */
    @GetMapping("/{inspectionId}")
    public ResultData getInspectionOrder(@PathVariable("inspectionId") Long inspectionId) {
        QmsInspectionOrder inspectionOrder = qmsInspectionOrderService.getInspectionOrderById(inspectionId);
        return ResultData.success(ResultCode.SUCCESS, inspectionOrder);
    }

    /**
     * 新增检验单；如果请求体包含results，则同步保存检验结果明细。
     * @param request 检验单请求体，JSON格式，可包含results
     * @return 操作结果或检验单ID
     */
    @PostMapping
    public ResultData save(@RequestBody InspectionOrderFullRequest request) {
        if (request.getResults() != null) {
            Long inspectionId = qmsInspectionOrderService.saveInspectionOrderFull(request, request.getResults());
            return ResultData.success(ResultCode.SUCCESS, Map.of("inspectionId", inspectionId));
        }
        boolean flag = qmsInspectionOrderService.saveInspectionOrder(request);
        return flag ? ResultData.success(ResultCode.SUCCESS, Map.of("inspectionId", request.getInspectionId())) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 新增检验单并保存检验结果明细。
     * @param request 检验单和检验结果请求体，JSON格式，包含results
     * @return 检验单ID
     */
    @PostMapping("/full")
    public ResultData saveFull(@RequestBody InspectionOrderFullRequest request) {
        Long inspectionId = qmsInspectionOrderService.saveInspectionOrderFull(request, request.getResults());
        return ResultData.success(ResultCode.SUCCESS, Map.of("inspectionId", inspectionId));
    }

    /**
     * 修改检验单。
     * @param inspectionOrder 检验单实体，JSON格式，必须包含inspectionId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody QmsInspectionOrder inspectionOrder) {
        boolean flag = qmsInspectionOrderService.updateInspectionOrder(inspectionOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 修改检验单并替换检验结果明细。
     * @param request 检验单和检验结果请求体，JSON格式，必须包含inspectionId
     * @return 操作结果
     */
    @PutMapping("/full")
    public ResultData updateFull(@RequestBody InspectionOrderFullRequest request) {
        boolean flag = qmsInspectionOrderService.updateInspectionOrderFull(request, request.getResults());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 提交检验单结果并结束检验单。
     * @param inspectionId 检验单ID
     * @param request 请求体，支持inspectionResult、remark参数
     * @param userId JWT 中的当前用户ID
     * @return 操作结果
     */
    @PutMapping("/{inspectionId}/submit")
    public ResultData submit(@PathVariable("inspectionId") Long inspectionId,
                             @RequestAttribute("userId") Long userId,
                             @RequestBody Map<String, Object> request) {
        boolean flag = qmsInspectionOrderService.submitInspectionOrder(inspectionId, getString(request, "inspectionResult"),
                getString(request, "remark"), userId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除检验单和检验结果明细。
     * @param inspectionId 检验单ID
     * @return 操作结果
     */
    @DeleteMapping("/{inspectionId}")
    public ResultData delete(@PathVariable("inspectionId") Long inspectionId) {
        boolean flag = qmsInspectionOrderService.removeInspectionOrderById(inspectionId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出检验单列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 检验单号关键字，可选
     * @param inspectionType 检验类型，可选
     * @param schemeId 方案ID，可选
     * @param workOrderId 工单ID，可选
     * @param taskId 任务ID，可选
     * @param reportId 报工ID，可选
     * @param batchId 批次ID，可选
     * @param inspectorId 检验员ID，可选
     * @param status 状态，可选
     * @param inspectionResult 检验结果，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String inspectionType,
                      @RequestParam(required = false) Long schemeId,
                      @RequestParam(required = false) Long workOrderId,
                      @RequestParam(required = false) Long taskId,
                      @RequestParam(required = false) Long reportId,
                      @RequestParam(required = false) Long batchId,
                      @RequestParam(required = false) Long inspectorId,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) String inspectionResult) {
        List<QmsInspectionOrder> orders = qmsInspectionOrderService.listInspectionOrders(
                keyword, inspectionType, schemeId, workOrderId, taskId, reportId, batchId, inspectorId, status, inspectionResult);
        excelExportUtil.exportExcel(response, orders, QmsInspectionOrder.class, "检验单列表", "检验单列表");
    }

    /**
     * 从请求体中读取String类型参数。
     * @param request 请求体Map
     * @param key 参数名
     * @return String类型参数值
     */
    private String getString(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 检验单主表和检验结果请求体。
     */
    public static class InspectionOrderFullRequest extends QmsInspectionOrder {

        private List<QmsInspectionResult> results;

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
