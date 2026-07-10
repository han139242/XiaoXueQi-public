package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.WmsOutboundDetail;
import com.neuedu.ems.entity.WmsOutboundOrder;
import com.neuedu.ems.service.WmsOutboundOrderService;
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
 * 出库单管理控制器。
 */
@RestController
@RequestMapping("/api/wms/outbound-order")
public class WmsOutboundOrderController {

    @Autowired
    private WmsOutboundOrderService wmsOutboundOrderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询出库单列表，支持单号、类型、仓库、工单、申请人和审核状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 关键字，匹配出库单号，可选
     * @param outboundType 出库类型，可选
     * @param warehouseId 仓库ID，可选
     * @param workOrderId 工单ID，可选
     * @param applicantId 申请人ID，可选
     * @param auditStatus 审核状态，可选
     * @return 出库单分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String outboundType,
                           @RequestParam(required = false) Long warehouseId,
                           @RequestParam(required = false) Long workOrderId,
                           @RequestParam(required = false) Long applicantId,
                           @RequestParam(required = false) String auditStatus) {
        Page<WmsOutboundOrder> result = wmsOutboundOrderService.pageOutboundOrders(
                current, size, keyword, outboundType, warehouseId, workOrderId, applicantId, auditStatus);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据出库单ID查询详情。
     * @param outboundId 出库单ID
     * @return 出库单实体
     */
    @GetMapping("/{outboundId}")
    public ResultData getOutboundOrder(@PathVariable("outboundId") Long outboundId) {
        WmsOutboundOrder outboundOrder = wmsOutboundOrderService.getOutboundOrderById(outboundId);
        return ResultData.success(ResultCode.SUCCESS, outboundOrder);
    }

    /**
     * 新增出库单；如果请求体包含details，则同步保存出库明细。
     * @param request 出库单请求体，JSON格式，可包含details
     * @return 操作结果或出库单ID
     */
    @PostMapping
    public ResultData save(@RequestBody OutboundOrderFullRequest request) {
        if (request.getDetails() != null) {
            Long outboundId = wmsOutboundOrderService.saveOutboundOrderFull(request, request.getDetails());
            return ResultData.success(ResultCode.SUCCESS, Map.of("outboundId", outboundId));
        }
        boolean flag = wmsOutboundOrderService.saveOutboundOrder(request);
        return flag ? ResultData.success(ResultCode.SUCCESS, Map.of("outboundId", request.getOutboundId())) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 新增出库单并保存出库明细。
     * @param request 出库单和明细请求体，JSON格式，必须包含details
     * @return 出库单ID
     */
    @PostMapping("/full")
    public ResultData saveFull(@RequestBody OutboundOrderFullRequest request) {
        Long outboundId = wmsOutboundOrderService.saveOutboundOrderFull(request, request.getDetails());
        return ResultData.success(ResultCode.SUCCESS, Map.of("outboundId", outboundId));
    }

    /**
     * 更新出库单信息，已确认出库单不允许普通修改。
     * @param outboundOrder 出库单实体，JSON格式，必须包含outboundId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsOutboundOrder outboundOrder) {
        boolean flag = wmsOutboundOrderService.updateOutboundOrder(outboundOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新出库单并替换出库明细，已确认出库单不允许替换明细。
     * @param request 出库单和明细请求体，JSON格式，必须包含outboundId
     * @return 操作结果
     */
    @PutMapping("/full")
    public ResultData updateFull(@RequestBody OutboundOrderFullRequest request) {
        boolean flag = wmsOutboundOrderService.updateOutboundOrderFull(request, request.getDetails());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 确认出库，扣减库存余额并写入库存流水。
     * @param outboundId 出库单ID
     * @param request 请求体，支持operatorId和remark参数
     * @return 操作结果
     */
    @PutMapping("/{outboundId}/confirm")
    public ResultData confirm(@PathVariable("outboundId") Long outboundId,
                              @RequestBody(required = false) Map<String, Object> request) {
        boolean flag = wmsOutboundOrderService.confirmOutboundOrder(outboundId, getLong(request, "operatorId"), getString(request, "remark"));
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除出库单和出库明细，已确认出库单不允许删除。
     * @param outboundId 出库单ID
     * @return 操作结果
     */
    @DeleteMapping("/{outboundId}")
    public ResultData delete(@PathVariable("outboundId") Long outboundId) {
        boolean flag = wmsOutboundOrderService.removeOutboundOrderById(outboundId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出出库单列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字，匹配出库单号，可选
     * @param outboundType 出库类型，可选
     * @param warehouseId 仓库ID，可选
     * @param workOrderId 工单ID，可选
     * @param applicantId 申请人ID，可选
     * @param auditStatus 审核状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String outboundType,
                      @RequestParam(required = false) Long warehouseId,
                      @RequestParam(required = false) Long workOrderId,
                      @RequestParam(required = false) Long applicantId,
                      @RequestParam(required = false) String auditStatus) {
        List<WmsOutboundOrder> outboundOrders = wmsOutboundOrderService.listOutboundOrders(
                keyword, outboundType, warehouseId, workOrderId, applicantId, auditStatus);
        excelExportUtil.exportExcel(response, outboundOrders, WmsOutboundOrder.class, "出库单列表", "出库单列表");
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
     * 出库单主表和明细请求体。
     */
    public static class OutboundOrderFullRequest extends WmsOutboundOrder {

        private List<WmsOutboundDetail> details;

        /**
         * 获取出库明细列表。
         * @return 出库明细列表
         */
        public List<WmsOutboundDetail> getDetails() {
            return details;
        }

        /**
         * 设置出库明细列表。
         * @param details 出库明细列表
         */
        public void setDetails(List<WmsOutboundDetail> details) {
            this.details = details;
        }
    }
}
