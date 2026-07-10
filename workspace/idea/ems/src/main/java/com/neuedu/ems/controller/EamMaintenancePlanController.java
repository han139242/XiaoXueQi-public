package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.EamMaintenancePlan;
import com.neuedu.ems.service.EamMaintenancePlanService;
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
 * 保养计划管理控制器。
 */
@RestController
@RequestMapping("/api/eam/maintenance-plan")
public class EamMaintenancePlanController {

    @Autowired
    private EamMaintenancePlanService eamMaintenancePlanService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询保养计划，支持设备、保养类型、周期类型、负责人和状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param equipmentId 设备ID，可选
     * @param maintenanceType 保养类型，可选
     * @param cycleType 周期类型，可选
     * @param responsibleId 负责人ID，可选
     * @param status 状态，可选
     * @return 保养计划分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long equipmentId,
                           @RequestParam(required = false) String maintenanceType,
                           @RequestParam(required = false) String cycleType,
                           @RequestParam(required = false) Long responsibleId,
                           @RequestParam(required = false) String status) {
        Page<EamMaintenancePlan> result = eamMaintenancePlanService.pageMaintenancePlans(
                current, size, equipmentId, maintenanceType, cycleType, responsibleId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据保养计划ID查询保养计划详情。
     * @param planId 保养计划ID
     * @return 保养计划实体信息
     */
    @GetMapping("/{planId}")
    public ResultData getMaintenancePlan(@PathVariable("planId") Long planId) {
        EamMaintenancePlan maintenancePlan = eamMaintenancePlanService.getMaintenancePlanById(planId);
        return ResultData.success(ResultCode.SUCCESS, maintenancePlan);
    }

    /**
     * 新增保养计划。
     * @param maintenancePlan 保养计划实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody EamMaintenancePlan maintenancePlan) {
        boolean flag = eamMaintenancePlanService.saveMaintenancePlan(maintenancePlan);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新保养计划。
     * @param maintenancePlan 保养计划实体，JSON格式，必须包含planId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody EamMaintenancePlan maintenancePlan) {
        boolean flag = eamMaintenancePlanService.updateMaintenancePlan(maintenancePlan);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除保养计划，采用软删除。
     * @param planId 保养计划ID
     * @return 操作结果
     */
    @DeleteMapping("/{planId}")
    public ResultData delete(@PathVariable("planId") Long planId) {
        boolean flag = eamMaintenancePlanService.removeMaintenancePlanById(planId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出保养计划列表为Excel文件。
     * @param response HTTP响应对象
     * @param equipmentId 设备ID，可选
     * @param maintenanceType 保养类型，可选
     * @param cycleType 周期类型，可选
     * @param responsibleId 负责人ID，可选
     * @param status 状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long equipmentId,
                      @RequestParam(required = false) String maintenanceType,
                      @RequestParam(required = false) String cycleType,
                      @RequestParam(required = false) Long responsibleId,
                      @RequestParam(required = false) String status) {
        List<EamMaintenancePlan> maintenancePlans = eamMaintenancePlanService.listMaintenancePlans(
                equipmentId, maintenanceType, cycleType, responsibleId, status);
        excelExportUtil.exportExcel(response, maintenancePlans, EamMaintenancePlan.class, "保养计划", "保养计划");
    }
}
