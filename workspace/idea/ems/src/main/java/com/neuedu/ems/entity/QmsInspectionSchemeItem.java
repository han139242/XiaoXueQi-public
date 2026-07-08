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
 * qms_inspection_scheme_item
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("qms_inspection_scheme_item")
public class QmsInspectionSchemeItem {

    private Long id;
    @TableId(value = "scheme_id", type = IdType.AUTO)
    private Long schemeId;
    private Long itemId;
    private Long standardId;
    private BigDecimal sampleQuantity;
    private Integer sequenceNo;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
