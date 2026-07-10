package com.neuedu.ems.entity.trace;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * trace_material_usage
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("trace_material_usage")
public class TraceMaterialUsage {

    @TableId(value = "usage_id", type = IdType.AUTO)
    private Long usageId;
    private Long productLotId;
    private Long barcodeId;
    private Long workOrderId;
    private Long reportId;
    private Long materialId;
    private Long batchId;
    private BigDecimal usedQuantity;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
