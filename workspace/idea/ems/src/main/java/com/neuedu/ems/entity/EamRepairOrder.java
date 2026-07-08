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
 * eam_repair_order
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("eam_repair_order")
public class EamRepairOrder {

    @TableId(value = "repair_id", type = IdType.AUTO)
    private Long repairId;
    private String repairNo;
    private Long equipmentId;
    private Long exceptionId;
    private Long reportedBy;
    private LocalDateTime reportedTime;
    private String faultDescription;
    private Long repairerId;
    private String repairResult;
    private BigDecimal repairCost;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
