package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * mfg_task_operator
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_task_operator")
public class MfgTaskOperator {

    private Long id;
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;
    private Long employeeId;
    private String roleInTask;
    private LocalDateTime assignedTime;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Integer deletedFlag;
}
