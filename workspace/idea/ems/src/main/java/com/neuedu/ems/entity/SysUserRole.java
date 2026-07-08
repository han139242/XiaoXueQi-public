package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * sys_user_role
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_user_role")
public class SysUserRole {

    private Long id;
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    private Long roleId;
    private LocalDateTime assignedTime;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Integer deletedFlag;
}
