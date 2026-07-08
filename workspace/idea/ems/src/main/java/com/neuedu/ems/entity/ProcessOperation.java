package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * process_operation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("process_operation")
public class ProcessOperation {

    @TableId(value = "operation_id", type = IdType.AUTO)
    private Long operationId;
    private String operationCode;
    private String operationName;
    private String operationType;
    private LocalDateTime standardTime;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
