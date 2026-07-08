package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * mdm_material
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mdm_material")
public class MdmMaterial {

    @TableId(value = "material_id", type = IdType.AUTO)
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long categoryId;
    private String specModel;
    private Long unitId;
    private String materialType;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
