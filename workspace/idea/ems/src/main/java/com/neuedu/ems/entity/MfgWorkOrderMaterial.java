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
 * mfg_work_order_material
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_work_order_material")
public class MfgWorkOrderMaterial {

    @TableId(value = "requirement_id", type = IdType.AUTO)
    private Long requirementId;
    private Long workOrderId;
    private Long materialId;
    private BigDecimal requiredQuantity;
    private BigDecimal issuedQuantity;
    private BigDecimal shortageQuantity;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
