package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * sys_role_menu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_role_menu")
public class SysRoleMenu {

    private Long id;
    @TableId(value = "role_id", type = IdType.AUTO)
    private Long roleId;
    private Long menuId;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Integer deletedFlag;
}
