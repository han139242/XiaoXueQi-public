package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.QmsNonconformingDetail;
import com.neuedu.ems.entity.QmsNonconformingOrder;
import com.neuedu.ems.service.QmsNonconformingOrderService;
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
import java.util.Map;

/**
 * 不合格品处理单管理控制器。
 */
@RestController
@RequestMapping("/api/qms/nonconforming-order")
public class QmsNonconformingOrderController {

    @Autowired
    private QmsNonconformingOrderService qmsNonconformingOrderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询不合格品处理单列表，支持单号、检验单、产品批次、处理方式、审批状态和状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 处理单号关键字，可选
     * @param inspectionId 检验单ID，可选
     * @param productLotId 产品批次ID，可选
     * @param dispositionType 处理方式，可选
     * @param approvalStatus 审批状态，可选
     * @param status 状态，可选
     * @return 不合格品处理单分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long inspectionId,
                           @RequestParam(required = false) Long productLotId,
                           @RequestParam(required = false) String dispositionType,
                           @RequestParam(required = false) String approvalStatus,
                           @RequestParam(required = false) String status) {
        Page<QmsNonconformingOrder> result = qmsNonconformingOrderService.pageNonconformingOrders(
                current, size, keyword, inspectionId, productLotId, dispositionType, approvalStatus, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据不合格品处理单ID查询详情。
     * @param ncrId 不合格品处理单ID
     * @return 不合格品处理单实体
     */
    @GetMapping("/{ncrId}")
    public ResultData getNonconformingOrder(@PathVariable("ncrId") Long ncrId) {
        QmsNonconformingOrder order = qmsNonconformingOrderService.getNonconformingOrderById(ncrId);
        return ResultData.success(ResultCode.SUCCESS, order);
    }

    /**
     * 新增不合格品处理单；如果请求体包含details，则同步保存处理明细。
     * @param request 不合格品处理单请求体，JSON格式，可包含details
     * @return 操作结果或处理单ID
     */
    @PostMapping
    public ResultData save(@RequestBody NonconformingOrderFullRequest request) {
        if (request.getDetails() != null) {
            Long ncrId = qmsNonconformingOrderService.saveNonconformingOrderFull(request, request.getDetails());
            return ResultData.success(ResultCode.SUCCESS, Map.of("ncrId", ncrId));
        }
        boolean flag = qmsNonconformingOrderService.saveNonconformingOrder(request);
        return flag ? ResultData.success(ResultCode.SUCCESS, Map.of("ncrId", request.getNcrId())) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 新增不合格品处理单并保存处理明细。
     * @param request 不合格品处理单和明细请求体，JSON格式，包含details
     * @return 处理单ID
     */
    @PostMapping("/full")
    public ResultData saveFull(@RequestBody NonconformingOrderFullRequest request) {
        Long ncrId = qmsNonconformingOrderService.saveNonconformingOrderFull(request, request.getDetails());
        return ResultData.success(ResultCode.SUCCESS, Map.of("ncrId", ncrId));
    }

    /**
     * 修改不合格品处理单。
     * @param order 不合格品处理单实体，JSON格式，必须包含ncrId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody QmsNonconformingOrder order) {
        boolean flag = qmsNonconformingOrderService.updateNonconformingOrder(order);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 修改不合格品处理单并替换处理明细。
     * @param request 不合格品处理单和明细请求体，JSON格式，必须包含ncrId
     * @return 操作结果
     */
    @PutMapping("/full")
    public ResultData updateFull(@RequestBody NonconformingOrderFullRequest request) {
        boolean flag = qmsNonconformingOrderService.updateNonconformingOrderFull(request, request.getDetails());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 审批不合格品处理单。
     * @param ncrId 不合格品处理单ID
     * @param request 请求体，支持approvalResult、approvalComment、operatorId参数
     * @return 操作结果
     */
    @PutMapping("/{ncrId}/approve")
    public ResultData approve(@PathVariable("ncrId") Long ncrId, @RequestBody Map<String, Object> request) {
        boolean flag = qmsNonconformingOrderService.approveNonconformingOrder(ncrId, getString(request, "approvalResult"),
                getString(request, "approvalComment"), getLong(request, "operatorId"));
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除不合格品处理单和处理明细。
     * @param ncrId 不合格品处理单ID
     * @return 操作结果
     */
    @DeleteMapping("/{ncrId}")
    public ResultData delete(@PathVariable("ncrId") Long ncrId) {
        boolean flag = qmsNonconformingOrderService.removeNonconformingOrderById(ncrId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出不合格品处理单列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 处理单号关键字，可选
     * @param inspectionId 检验单ID，可选
     * @param productLotId 产品批次ID，可选
     * @param dispositionType 处理方式，可选
     * @param approvalStatus 审批状态，可选
     * @param status 状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long inspectionId,
                      @RequestParam(required = false) Long productLotId,
                      @RequestParam(required = false) String dispositionType,
                      @RequestParam(required = false) String approvalStatus,
                      @RequestParam(required = false) String status) {
        List<QmsNonconformingOrder> orders = qmsNonconformingOrderService.listNonconformingOrders(
                keyword, inspectionId, productLotId, dispositionType, approvalStatus, status);
        excelExportUtil.exportExcel(response, orders, QmsNonconformingOrder.class, "不合格品处理单列表", "不合格品处理单列表");
    }

    /**
     * 从请求体中读取Long类型参数。
     * @param request 请求体Map
     * @param key 参数名
     * @return Long类型参数值
     */
    private Long getLong(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : Long.valueOf(value.toString());
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
     * 不合格品处理单主表和处理明细请求体。
     */
    public static class NonconformingOrderFullRequest extends QmsNonconformingOrder {

        private List<QmsNonconformingDetail> details;

        /**
         * 获取不合格品处理明细列表。
         * @return 不合格品处理明细列表
         */
        public List<QmsNonconformingDetail> getDetails() {
            return details;
        }

        /**
         * 设置不合格品处理明细列表。
         * @param details 不合格品处理明细列表
         */
        public void setDetails(List<QmsNonconformingDetail> details) {
            this.details = details;
        }
    }
}
