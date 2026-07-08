package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.SysUser;
import com.neuedu.ems.service.UserService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size) {
        Page<SysUser> page = new Page<>(current, size);
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        Page<SysUser> result = userService.page(page, wrapper);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 新增用户
     */
    @PostMapping
    public ResultData save(@RequestBody SysUser user) {
        boolean flag = userService.save(user);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{userId}")
    public ResultData getUser(@PathVariable("userId") Long id) {
        SysUser user = userService.getById(id);
        return ResultData.success(ResultCode.SUCCESS, user);
    }

    /**
     * 删除用户（软删除）
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setDeletedFlag(1);
        boolean flag = userService.updateById(user);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出用户Excel
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        List<SysUser> users = userService.list(wrapper);
        excelExportUtil.exportExcel(response, users, SysUser.class, "用户列表", "用户列表");
    }

    /**
     * 更新用户
     */
    @PutMapping
    public ResultData update(@RequestBody SysUser user) {
        boolean flag = userService.updateById(user);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }
}
