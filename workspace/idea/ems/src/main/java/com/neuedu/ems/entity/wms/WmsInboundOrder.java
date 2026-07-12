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
 * wms_inbound_order
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_inbound_order")
public class WmsInboundOrder {

    @TableId(value = "inbound_id", type = IdType.AUTO)
    private Long inboundId;
    private String inboundNo;
    private String inboundType;
    private Long warehouseId;
    private String sourceOrderType;
    private Long sourceOrderId;
    private String auditStatus;
    private LocalDateTime inboundTime;
    private String remark;
    @Version
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
