package com.neuedu.ems.controller.dashboard;

import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.service.dashboard.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 首页看板控制器。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 查询首页统计汇总。
     * @param beginDate 开始日期，可选
     * @param endDate 结束日期，可选
     * @return 看板汇总数据
     */
    @GetMapping("/summary")
    public ResultData summary(@RequestParam(required = false) LocalDate beginDate,
                              @RequestParam(required = false) LocalDate endDate) {
        return ResultData.success(ResultCode.SUCCESS, dashboardService.getSummary(beginDate, endDate));
    }

    /**
     * 查询生产趋势。
     * @param beginDate 开始日期，可选
     * @param endDate 结束日期，可选
     * @return 趋势数据列表
     */
    @GetMapping("/production-trend")
    public ResultData productionTrend(@RequestParam(required = false) LocalDate beginDate,
                                      @RequestParam(required = false) LocalDate endDate) {
        return ResultData.success(ResultCode.SUCCESS, dashboardService.getProductionTrend(beginDate, endDate));
    }

    /**
     * 查询质量统计。
     * @param beginDate 开始日期，可选
     * @param endDate 结束日期，可选
     * @return 质量统计数据
     */
    @GetMapping("/quality-statistics")
    public ResultData qualityStatistics(@RequestParam(required = false) LocalDate beginDate,
                                        @RequestParam(required = false) LocalDate endDate) {
        return ResultData.success(ResultCode.SUCCESS, dashboardService.getQualityStatistics(beginDate, endDate));
    }

    /**
     * 查询设备状态统计。
     * @return 设备状态统计数据
     */
    @GetMapping("/equipment-status")
    public ResultData equipmentStatus() {
        return ResultData.success(ResultCode.SUCCESS, dashboardService.getEquipmentStatus());
    }
}
