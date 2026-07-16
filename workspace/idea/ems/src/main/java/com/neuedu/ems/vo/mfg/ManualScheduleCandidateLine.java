package com.neuedu.ems.vo.mfg;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 手工排程时订单可选择的产线及其最早可用时段。
 */
@Data
public class ManualScheduleCandidateLine {
    private Long lineId;
    private String lineCode;
    private String lineName;
    private Long workshopId;
    private Long routeId;
    private Integer priorityLevel;
    private BigDecimal capacityFactor;
    private BigDecimal estimatedMinutes;
    private Long minimumWorkingMinutes;
    private LocalDateTime earliestStartTime;
    private LocalDateTime earliestEndTime;
}
