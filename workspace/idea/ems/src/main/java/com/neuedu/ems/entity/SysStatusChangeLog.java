package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * sys_status_change_log
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_status_change_log")
public class SysStatusChangeLog {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;
    private String bizType;
    private Long bizId;
    private String oldStatus;
    private String newStatus;
    private String changeReason;
    private Long changedBy;
    private LocalDateTime changedTime;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
