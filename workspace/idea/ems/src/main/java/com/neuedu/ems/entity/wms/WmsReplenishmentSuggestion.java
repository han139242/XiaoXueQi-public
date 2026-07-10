package com.neuedu.ems.entity.wms;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * wms_replenishment_suggestion
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_replenishment_suggestion")
public class WmsReplenishmentSuggestion {

    @TableId(value = "suggestion_id", type = IdType.AUTO)
    private Long suggestionId;
    private String suggestionNo;
    private Long materialId;
    private BigDecimal requiredQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal suggestedQuantity;
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
