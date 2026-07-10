package com.neuedu.ems.service.impl.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neuedu.ems.entity.eam.EamEquipment;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.wms.WmsReplenishmentSuggestion;
import com.neuedu.ems.mapper.eam.EamEquipmentMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.wms.WmsReplenishmentSuggestionMapper;
import com.neuedu.ems.service.dashboard.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页看板 Service 实现类。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private MfgProductionOrderMapper mfgProductionOrderMapper;

    @Autowired
    private MfgWorkOrderMapper mfgWorkOrderMapper;

    @Autowired
    private MfgTaskMapper mfgTaskMapper;

    @Autowired
    private MfgWorkReportMapper mfgWorkReportMapper;

    @Autowired
    private WmsReplenishmentSuggestionMapper wmsReplenishmentSuggestionMapper;

    @Autowired
    private EamEquipmentMapper eamEquipmentMapper;

    @Autowired
    private QmsInspectionOrderMapper qmsInspectionOrderMapper;

    /**
     * 查询首页统计汇总，日期为空时默认按今天统计产量，其他项返回当前可用总量。
     * @param beginDate 开始日期，可空
     * @param endDate 结束日期，可空
     * @return 看板汇总数据
     */
    @Override
    public Map<String, Object> getSummary(LocalDate beginDate, LocalDate endDate) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("productionOrderCount", countProductionOrders(beginDate, endDate));
        summary.put("workOrderCount", countWorkOrders(beginDate, endDate));
        summary.put("runningTaskCount", countTasks("生产中"));
        LocalDateTime quantityStart = normalizeStart(beginDate, endDate);
        LocalDateTime quantityEnd = normalizeEnd(beginDate, endDate);
        summary.put("todayQualifiedQuantity", sumReportQuantity(quantityStart, quantityEnd, true));
        summary.put("todayDefectQuantity", sumReportQuantity(quantityStart, quantityEnd, false));
        summary.put("inventoryWarningCount", countInventoryWarnings());
        summary.put("equipmentFaultCount", countEquipmentFaults());
        return summary;
    }

    /**
     * 查询生产趋势，默认返回近7天按天汇总的合格量和不良量。
     * @param beginDate 开始日期，可空，默认近7天
     * @param endDate 结束日期，可空，默认近7天
     * @return 趋势数据列表
     */
    @Override
    public List<Map<String, Object>> getProductionTrend(LocalDate beginDate, LocalDate endDate) {
        LocalDate actualEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate actualStart = beginDate == null ? actualEnd.minusDays(6) : beginDate;
        if (actualStart.isAfter(actualEnd)) {
            LocalDate temp = actualStart;
            actualStart = actualEnd;
            actualEnd = temp;
        }
        LocalDateTime start = actualStart.atStartOfDay();
        LocalDateTime end = actualEnd.atTime(LocalTime.MAX);

        LambdaQueryWrapper<MfgWorkReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgWorkReport::getDeletedFlag, 0)
                .ge(MfgWorkReport::getReportTime, start)
                .le(MfgWorkReport::getReportTime, end);
        List<MfgWorkReport> reports = mfgWorkReportMapper.selectList(queryWrapper);
        Map<LocalDate, List<MfgWorkReport>> grouped = reports.stream()
                .collect(Collectors.groupingBy(item -> item.getReportTime().toLocalDate()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate day = actualStart; !day.isAfter(actualEnd); day = day.plusDays(1)) {
            List<MfgWorkReport> dailyReports = grouped.getOrDefault(day, List.of());
            BigDecimal qualified = dailyReports.stream()
                    .map(item -> item.getQualifiedQuantity() == null ? BigDecimal.ZERO : item.getQualifiedQuantity())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal defect = dailyReports.stream()
                    .map(item -> item.getDefectQuantity() == null ? BigDecimal.ZERO : item.getDefectQuantity())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", day.toString());
            row.put("qualifiedQuantity", qualified);
            row.put("defectQuantity", defect);
            result.add(row);
        }
        return result;
    }

    /**
     * 查询质量统计，默认返回近7天检验汇总。
     * @param beginDate 开始日期，可空，默认近7天
     * @param endDate 结束日期，可空，默认近7天
     * @return 质量统计数据
     */
    @Override
    public Map<String, Object> getQualityStatistics(LocalDate beginDate, LocalDate endDate) {
        LocalDateTime start = normalizeStart(beginDate, endDate);
        LocalDateTime end = normalizeEnd(beginDate, endDate);
        LambdaQueryWrapper<QmsInspectionOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionOrder::getDeletedFlag, 0)
                .ge(QmsInspectionOrder::getCreatedTime, start)
                .le(QmsInspectionOrder::getCreatedTime, end);
        List<QmsInspectionOrder> orders = qmsInspectionOrderMapper.selectList(queryWrapper);
        long inspectionCount = orders.size();
        long qualifiedCount = orders.stream().filter(item -> "合格".equals(item.getInspectionResult())).count();
        long unqualifiedCount = orders.stream().filter(item -> "不合格".equals(item.getInspectionResult())).count();
        BigDecimal qualifiedRate = inspectionCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(qualifiedCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(inspectionCount), 2, RoundingMode.HALF_UP);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inspectionCount", inspectionCount);
        result.put("qualifiedCount", qualifiedCount);
        result.put("unqualifiedCount", unqualifiedCount);
        result.put("qualifiedRate", qualifiedRate);
        return result;
    }

    /**
     * 查询设备状态统计。
     * @return 设备状态统计数据
     */
    @Override
    public Map<String, Object> getEquipmentStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("running", countEquipmentByStatus("运行"));
        result.put("stopped", countEquipmentByStatus("停机"));
        result.put("fault", countEquipmentByStatus("故障"));
        result.put("maintenance", countEquipmentByStatus("维修中"));
        return result;
    }

    /**
     * 统计生产订单数量。
     * @param start 开始时间，可空
     * @param end 结束时间，可空
     * @return 数量
     */
    private long countProductionOrders(LocalDate beginDate, LocalDate endDate) {
        QueryWrapper<MfgProductionOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted_flag", 0);
        if (beginDate != null || endDate != null) {
            LocalDate actualStart = beginDate == null ? endDate : beginDate;
            LocalDate actualEnd = endDate == null ? beginDate : endDate;
            if (actualStart.isAfter(actualEnd)) {
                LocalDate temp = actualStart;
                actualStart = actualEnd;
                actualEnd = temp;
            }
            queryWrapper.ge("created_time", actualStart.atStartOfDay())
                    .le("created_time", actualEnd.atTime(LocalTime.MAX));
        }
        return mfgProductionOrderMapper.selectCount(queryWrapper);
    }

    /**
     * 统计工单数量。
     * @param start 开始时间，可空
     * @param end 结束时间，可空
     * @return 数量
     */
    private long countWorkOrders(LocalDate beginDate, LocalDate endDate) {
        QueryWrapper<MfgWorkOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted_flag", 0);
        if (beginDate != null || endDate != null) {
            LocalDate actualStart = beginDate == null ? endDate : beginDate;
            LocalDate actualEnd = endDate == null ? beginDate : endDate;
            if (actualStart.isAfter(actualEnd)) {
                LocalDate temp = actualStart;
                actualStart = actualEnd;
                actualEnd = temp;
            }
            queryWrapper.ge("created_time", actualStart.atStartOfDay())
                    .le("created_time", actualEnd.atTime(LocalTime.MAX));
        }
        return mfgWorkOrderMapper.selectCount(queryWrapper);
    }

    /**
     * 统计指定状态的任务数量。
     * @param status 任务状态
     * @return 数量
     */
    private long countTasks(String status) {
        LambdaQueryWrapper<MfgTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgTask::getDeletedFlag, 0)
                .eq(MfgTask::getStatus, status);
        return mfgTaskMapper.selectCount(queryWrapper);
    }

    /**
     * 汇总报工数量。
     * @param start 开始时间
     * @param end 结束时间
     * @param qualified 是否统计合格数量
     * @return 汇总值
     */
    private BigDecimal sumReportQuantity(LocalDateTime start, LocalDateTime end, boolean qualified) {
        LambdaQueryWrapper<MfgWorkReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgWorkReport::getDeletedFlag, 0)
                .ge(MfgWorkReport::getReportTime, start)
                .le(MfgWorkReport::getReportTime, end);
        List<MfgWorkReport> reports = mfgWorkReportMapper.selectList(queryWrapper);
        return reports.stream()
                .map(item -> qualified ? item.getQualifiedQuantity() : item.getDefectQuantity())
                .map(value -> value == null ? BigDecimal.ZERO : value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 统计待处理补货建议数量。
     * @return 数量
     */
    private long countInventoryWarnings() {
        LambdaQueryWrapper<WmsReplenishmentSuggestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsReplenishmentSuggestion::getDeletedFlag, 0)
                .eq(WmsReplenishmentSuggestion::getStatus, "待处理");
        return wmsReplenishmentSuggestionMapper.selectCount(queryWrapper);
    }

    /**
     * 统计设备故障数量。
     * @return 数量
     */
    private long countEquipmentFaults() {
        LambdaQueryWrapper<EamEquipment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamEquipment::getDeletedFlag, 0)
                .eq(EamEquipment::getStatus, "故障");
        return eamEquipmentMapper.selectCount(queryWrapper);
    }

    /**
     * 统计指定状态的设备数量。
     * @param status 设备状态
     * @return 数量
     */
    private long countEquipmentByStatus(String status) {
        LambdaQueryWrapper<EamEquipment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamEquipment::getDeletedFlag, 0)
                .eq(EamEquipment::getStatus, status);
        return eamEquipmentMapper.selectCount(queryWrapper);
    }

    /**
     * 规范化统计开始时间。
     * @param beginDate 开始日期，可空
     * @param endDate 结束日期，可空
     * @return 开始时间
     */
    private LocalDateTime normalizeStart(LocalDate beginDate, LocalDate endDate) {
        LocalDate actualStart = beginDate;
        if (actualStart == null && endDate != null) {
            actualStart = endDate;
        }
        if (actualStart == null) {
            actualStart = LocalDate.now();
        }
        if (endDate != null && actualStart.isAfter(endDate)) {
            actualStart = endDate;
        }
        return actualStart.atStartOfDay();
    }

    /**
     * 规范化统计结束时间。
     * @param beginDate 开始日期，可空
     * @param endDate 结束日期，可空
     * @return 结束时间
     */
    private LocalDateTime normalizeEnd(LocalDate beginDate, LocalDate endDate) {
        LocalDate actualEnd = endDate;
        if (actualEnd == null && beginDate != null) {
            actualEnd = beginDate;
        }
        if (actualEnd == null) {
            actualEnd = LocalDate.now();
        }
        if (beginDate != null && actualEnd.isBefore(beginDate)) {
            actualEnd = beginDate;
        }
        return actualEnd.atTime(LocalTime.MAX);
    }
}
