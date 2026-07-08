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
 * mfg_work_order
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_work_order")
public class MfgWorkOrder {

    @TableId(value = "work_order_id", type = IdType.AUTO)
    private Long workOrderId;
    private String workOrderNo;
    private Long planId;
    private Long productId;
    private Long bomId;
    private Long routeId;
    private BigDecimal plannedQuantity;
    private BigDecimal completedQuantity;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
