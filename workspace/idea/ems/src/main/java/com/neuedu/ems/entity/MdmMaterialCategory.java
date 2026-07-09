package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * mdm_material_category
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mdm_material_category")
public class MdmMaterialCategory {

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
    @TableField(exist = false)
    private List<MdmMaterialCategory> children;
}
