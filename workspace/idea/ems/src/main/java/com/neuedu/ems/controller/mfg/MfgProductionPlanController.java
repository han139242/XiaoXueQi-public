package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.service.mfg.MfgProductionPlanService;
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
 * 生产计划管理控制器。
 * 提供生产计划的增删改查、分页查询和导出功能。
 */
@RestController
@RequestMapping("/api/mfg/production-plan")
public class MfgProductionPlanController {

    @Autowired
    private MfgProductionPlanService mfgProductionPlanService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询生产计划列表，支持关键字搜索、状态筛选、生产订单筛选和产线筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配计划编号），可选
     * @param status 计划状态（待生效、已生效、已完成、已取消），可选
     * @param productionOrderId 生产订单ID，可选
     * @param lineId 产线ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long productionOrderId,
                           @RequestParam(required = false) Long lineId) {
        Page<MfgProductionPlan> result = mfgProductionPlanService.pagePlans(current, size, keyword, status, productionOrderId, lineId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据生产计划ID查询生产计划详情。
     * @param planId 生产计划ID
     * @return 生产计划实体信息
     */
    @GetMapping("/{planId}")
    public ResultData getPlan(@PathVariable("planId") Long planId) {
        MfgProductionPlan plan = mfgProductionPlanService.getPlanById(planId);
        return ResultData.success(ResultCode.SUCCESS, plan);
    }

    /**
     * 新增生产计划。
     * @param plan 生产计划实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody MfgProductionPlan plan) {
        boolean flag = mfgProductionPlanService.savePlan(plan);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新生产计划信息。
     * @param plan 生产计划实体（JSON格式，必须包含planId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgProductionPlan plan) {
        boolean flag = mfgProductionPlanService.updatePlan(plan);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除生产计划（软删除，将删除标志设为1）。
     * @param planId 生产计划ID
     * @return 操作结果
     */
    @DeleteMapping("/{planId}")
    public ResultData delete(@PathVariable("planId") Long planId) {
        boolean flag = mfgProductionPlanService.removePlanById(planId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出生产计划列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配计划编号），可选
     * @param status 计划状态，可选
     * @param productionOrderId 生产订单ID，可选
     * @param lineId 产线ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) Long productionOrderId,
                      @RequestParam(required = false) Long lineId) {
        List<MfgProductionPlan> plans = mfgProductionPlanService.listPlans(keyword, status, productionOrderId, lineId);
        excelExportUtil.exportExcel(response, plans, MfgProductionPlan.class, "生产计划列表", "生产计划列表");
    }
}
