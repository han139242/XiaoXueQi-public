package com.neuedu.ems.entity.wms;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * wms_return_detail
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_return_detail")
public class WmsReturnDetail {

    @TableId(value = "detail_id", type = IdType.AUTO)
    private Long detailId;
    private Long returnId;
    private Long materialId;
    private Long batchId;
    private BigDecimal returnQuantity;
    private String returnReason;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
