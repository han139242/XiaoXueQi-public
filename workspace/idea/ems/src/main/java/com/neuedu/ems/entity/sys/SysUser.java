package com.neuedu.ems.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * sys_user
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_user")
public class SysUser {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;
    private Long employeeId;
    private String status;
    private Integer loginFailCount;
    private LocalDateTime lockedTime;
    private LocalDateTime lastLoginTime;
    private String remark;
    @Version
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
