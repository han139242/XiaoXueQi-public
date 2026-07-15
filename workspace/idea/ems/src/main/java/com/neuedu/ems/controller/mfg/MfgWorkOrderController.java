package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.service.mfg.MfgWorkOrderService;
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
 * 生产工单管理控制器。
 * 提供生产工单的增删改查、分页查询和导出功能。
 */
@RestController
@RequestMapping("/api/mfg/work-order")
public class MfgWorkOrderController {

    @Autowired
    private MfgWorkOrderService mfgWorkOrderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询生产工单列表，支持关键字搜索、状态筛选、生产计划筛选和产品筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配工单编号），可选
     * @param status 工单状态（新建、已审核、已下达、生产中、已完成、已关闭），可选
     * @param planId 生产计划ID，可选
     * @param productId 产品ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long planId,
                           @RequestParam(required = false) Long productId) {
        Page<MfgWorkOrder> result = mfgWorkOrderService.pageWorkOrders(current, size, keyword, status, planId, productId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据生产工单ID查询生产工单详情。
     * @param workOrderId 生产工单ID
     * @return 生产工单实体信息
     */
    @GetMapping("/{workOrderId}")
    public ResultData getWorkOrder(@PathVariable("workOrderId") Long workOrderId) {
        MfgWorkOrder workOrder = mfgWorkOrderService.getWorkOrderById(workOrderId);
        return ResultData.success(ResultCode.SUCCESS, workOrder);
    }

    /**
     * 新增生产工单。
     * @param workOrder 生产工单实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody MfgWorkOrder workOrder) {
        boolean flag = mfgWorkOrderService.saveWorkOrder(workOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    @PostMapping("/generate/{planId}")
    public ResultData generate(@PathVariable Long planId,
                               @RequestAttribute("userId") Long userId,
                               @RequestBody(required = false) MfgWorkOrder request) {
        return ResultData.success(ResultCode.SUCCESS,
                mfgWorkOrderService.generateWorkOrder(planId, request, userId));
    }

    /**
     * 更新生产工单信息。
     * @param workOrder 生产工单实体（JSON格式，必须包含workOrderId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgWorkOrder workOrder) {
        boolean flag = mfgWorkOrderService.updateWorkOrder(workOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    @PutMapping("/{workOrderId}/release")
    public ResultData release(@PathVariable Long workOrderId,
                              @RequestAttribute("userId") Long userId,
                              @RequestBody(required = false) ReleaseRequest request) {
        return ResultData.success(ResultCode.SUCCESS, mfgWorkOrderService.releaseWorkOrder(
                workOrderId, userId, request == null ? null : request.getRemark()));
    }

    @PutMapping("/{workOrderId}/withdraw")
    public ResultData withdraw(@PathVariable Long workOrderId,
                               @RequestAttribute("userId") Long userId,
                               @RequestBody(required = false) Map<String, Object> request) {
        mfgWorkOrderService.withdrawWorkOrder(workOrderId, userId,
                request == null || request.get("remark") == null ? null : request.get("remark").toString());
        return ResultData.success(ResultCode.SUCCESS);
    }

    /**
     * 删除生产工单（软删除，将删除标志设为1）。
     * @param workOrderId 生产工单ID
     * @return 操作结果
     */
    @DeleteMapping("/{workOrderId}")
    public ResultData delete(@PathVariable("workOrderId") Long workOrderId) {
        boolean flag = mfgWorkOrderService.removeWorkOrderById(workOrderId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出生产工单列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配工单编号），可选
     * @param status 工单状态，可选
     * @param planId 生产计划ID，可选
     * @param productId 产品ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) Long planId,
                      @RequestParam(required = false) Long productId) {
        List<MfgWorkOrder> workOrders = mfgWorkOrderService.listWorkOrders(keyword, status, planId, productId);
        excelExportUtil.exportExcel(response, workOrders, MfgWorkOrder.class, "生产工单列表", "生产工单列表");
    }

    @lombok.Data
    public static class ReleaseRequest {
        private String remark;
    }
}
