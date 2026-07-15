package com.neuedu.ems.controller.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.wms.WmsReturnDetail;
import com.neuedu.ems.entity.wms.WmsReturnOrder;
import com.neuedu.ems.service.wms.WmsReturnOrderService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 退料单管理控制器。
 */
@RestController
@RequestMapping("/api/wms/return-order")
public class WmsReturnOrderController {

    @Autowired
    private WmsReturnOrderService wmsReturnOrderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询退料单列表，支持单号、类型、工单、申请人和审核状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 关键字，匹配退料单号，可选
     * @param returnType 退料类型，可选
     * @param workOrderId 工单ID，可选
     * @param applicantId 申请人ID，可选
     * @param auditStatus 审核状态，可选
     * @return 退料单分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String returnType,
                           @RequestParam(required = false) Long workOrderId,
                           @RequestParam(required = false) Long applicantId,
                           @RequestParam(required = false) String auditStatus) {
        Page<WmsReturnOrder> result = wmsReturnOrderService.pageReturnOrders(
                current, size, keyword, returnType, workOrderId, applicantId, auditStatus);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据退料单ID查询详情。
     * @param returnId 退料单ID
     * @return 退料单实体
     */
    @GetMapping("/{returnId}")
    public ResultData getReturnOrder(@PathVariable("returnId") Long returnId) {
        WmsReturnOrder returnOrder = wmsReturnOrderService.getReturnOrderById(returnId);
        return ResultData.success(ResultCode.SUCCESS, returnOrder);
    }

    /**
     * 新增退料单；如果请求体包含details，则同步保存退料明细。
     * @param request 退料单请求体，JSON格式，可包含details
     * @return 操作结果或退料单ID
     */
    @PostMapping
    public ResultData save(@RequestBody ReturnOrderFullRequest request) {
        if (request.getDetails() != null) {
            Long returnId = wmsReturnOrderService.saveReturnOrderFull(request, request.getDetails());
            return ResultData.success(ResultCode.SUCCESS, Map.of("returnId", returnId));
        }
        boolean flag = wmsReturnOrderService.saveReturnOrder(request);
        return flag ? ResultData.success(ResultCode.SUCCESS, Map.of("returnId", request.getReturnId())) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 新增退料单并保存退料明细。
     * @param request 退料单和明细请求体，JSON格式，必须包含details
     * @return 退料单ID
     */
    @PostMapping("/full")
    public ResultData saveFull(@RequestBody ReturnOrderFullRequest request) {
        Long returnId = wmsReturnOrderService.saveReturnOrderFull(request, request.getDetails());
        return ResultData.success(ResultCode.SUCCESS, Map.of("returnId", returnId));
    }

    /**
     * 更新退料单信息，已确认退料单不允许普通修改。
     * @param returnOrder 退料单实体，JSON格式，必须包含returnId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsReturnOrder returnOrder) {
        boolean flag = wmsReturnOrderService.updateReturnOrder(returnOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新退料单并替换退料明细，已确认退料单不允许替换明细。
     * @param request 退料单和明细请求体，JSON格式，必须包含returnId
     * @return 操作结果
     */
    @PutMapping("/full")
    public ResultData updateFull(@RequestBody ReturnOrderFullRequest request) {
        boolean flag = wmsReturnOrderService.updateReturnOrderFull(request, request.getDetails());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 确认退料入库，回加库存余额并写入库存流水。
     * @param returnId 退料单ID
     * @param userId JWT 中的当前用户ID
     * @param request 请求体，支持remark参数
     * @return 操作结果
     */
    @PutMapping("/{returnId}/confirm")
    public ResultData confirm(@PathVariable("returnId") Long returnId,
                              @RequestAttribute("userId") Long userId,
                              @RequestBody(required = false) Map<String, Object> request) {
        boolean flag = wmsReturnOrderService.confirmReturnOrder(returnId, userId, getString(request, "remark"));
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    @PutMapping("/{returnId}/withdraw")
    public ResultData withdraw(@PathVariable Long returnId, @RequestAttribute("userId") Long userId,
                               @RequestBody(required = false) Map<String, Object> request) {
        wmsReturnOrderService.withdrawReturnOrder(returnId, userId, getString(request, "remark"));
        return ResultData.success(ResultCode.SUCCESS);
    }

    /**
     * 软删除退料单和退料明细，已确认退料单不允许删除。
     * @param returnId 退料单ID
     * @return 操作结果
     */
    @DeleteMapping("/{returnId}")
    public ResultData delete(@PathVariable("returnId") Long returnId) {
        boolean flag = wmsReturnOrderService.removeReturnOrderById(returnId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出退料单列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字，匹配退料单号，可选
     * @param returnType 退料类型，可选
     * @param workOrderId 工单ID，可选
     * @param applicantId 申请人ID，可选
     * @param auditStatus 审核状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String returnType,
                      @RequestParam(required = false) Long workOrderId,
                      @RequestParam(required = false) Long applicantId,
                      @RequestParam(required = false) String auditStatus) {
        List<WmsReturnOrder> returnOrders = wmsReturnOrderService.listReturnOrders(
                keyword, returnType, workOrderId, applicantId, auditStatus);
        excelExportUtil.exportExcel(response, returnOrders, WmsReturnOrder.class, "退料单列表", "退料单列表");
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
     * 退料单主表和明细请求体。
     */
    public static class ReturnOrderFullRequest extends WmsReturnOrder {

        private List<WmsReturnDetail> details;

        /**
         * 获取退料明细列表。
         * @return 退料明细列表
         */
        public List<WmsReturnDetail> getDetails() {
            return details;
        }

        /**
         * 设置退料明细列表。
         * @param details 退料明细列表
         */
        public void setDetails(List<WmsReturnDetail> details) {
            this.details = details;
        }
    }
}
