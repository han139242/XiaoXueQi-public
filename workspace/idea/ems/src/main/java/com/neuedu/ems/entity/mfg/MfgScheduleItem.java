package com.neuedu.ems.entity.mfg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_schedule_item")
public class MfgScheduleItem {

    @TableId(value = "schedule_item_id", type = IdType.AUTO)
    private Long scheduleItemId;
    private Long scenarioId;
    private Long productionOrderId;
    private Long productId;
    private Long routeId;
    private Long lineId;
    private Long planId;
    private Integer sequenceNo;
    private BigDecimal plannedQuantity;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private BigDecimal estimatedMinutes;
    private BigDecimal priorityScore;
    private Long tardinessMinutes;
    private String decisionSource;
    private String status;
    private String conflictReason;
    private String explanation;
    private String remark;
    @Version
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
