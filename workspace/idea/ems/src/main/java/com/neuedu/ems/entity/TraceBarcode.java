package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * trace_barcode
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("trace_barcode")
public class TraceBarcode {

    @TableId(value = "barcode_id", type = IdType.AUTO)
    private Long barcodeId;
    private String barcodeValue;
    private String barcodeType;
    private String bizType;
    private Long bizId;
    private String generatedRule;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
