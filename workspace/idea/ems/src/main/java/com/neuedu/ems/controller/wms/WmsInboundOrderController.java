package com.neuedu.ems.controller.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.wms.WmsInboundDetail;
import com.neuedu.ems.entity.wms.WmsInboundOrder;
import com.neuedu.ems.service.wms.WmsInboundOrderService;
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
 * 入库单管理控制器。
 */
@RestController
@RequestMapping("/api/wms/inbound-order")
public class WmsInboundOrderController {

    @Autowired
    private WmsInboundOrderService wmsInboundOrderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询入库单列表，支持入库单号、类型、仓库、来源单据和审核状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 关键字，匹配入库单号，可选
     * @param inboundType 入库类型，可选
     * @param warehouseId 仓库ID，可选
     * @param sourceOrderType 来源单据类型，可选
     * @param sourceOrderId 来源单据ID，可选
     * @param auditStatus 审核状态，可选
     * @return 入库单分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String inboundType,
                           @RequestParam(required = false) Long warehouseId,
                           @RequestParam(required = false) String sourceOrderType,
                           @RequestParam(required = false) Long sourceOrderId,
                           @RequestParam(required = false) String auditStatus) {
        Page<WmsInboundOrder> result = wmsInboundOrderService.pageInboundOrders(
                current, size, keyword, inboundType, warehouseId, sourceOrderType, sourceOrderId, auditStatus);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据入库单ID查询入库单详情。
     * @param inboundId 入库单ID
     * @return 入库单实体信息
     */
    @GetMapping("/{inboundId}")
    public ResultData getInboundOrder(@PathVariable("inboundId") Long inboundId) {
        WmsInboundOrder inboundOrder = wmsInboundOrderService.getInboundOrderById(inboundId);
        return ResultData.success(ResultCode.SUCCESS, inboundOrder);
    }

    /**
     * 新增入库单；如果请求体包含details，则同步保存入库明细。
     * @param request 入库单请求体，JSON格式，可包含details
     * @return 操作结果或入库单ID
     */
    @PostMapping
    public ResultData save(@RequestBody InboundOrderFullRequest request) {
        if (request.getDetails() != null) {
            Long inboundId = wmsInboundOrderService.saveInboundOrderFull(request, request.getDetails());
            return ResultData.success(ResultCode.SUCCESS, Map.of("inboundId", inboundId));
        }
        boolean flag = wmsInboundOrderService.saveInboundOrder(request);
        return flag ? ResultData.success(ResultCode.SUCCESS, Map.of("inboundId", request.getInboundId())) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 新增入库单并保存入库明细。
     * @param request 入库单和明细请求体，JSON格式，必须包含details
     * @return 入库单ID
     */
    @PostMapping("/full")
    public ResultData saveFull(@RequestBody InboundOrderFullRequest request) {
        Long inboundId = wmsInboundOrderService.saveInboundOrderFull(request, request.getDetails());
        return ResultData.success(ResultCode.SUCCESS, Map.of("inboundId", inboundId));
    }

    /**
     * 更新入库单信息，已确认入库单不允许普通修改。
     * @param inboundOrder 入库单实体，JSON格式，必须包含inboundId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsInboundOrder inboundOrder) {
        boolean flag = wmsInboundOrderService.updateInboundOrder(inboundOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新入库单并替换入库明细，已确认入库单不允许替换明细。
     * @param request 入库单和明细请求体，JSON格式，必须包含inboundId
     * @return 操作结果
     */
    @PutMapping("/full")
    public ResultData updateFull(@RequestBody InboundOrderFullRequest request) {
        boolean flag = wmsInboundOrderService.updateInboundOrderFull(request, request.getDetails());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 确认入库，更新库存余额并写入库存流水。
     * @param inboundId 入库单ID
     * @param request 请求体，支持remark参数
     * @param userId JWT 中的当前用户ID
     * @return 操作结果
     */
    @PutMapping("/{inboundId}/confirm")
    public ResultData confirm(@PathVariable("inboundId") Long inboundId,
                              @RequestAttribute("userId") Long userId,
                              @RequestBody(required = false) Map<String, Object> request) {
        boolean flag = wmsInboundOrderService.confirmInboundOrder(inboundId, userId, getString(request, "remark"));
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    @PutMapping("/{inboundId}/withdraw")
    public ResultData withdraw(@PathVariable Long inboundId, @RequestAttribute("userId") Long userId,
                               @RequestBody(required = false) Map<String, Object> request) {
        wmsInboundOrderService.withdrawInboundOrder(inboundId, userId, getString(request, "remark"));
        return ResultData.success(ResultCode.SUCCESS);
    }

    /**
     * 删除入库单和入库明细，采用软删除，已确认入库单不允许删除。
     * @param inboundId 入库单ID
     * @return 操作结果
     */
    @DeleteMapping("/{inboundId}")
    public ResultData delete(@PathVariable("inboundId") Long inboundId) {
        boolean flag = wmsInboundOrderService.removeInboundOrderById(inboundId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出入库单列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字，匹配入库单号，可选
     * @param inboundType 入库类型，可选
     * @param warehouseId 仓库ID，可选
     * @param sourceOrderType 来源单据类型，可选
     * @param sourceOrderId 来源单据ID，可选
     * @param auditStatus 审核状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String inboundType,
                      @RequestParam(required = false) Long warehouseId,
                      @RequestParam(required = false) String sourceOrderType,
                      @RequestParam(required = false) Long sourceOrderId,
                      @RequestParam(required = false) String auditStatus) {
        List<WmsInboundOrder> inboundOrders = wmsInboundOrderService.listInboundOrders(
                keyword, inboundType, warehouseId, sourceOrderType, sourceOrderId, auditStatus);
        excelExportUtil.exportExcel(response, inboundOrders, WmsInboundOrder.class, "入库单列表", "入库单列表");
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
     * 入库单主表和明细请求体。
     */
    public static class InboundOrderFullRequest extends WmsInboundOrder {

        private List<WmsInboundDetail> details;

        /**
         * 获取入库明细列表。
         * @return 入库明细列表
         */
        public List<WmsInboundDetail> getDetails() {
            return details;
        }

        /**
         * 设置入库明细列表。
         * @param details 入库明细列表
         */
        public void setDetails(List<WmsInboundDetail> details) {
            this.details = details;
        }
    }
}
