package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.service.mfg.MfgProductionOrderService;
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
 * 生产订单管理控制器。
 * 提供生产订单的增删改查、分页查询和导出功能。
 */
@RestController
@RequestMapping("/api/mfg/production-order")
public class MfgProductionOrderController {

    @Autowired
    private MfgProductionOrderService mfgProductionOrderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询生产订单列表，支持关键字搜索、状态筛选、客户筛选和产品筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配订单编号），可选
     * @param status 订单状态（新建、已排产、生产中、已完成、已关闭），可选
     * @param customerId 客户ID，可选
     * @param productId 产品ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long customerId,
                           @RequestParam(required = false) Long productId) {
        Page<MfgProductionOrder> result = mfgProductionOrderService.pageOrders(current, size, keyword, status, customerId, productId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据生产订单ID查询生产订单详情。
     * @param productionOrderId 生产订单ID
     * @return 生产订单实体信息
     */
    @GetMapping("/{productionOrderId}")
    public ResultData getOrder(@PathVariable("productionOrderId") Long productionOrderId) {
        MfgProductionOrder order = mfgProductionOrderService.getOrderById(productionOrderId);
        return ResultData.success(ResultCode.SUCCESS, order);
    }

    /**
     * 新增生产订单。
     * @param order 生产订单实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody MfgProductionOrder order) {
        boolean flag = mfgProductionOrderService.saveOrder(order);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新生产订单信息。
     * @param order 生产订单实体（JSON格式，必须包含productionOrderId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgProductionOrder order) {
        boolean flag = mfgProductionOrderService.updateOrder(order);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除生产订单（软删除，将删除标志设为1）。
     * @param productionOrderId 生产订单ID
     * @return 操作结果
     */
    @DeleteMapping("/{productionOrderId}")
    public ResultData delete(@PathVariable("productionOrderId") Long productionOrderId) {
        boolean flag = mfgProductionOrderService.removeOrderById(productionOrderId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出生产订单列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配订单编号），可选
     * @param status 订单状态，可选
     * @param customerId 客户ID，可选
     * @param productId 产品ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) Long customerId,
                      @RequestParam(required = false) Long productId) {
        List<MfgProductionOrder> orders = mfgProductionOrderService.listOrders(keyword, status, customerId, productId);
        excelExportUtil.exportExcel(response, orders, MfgProductionOrder.class, "生产订单列表", "生产订单列表");
    }
}
