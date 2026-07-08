package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * mfg_production_order
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_production_order")
public class MfgProductionOrder {

    @TableId(value = "production_order_id", type = IdType.AUTO)
    private Long productionOrderId;
    private String productionOrderNo;
    private Long customerId;
    private Long productId;
    private BigDecimal orderQuantity;
    private LocalDate deliveryDate;
    private String sourceType;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
