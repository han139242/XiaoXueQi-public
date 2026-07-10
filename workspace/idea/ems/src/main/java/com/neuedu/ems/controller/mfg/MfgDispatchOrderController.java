package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.service.mfg.MfgDispatchOrderService;
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
 * 派工单管理控制器。
 * 提供派工单的增删改查、分页查询和导出功能。
 */
@RestController
@RequestMapping("/api/mfg/dispatch-order")
public class MfgDispatchOrderController {

    @Autowired
    private MfgDispatchOrderService mfgDispatchOrderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询派工单列表，支持关键字搜索、状态筛选、工单筛选和班组筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配派工单编号），可选
     * @param status 派工单状态（待派发、已派发、已接受、生产中、已完成、已驳回），可选
     * @param workOrderId 工单ID，可选
     * @param teamId 班组ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long workOrderId,
                           @RequestParam(required = false) Long teamId) {
        Page<MfgDispatchOrder> result = mfgDispatchOrderService.pageDispatchOrders(current, size, keyword, status, workOrderId, teamId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据派工单ID查询派工单详情。
     * @param dispatchId 派工单ID
     * @return 派工单实体信息
     */
    @GetMapping("/{dispatchId}")
    public ResultData getDispatchOrder(@PathVariable("dispatchId") Long dispatchId) {
        MfgDispatchOrder dispatchOrder = mfgDispatchOrderService.getDispatchOrderById(dispatchId);
        return ResultData.success(ResultCode.SUCCESS, dispatchOrder);
    }

    /**
     * 新增派工单。
     * @param dispatchOrder 派工单实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody MfgDispatchOrder dispatchOrder) {
        boolean flag = mfgDispatchOrderService.saveDispatchOrder(dispatchOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新派工单信息。
     * @param dispatchOrder 派工单实体（JSON格式，必须包含dispatchId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgDispatchOrder dispatchOrder) {
        boolean flag = mfgDispatchOrderService.updateDispatchOrder(dispatchOrder);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除派工单（软删除，将删除标志设为1）。
     * @param dispatchId 派工单ID
     * @return 操作结果
     */
    @DeleteMapping("/{dispatchId}")
    public ResultData delete(@PathVariable("dispatchId") Long dispatchId) {
        boolean flag = mfgDispatchOrderService.removeDispatchOrderById(dispatchId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出派工单列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配派工单编号），可选
     * @param status 派工单状态，可选
     * @param workOrderId 工单ID，可选
     * @param teamId 班组ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) Long workOrderId,
                      @RequestParam(required = false) Long teamId) {
        List<MfgDispatchOrder> dispatchOrders = mfgDispatchOrderService.listDispatchOrders(keyword, status, workOrderId, teamId);
        excelExportUtil.exportExcel(response, dispatchOrders, MfgDispatchOrder.class, "派工单列表", "派工单列表");
    }
}
