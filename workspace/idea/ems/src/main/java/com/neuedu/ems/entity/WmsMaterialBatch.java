package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * wms_material_batch
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_material_batch")
public class WmsMaterialBatch {

    @TableId(value = "batch_id", type = IdType.AUTO)
    private Long batchId;
    private String batchNo;
    private Long materialId;
    private Long supplierId;
    private String supplierBatchNo;
    private LocalDate productionDate;
    private LocalDate receivedDate;
    private String qualityStatus;
    private String status;
    private String sourceOrderType;
    private Long sourceOrderId;
    private Long sourceProductLotId;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
