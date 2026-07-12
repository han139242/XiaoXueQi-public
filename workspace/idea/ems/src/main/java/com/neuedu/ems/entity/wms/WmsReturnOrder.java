package com.neuedu.ems.entity.wms;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * wms_return_order
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_return_order")
public class WmsReturnOrder {

    @TableId(value = "return_id", type = IdType.AUTO)
    private Long returnId;
    private String returnNo;
    private String returnType;
    private Long workOrderId;
    private Long applicantId;
    private String auditStatus;
    private LocalDateTime returnTime;
    private String remark;
    @Version
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
