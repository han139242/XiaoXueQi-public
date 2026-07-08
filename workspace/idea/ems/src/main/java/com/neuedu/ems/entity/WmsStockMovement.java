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
 * wms_stock_movement
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_stock_movement")
public class WmsStockMovement {

    @TableId(value = "movement_id", type = IdType.AUTO)
    private Long movementId;
    private String movementNo;
    private String movementType;
    private String sourceOrderType;
    private Long sourceOrderId;
    private Long materialId;
    private Long batchId;
    private BigDecimal quantity;
    private String direction;
    private LocalDateTime operationTime;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
