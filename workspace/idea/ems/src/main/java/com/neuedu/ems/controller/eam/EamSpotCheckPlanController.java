package com.neuedu.ems.controller.eam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.eam.EamSpotCheckPlan;
import com.neuedu.ems.service.eam.EamSpotCheckPlanService;
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
 * 点检计划管理控制器。
 */
@RestController
@RequestMapping("/api/eam/spot-check-plan")
public class EamSpotCheckPlanController {

    @Autowired
    private EamSpotCheckPlanService eamSpotCheckPlanService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询点检计划，支持设备、点检周期、负责岗位和状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param equipmentId 设备ID，可选
     * @param checkCycle 点检周期，可选
     * @param responsibleRole 负责岗位，可选
     * @param status 状态，可选
     * @return 点检计划分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long equipmentId,
                           @RequestParam(required = false) String checkCycle,
                           @RequestParam(required = false) String responsibleRole,
                           @RequestParam(required = false) String status) {
        Page<EamSpotCheckPlan> result = eamSpotCheckPlanService.pageSpotCheckPlans(
                current, size, equipmentId, checkCycle, responsibleRole, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据点检计划ID查询点检计划详情。
     * @param planId 点检计划ID
     * @return 点检计划实体信息
     */
    @GetMapping("/{planId}")
    public ResultData getSpotCheckPlan(@PathVariable("planId") Long planId) {
        EamSpotCheckPlan spotCheckPlan = eamSpotCheckPlanService.getSpotCheckPlanById(planId);
        return ResultData.success(ResultCode.SUCCESS, spotCheckPlan);
    }

    /**
     * 新增点检计划。
     * @param spotCheckPlan 点检计划实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody EamSpotCheckPlan spotCheckPlan) {
        boolean flag = eamSpotCheckPlanService.saveSpotCheckPlan(spotCheckPlan);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新点检计划。
     * @param spotCheckPlan 点检计划实体，JSON格式，必须包含planId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody EamSpotCheckPlan spotCheckPlan) {
        boolean flag = eamSpotCheckPlanService.updateSpotCheckPlan(spotCheckPlan);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除点检计划，采用软删除。
     * @param planId 点检计划ID
     * @return 操作结果
     */
    @DeleteMapping("/{planId}")
    public ResultData delete(@PathVariable("planId") Long planId) {
        boolean flag = eamSpotCheckPlanService.removeSpotCheckPlanById(planId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出点检计划列表为Excel文件。
     * @param response HTTP响应对象
     * @param equipmentId 设备ID，可选
     * @param checkCycle 点检周期，可选
     * @param responsibleRole 负责岗位，可选
     * @param status 状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long equipmentId,
                      @RequestParam(required = false) String checkCycle,
                      @RequestParam(required = false) String responsibleRole,
                      @RequestParam(required = false) String status) {
        List<EamSpotCheckPlan> spotCheckPlans = eamSpotCheckPlanService.listSpotCheckPlans(
                equipmentId, checkCycle, responsibleRole, status);
        excelExportUtil.exportExcel(response, spotCheckPlans, EamSpotCheckPlan.class, "点检计划", "点检计划");
    }
}
