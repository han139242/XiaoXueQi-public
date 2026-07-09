package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.SysUserRole;
import com.neuedu.ems.service.SysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sys/user-role")
public class SysUserRoleController {

    @Autowired
    private SysUserRoleService sysUserRoleService;

    /**
     * 分页查询用户角色关联列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long userId,
                           @RequestParam(required = false) Long roleId) {
        Page<SysUserRole> result = sysUserRoleService.pageUserRoles(current, size, userId, roleId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据用户查询角色关联。
     */
    @GetMapping("/user/{userId}")
    public ResultData listByUser(@PathVariable("userId") Long userId) {
        List<SysUserRole> result = sysUserRoleService.listByUserId(userId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询用户角色关联。
     */
    @GetMapping("/{id}")
    public ResultData getUserRole(@PathVariable("id") Long id) {
        SysUserRole userRole = sysUserRoleService.getUserRoleById(id);
        return ResultData.success(ResultCode.SUCCESS, userRole);
    }

    /**
     * 新增用户角色关联。
     */
    @PostMapping
    public ResultData save(@RequestBody SysUserRole userRole) {
        boolean flag = sysUserRoleService.saveUserRole(userRole);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新用户角色关联。
     */
    @PutMapping
    public ResultData update(@RequestBody SysUserRole userRole) {
        boolean flag = sysUserRoleService.updateUserRole(userRole);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除用户角色关联，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = sysUserRoleService.removeUserRoleById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }
}
