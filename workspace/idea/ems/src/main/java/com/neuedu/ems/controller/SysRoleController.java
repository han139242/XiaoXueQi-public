package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.SysRole;
import com.neuedu.ems.service.SysRoleService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
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
@RequestMapping("/api/sys/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询角色列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<SysRole> page = new Page<>(current, size);
        QueryWrapper<SysRole> wrapper = buildRoleQuery(keyword, status);
        Page<SysRole> result = sysRoleService.page(page, wrapper);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询角色。
     */
    @GetMapping("/{roleId}")
    public ResultData getRole(@PathVariable("roleId") Long id) {
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", id);
        wrapper.eq("deleted_flag", 0);
        SysRole role = sysRoleService.getOne(wrapper, false);
        return ResultData.success(ResultCode.SUCCESS, role);
    }

    /**
     * 新增角色。
     */
    @PostMapping
    public ResultData save(@RequestBody SysRole role) {
        if (role.getDeletedFlag() == null) {
            role.setDeletedFlag(0);
        }
        boolean flag = sysRoleService.save(role);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新角色。
     */
    @PutMapping
    public ResultData update(@RequestBody SysRole role) {
        if (role.getRoleId() == null) {
            return ResultData.fail(ResultCode.PARAM_ERROR);
        }
        role.setDeletedFlag(null);
        boolean flag = sysRoleService.updateById(role);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除角色，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        SysRole role = new SysRole();
        role.setRoleId(id);
        role.setDeletedFlag(1);
        boolean flag = sysRoleService.updateById(role);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出角色 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        QueryWrapper<SysRole> wrapper = buildRoleQuery(keyword, status);
        List<SysRole> roles = sysRoleService.list(wrapper);
        excelExportUtil.exportExcel(response, roles, SysRole.class, "角色列表", "角色列表");
    }

    private QueryWrapper<SysRole> buildRoleQuery(String keyword, String status) {
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("role_code", keyword).or().like("role_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("role_id");
        return wrapper;
    }
}
