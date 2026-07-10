package com.neuedu.ems.entity.mfg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * mfg_dispatch_order
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_dispatch_order")
public class MfgDispatchOrder {

    @TableId(value = "dispatch_id", type = IdType.AUTO)
    private Long dispatchId;
    private String dispatchNo;
    private Long workOrderId;
    private Long teamId;
    private BigDecimal plannedQuantity;
    private LocalDateTime dueTime;
    private String materialReadyStatus;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
