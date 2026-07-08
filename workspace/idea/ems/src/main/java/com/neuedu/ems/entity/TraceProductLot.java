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
 * trace_product_lot
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("trace_product_lot")
public class TraceProductLot {

    @TableId(value = "product_lot_id", type = IdType.AUTO)
    private Long productLotId;
    private String lotNo;
    private Long productId;
    private Long workOrderId;
    private BigDecimal batchQuantity;
    private BigDecimal qualifiedQuantity;
    private BigDecimal defectQuantity;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
