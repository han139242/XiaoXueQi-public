package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.SysUser;
import com.neuedu.ems.service.SysUserService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
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
@RequestMapping("/api/sys/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询用户列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<SysUser> result = sysUserService.pageUsers(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询用户。
     */
    @GetMapping("/{userId}")
    public ResultData getUser(@PathVariable("userId") Long id) {
        SysUser user = sysUserService.getUserById(id);
        return ResultData.success(ResultCode.SUCCESS, user);
    }

    /**
     * 新增用户。
     */
    @PostMapping
    public ResultData save(@RequestBody SysUser user) {
        boolean flag = sysUserService.saveUser(user);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新用户。
     */
    @PutMapping
    public ResultData update(@RequestBody SysUser user) {
        boolean flag = sysUserService.updateUser(user);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除用户，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = sysUserService.removeUserById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 重置用户密码。
     */
    @PutMapping("/{userId}/reset-password")
    public ResultData resetPassword(@PathVariable("userId") Long userId,
                                    @RequestBody ResetPasswordRequest request) {
        boolean flag = sysUserService.resetPassword(userId, request.getPassword());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 分配用户角色。
     */
    @PutMapping("/{userId}/roles")
    public ResultData assignRoles(@PathVariable("userId") Long userId,
                                  @RequestBody AssignRolesRequest request) {
        boolean flag = sysUserService.assignRoles(userId, request.getRoleIds());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出用户 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<SysUser> users = sysUserService.listUsers(keyword, status);
        excelExportUtil.exportExcel(response, users, SysUser.class, "用户列表", "用户列表");
    }

    @Data
    public static class ResetPasswordRequest {
        private String password;
    }

    @Data
    public static class AssignRolesRequest {
        private List<Long> roleIds;
    }
}
