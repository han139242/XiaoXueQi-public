package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * wms_stocktake_order
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_stocktake_order")
public class WmsStocktakeOrder {

    @TableId(value = "stocktake_id", type = IdType.AUTO)
    private Long stocktakeId;
    private String stocktakeNo;
    private Long warehouseId;
    private String stocktakeType;
    private String stocktakeStatus;
    private LocalDateTime startedTime;
    private LocalDateTime finishedTime;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
