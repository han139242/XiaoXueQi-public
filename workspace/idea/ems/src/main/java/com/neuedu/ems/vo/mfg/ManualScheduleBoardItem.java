package com.neuedu.ems.vo.mfg;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ManualScheduleBoardItem {
    private String sourceType;
    private Boolean aiBatch;
    private Long scenarioId;
    private String scenarioNo;
    private Integer scenarioVersion;
    private Long scheduleItemId;
    private Integer itemVersion;
    private Long planId;
    private String planNo;
    private Long productionOrderId;
    private String productionOrderNo;
    private Long productId;
    private Long lineId;
    private BigDecimal plannedQuantity;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private Long tardinessMinutes;
    private String status;
    private String conflictReason;
    private String remark;
    private Boolean editable;
}
