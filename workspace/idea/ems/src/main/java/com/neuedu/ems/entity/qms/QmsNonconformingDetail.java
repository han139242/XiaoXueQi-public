package com.neuedu.ems.entity.qms;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * qms_nonconforming_detail
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("qms_nonconforming_detail")
public class QmsNonconformingDetail {

    @TableId(value = "detail_id", type = IdType.AUTO)
    private Long detailId;
    private Long ncrId;
    private Long barcodeId;
    private Long inspectionResultId;
    private BigDecimal defectQuantity;
    private String finalDisposition;
    private String recheckResult;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
