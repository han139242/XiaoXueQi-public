package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * qms_inspection_standard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("qms_inspection_standard")
public class QmsInspectionStandard {

    @TableId(value = "standard_id", type = IdType.AUTO)
    private Long standardId;
    private Long itemId;
    private Long productId;
    private Long operationId;
    private String standardValue;
    private String upperLimit;
    private String lowerLimit;
    private String judgementRule;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
