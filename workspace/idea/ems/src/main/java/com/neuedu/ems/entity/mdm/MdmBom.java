package com.neuedu.ems.entity.mdm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * mdm_bom
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mdm_bom")
public class MdmBom {

    @TableId(value = "bom_id", type = IdType.AUTO)
    private Long bomId;
    private String bomCode;
    private Long productId;
    private String bomVersion;
    private LocalDate effectiveDate;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
