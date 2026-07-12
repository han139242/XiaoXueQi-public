package com.neuedu.ems.entity.qms;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * qms_nonconforming_order
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("qms_nonconforming_order")
public class QmsNonconformingOrder {

    @TableId(value = "ncr_id", type = IdType.AUTO)
    private Long ncrId;
    private String ncrNo;
    private Long inspectionId;
    private Long productLotId;
    private String defectReason;
    private String dispositionType;
    private String approvalStatus;
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
