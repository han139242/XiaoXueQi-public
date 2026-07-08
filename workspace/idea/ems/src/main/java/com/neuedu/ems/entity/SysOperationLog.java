package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * sys_operation_log
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;
    private Long userId;
    private String operationType;
    private String moduleCode;
    private Long bizId;
    private String requestIp;
    private LocalDateTime operationTime;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
