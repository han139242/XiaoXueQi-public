package com.neuedu.ems.entity.qms;

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
 * qms_inspection_order
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("qms_inspection_order")
public class QmsInspectionOrder {

    @TableId(value = "inspection_id", type = IdType.AUTO)
    private Long inspectionId;
    private String inspectionNo;
    private String inspectionType;
    private Long schemeId;
    private Long workOrderId;
    private Long taskId;
    private Long reportId;
    private Long batchId;
    private Long productLotId;
    private Long inspectorId;
    private BigDecimal sampleQuantity;
    private BigDecimal qualifiedQuantity;
    private BigDecimal unqualifiedQuantity;
    private LocalDateTime inspectionTime;
    private String inspectionResult;
    private String status;
    private String remark;
    @Version
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
