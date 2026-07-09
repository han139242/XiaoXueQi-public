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
 * mfg_work_order 生产工单表
 * 用于管理生产工单信息，包括工单编号、生产计划、产品、BOM、工艺路线、计划数量、完成数量和状态等
 * 工单状态：新建、已审核、已下达、生产中、已完成、已关闭
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
