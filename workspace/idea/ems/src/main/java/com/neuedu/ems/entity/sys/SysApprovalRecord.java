package com.neuedu.ems.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * sys_approval_record
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_approval_record")
public class SysApprovalRecord {

    @TableId(value = "approval_id", type = IdType.AUTO)
    private Long approvalId;
    private String bizType;
    private Long bizId;
    private String approvalNode;
    private Long approverId;
    private String approvalResult;
    private String approvalComment;
    private LocalDateTime approvalTime;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
