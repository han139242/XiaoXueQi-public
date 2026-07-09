package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * mfg_production_plan 生产计划表
 * 用于管理生产计划信息，包括计划编号、关联生产订单、计划产线、计划开始/结束时间和计划数量等
 * 计划状态：待生效、已生效、已完成、已取消
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_production_plan")
public class MfgProductionPlan {

    @TableId(value = "plan_id", type = IdType.AUTO)
    private Long planId;
    private String planNo;
    private Long productionOrderId;
    private Long lineId;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private BigDecimal plannedQuantity;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
