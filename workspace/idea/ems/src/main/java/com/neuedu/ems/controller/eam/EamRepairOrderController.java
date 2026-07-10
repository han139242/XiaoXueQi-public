package com.neuedu.ems.controller.eam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.eam.EamRepairOrder;
import com.neuedu.ems.service.eam.EamRepairOrderService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 设备维修单管理控制器。
 */
@RestController
@RequestMapping("/api/eam/repair-order")
public class EamRepairOrderController {

    @Autowired
    private EamRepairOrderService eamRepairOrderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询维修单，支持单号、设备、异常、维修人和状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 关键字，匹配维修单号或故障描述，可选
     * @param equipmentId 设备ID，可选
     * @param exceptionId 生产异常ID，可选
     * @param repairerId 维修人ID，可选
     * @param status 状态，可选
     * @return 维修单分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long equipmentId,
                           @RequestParam(required = false) Long exceptionId,
                           @RequestParam(required = false) Long repairerId,
                           @RequestParam(required = false) String status) {
        Page<EamRepairOrder> result = eamRepairOrderService.pageRepairOrders(
                current, size, keyword, equipmentId, exceptionId, repairerId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据维修单ID查询维修单详情。
     * @param repairId 维修单ID
     * @return 维修单实体信息
     */
    @GetMapping("/{repairId}")
    public ResultData getRepairOrder(@PathVariable("repairId") Long repairId) {
        EamRepairOrder repairOrder = eamRepairOrderService.getRepairOrderById(repairId);
        return ResultData.success(ResultCode.SUCCESS, repairOrder);
    }

    /**
     * 新增维修单。
     * @param repairOrder 维修单实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody EamRepairOrder repairOrder) {
        boolean flag = eamRepairOrderService.saveRepairOrder(repairOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新维修单。
     * @param repairOrder 维修单实体，JSON格式，必须包含repairId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody EamRepairOrder repairOrder) {
        boolean flag = eamRepairOrderService.updateRepairOrder(repairOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 派工维修单并同步设备状态。
     * @param repairId 维修单ID
     * @param request 请求体，支持repairerId和remark参数
     * @return 操作结果
     */
    @PutMapping("/{repairId}/assign")
    public ResultData assign(@PathVariable("repairId") Long repairId,
                             @RequestBody(required = false) Map<String, Object> request) {
        boolean flag = eamRepairOrderService.assignRepairOrder(
                repairId, getLong(request, "repairerId"), getString(request, "remark"));
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 关闭维修单并同步设备状态。
     * @param repairId 维修单ID
     * @param request 请求体，支持repairResult和repairCost参数
     * @return 操作结果
     */
    @PutMapping("/{repairId}/close")
    public ResultData close(@PathVariable("repairId") Long repairId,
                            @RequestBody(required = false) Map<String, Object> request) {
        boolean flag = eamRepairOrderService.closeRepairOrder(
                repairId, getString(request, "repairResult"), getBigDecimal(request, "repairCost"));
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除维修单，采用软删除。
     * @param repairId 维修单ID
     * @return 操作结果
     */
    @DeleteMapping("/{repairId}")
    public ResultData delete(@PathVariable("repairId") Long repairId) {
        boolean flag = eamRepairOrderService.removeRepairOrderById(repairId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出维修单列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字，匹配维修单号或故障描述，可选
     * @param equipmentId 设备ID，可选
     * @param exceptionId 生产异常ID，可选
     * @param repairerId 维修人ID，可选
     * @param status 状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long equipmentId,
                      @RequestParam(required = false) Long exceptionId,
                      @RequestParam(required = false) Long repairerId,
                      @RequestParam(required = false) String status) {
        List<EamRepairOrder> repairOrders = eamRepairOrderService.listRepairOrders(
                keyword, equipmentId, exceptionId, repairerId, status);
        excelExportUtil.exportExcel(response, repairOrders, EamRepairOrder.class, "设备维修单", "设备维修单");
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
     * 从请求体中读取BigDecimal类型参数。
     * @param request 请求体Map
     * @param key 参数名
     * @return BigDecimal类型参数值
     */
    private BigDecimal getBigDecimal(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : new BigDecimal(value.toString());
    }
}
