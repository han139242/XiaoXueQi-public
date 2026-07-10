package com.neuedu.ems.controller.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.wms.WmsStocktakeDetail;
import com.neuedu.ems.entity.wms.WmsStocktakeOrder;
import com.neuedu.ems.service.wms.WmsStocktakeOrderService;
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
 * 盘点单管理控制器。
 */
@RestController
@RequestMapping("/api/wms/stocktake-order")
public class WmsStocktakeOrderController {

    @Autowired
    private WmsStocktakeOrderService wmsStocktakeOrderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询盘点单列表，支持单号、仓库、盘点类型和状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 关键字，匹配盘点单号，可选
     * @param warehouseId 仓库ID，可选
     * @param stocktakeType 盘点类型，可选
     * @param stocktakeStatus 盘点状态，可选
     * @return 盘点单分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long warehouseId,
                           @RequestParam(required = false) String stocktakeType,
                           @RequestParam(required = false) String stocktakeStatus) {
        Page<WmsStocktakeOrder> result = wmsStocktakeOrderService.pageStocktakeOrders(
                current, size, keyword, warehouseId, stocktakeType, stocktakeStatus);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据盘点单ID查询详情。
     * @param stocktakeId 盘点单ID
     * @return 盘点单实体
     */
    @GetMapping("/{stocktakeId}")
    public ResultData getStocktakeOrder(@PathVariable("stocktakeId") Long stocktakeId) {
        WmsStocktakeOrder stocktakeOrder = wmsStocktakeOrderService.getStocktakeOrderById(stocktakeId);
        return ResultData.success(ResultCode.SUCCESS, stocktakeOrder);
    }

    /**
     * 新增盘点单；如果请求体包含details，则同步保存盘点明细。
     * @param request 盘点单请求体，JSON格式，可包含details
     * @return 操作结果或盘点单ID
     */
    @PostMapping
    public ResultData save(@RequestBody StocktakeOrderFullRequest request) {
        if (request.getDetails() != null) {
            Long stocktakeId = wmsStocktakeOrderService.saveStocktakeOrderFull(request, request.getDetails());
            return ResultData.success(ResultCode.SUCCESS, Map.of("stocktakeId", stocktakeId));
        }
        boolean flag = wmsStocktakeOrderService.saveStocktakeOrder(request);
        return flag ? ResultData.success(ResultCode.SUCCESS, Map.of("stocktakeId", request.getStocktakeId())) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 新增盘点单并保存盘点明细。
     * @param request 盘点单和明细请求体，JSON格式，必须包含details
     * @return 盘点单ID
     */
    @PostMapping("/full")
    public ResultData saveFull(@RequestBody StocktakeOrderFullRequest request) {
        Long stocktakeId = wmsStocktakeOrderService.saveStocktakeOrderFull(request, request.getDetails());
        return ResultData.success(ResultCode.SUCCESS, Map.of("stocktakeId", stocktakeId));
    }

    /**
     * 更新盘点单信息，已完成盘点单不允许普通修改。
     * @param stocktakeOrder 盘点单实体，JSON格式，必须包含stocktakeId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsStocktakeOrder stocktakeOrder) {
        boolean flag = wmsStocktakeOrderService.updateStocktakeOrder(stocktakeOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新盘点单并替换盘点明细，已完成盘点单不允许替换明细。
     * @param request 盘点单和明细请求体，JSON格式，必须包含stocktakeId
     * @return 操作结果
     */
    @PutMapping("/full")
    public ResultData updateFull(@RequestBody StocktakeOrderFullRequest request) {
        boolean flag = wmsStocktakeOrderService.updateStocktakeOrderFull(request, request.getDetails());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 完成盘点，根据盘点差异调整库存并写入库存流水。
     * @param stocktakeId 盘点单ID
     * @param request 请求体，支持operatorId和remark参数
     * @return 操作结果
     */
    @PutMapping("/{stocktakeId}/finish")
    public ResultData finish(@PathVariable("stocktakeId") Long stocktakeId,
                             @RequestBody(required = false) Map<String, Object> request) {
        boolean flag = wmsStocktakeOrderService.finishStocktakeOrder(stocktakeId, getLong(request, "operatorId"), getString(request, "remark"));
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除盘点单和盘点明细，已完成盘点单不允许删除。
     * @param stocktakeId 盘点单ID
     * @return 操作结果
     */
    @DeleteMapping("/{stocktakeId}")
    public ResultData delete(@PathVariable("stocktakeId") Long stocktakeId) {
        boolean flag = wmsStocktakeOrderService.removeStocktakeOrderById(stocktakeId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出盘点单列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字，匹配盘点单号，可选
     * @param warehouseId 仓库ID，可选
     * @param stocktakeType 盘点类型，可选
     * @param stocktakeStatus 盘点状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long warehouseId,
                      @RequestParam(required = false) String stocktakeType,
                      @RequestParam(required = false) String stocktakeStatus) {
        List<WmsStocktakeOrder> stocktakeOrders = wmsStocktakeOrderService.listStocktakeOrders(
                keyword, warehouseId, stocktakeType, stocktakeStatus);
        excelExportUtil.exportExcel(response, stocktakeOrders, WmsStocktakeOrder.class, "盘点单列表", "盘点单列表");
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
     * 盘点单主表和明细请求体。
     */
    public static class StocktakeOrderFullRequest extends WmsStocktakeOrder {

        private List<WmsStocktakeDetail> details;

        /**
         * 获取盘点明细列表。
         * @return 盘点明细列表
         */
        public List<WmsStocktakeDetail> getDetails() {
            return details;
        }

        /**
         * 设置盘点明细列表。
         * @param details 盘点明细列表
         */
        public void setDetails(List<WmsStocktakeDetail> details) {
            this.details = details;
        }
    }
}
