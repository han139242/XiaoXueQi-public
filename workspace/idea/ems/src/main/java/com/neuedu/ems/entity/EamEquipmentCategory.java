package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * eam_equipment_category 设备分类表
 * 用于管理设备分类信息，支持树形结构（通过 parentId 实现）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("eam_equipment_category")
public class EamEquipmentCategory {

    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
