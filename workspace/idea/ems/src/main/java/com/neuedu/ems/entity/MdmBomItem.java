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
 * mdm_bom_item
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mdm_bom_item")
public class MdmBomItem {

    @TableId(value = "bom_item_id", type = IdType.AUTO)
    private Long bomItemId;
    private Long bomId;
    private Long materialId;
    private BigDecimal quantity;
    private BigDecimal lossRate;
    private Integer sequenceNo;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
