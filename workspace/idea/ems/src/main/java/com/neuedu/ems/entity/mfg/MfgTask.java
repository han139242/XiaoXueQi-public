package com.neuedu.ems.entity.mfg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * mfg_task
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_task")
public class MfgTask {

    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;
    private String taskNo;
    private Long dispatchId;
    private Long workOrderId;
    private Long operationId;
    private Long stationId;
    private BigDecimal plannedQuantity;
    private BigDecimal completedQuantity;
    private BigDecimal defectQuantity;
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
