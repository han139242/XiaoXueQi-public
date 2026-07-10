package com.neuedu.ems.entity.mdm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * mdm_unit
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mdm_unit")
public class MdmUnit {

    @TableId(value = "unit_id", type = IdType.AUTO)
    private Long unitId;
    private String unitCode;
    private String unitName;
    private Integer precisionScale;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
