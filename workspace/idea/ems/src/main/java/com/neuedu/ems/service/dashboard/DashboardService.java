package com.neuedu.ems.service.dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 首页看板 Service 接口。
 */
public interface DashboardService {

    /**
     * 查询首页统计汇总。
     * @param beginDate 开始日期，可空
     * @param endDate 结束日期，可空
     * @return 看板汇总数据
     */
    Map<String, Object> getSummary(LocalDate beginDate, LocalDate endDate);

    /**
     * 查询生产趋势。
     * @param beginDate 开始日期，可空，默认近7天
     * @param endDate 结束日期，可空，默认近7天
     * @return 趋势数据列表
     */
    List<Map<String, Object>> getProductionTrend(LocalDate beginDate, LocalDate endDate);

    /**
     * 查询质量统计。
     * @param beginDate 开始日期，可空，默认近7天
     * @param endDate 结束日期，可空，默认近7天
     * @return 质量统计数据
     */
    Map<String, Object> getQualityStatistics(LocalDate beginDate, LocalDate endDate);

    /**
     * 查询设备状态统计。
     * @return 设备状态统计数据
     */
    Map<String, Object> getEquipmentStatus();
}
