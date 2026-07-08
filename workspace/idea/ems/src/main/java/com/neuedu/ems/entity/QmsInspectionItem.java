package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * qms_inspection_item
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("qms_inspection_item")
public class QmsInspectionItem {

    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String itemType;
    private String inspectionMethod;
    private Long unitId;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
