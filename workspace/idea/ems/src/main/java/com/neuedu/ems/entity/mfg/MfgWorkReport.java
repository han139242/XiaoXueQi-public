package com.neuedu.ems.entity.mfg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * mfg_work_report
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_work_report")
public class MfgWorkReport {

    @TableId(value = "report_id", type = IdType.AUTO)
    private Long reportId;
    private String reportNo;
    private Long taskId;
    private Long operatorId;
    private Long stationId;
    private BigDecimal qualifiedQuantity;
    private BigDecimal defectQuantity;
    private BigDecimal actualWorkHours;
    private LocalDateTime reportTime;
    private String auditStatus;
    private String remark;
    @Version
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
