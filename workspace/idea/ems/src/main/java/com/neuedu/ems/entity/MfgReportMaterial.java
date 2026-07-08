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
 * mfg_report_material
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_report_material")
public class MfgReportMaterial {

    private Long id;
    @TableId(value = "report_id", type = IdType.AUTO)
    private Long reportId;
    private Long materialId;
    private Long batchId;
    private BigDecimal usedQuantity;
    private BigDecimal scrapQuantity;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
