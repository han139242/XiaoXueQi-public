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
 * wms_outbound_order
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_outbound_order")
public class WmsOutboundOrder {

    @TableId(value = "outbound_id", type = IdType.AUTO)
    private Long outboundId;
    private String outboundNo;
    private String outboundType;
    private Long warehouseId;
    private Long workOrderId;
    private Long applicantId;
    private String auditStatus;
    private LocalDateTime outboundTime;
    private String remark;
    @Version
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
