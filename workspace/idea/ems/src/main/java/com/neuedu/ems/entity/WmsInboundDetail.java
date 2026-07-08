package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * wms_inbound_detail
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_inbound_detail")
public class WmsInboundDetail {

    @TableId(value = "detail_id", type = IdType.AUTO)
    private Long detailId;
    private Long inboundId;
    private Long materialId;
    private Long batchId;
    private Long locationId;
    private BigDecimal inboundQuantity;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
