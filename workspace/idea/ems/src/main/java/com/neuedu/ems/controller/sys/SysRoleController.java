package com.neuedu.ems.controller.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.sys.SysRole;
import com.neuedu.ems.service.sys.SysRoleService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
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
        Page<SysRole> result = sysRoleService.pageRoles(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询角色。
     */
    @GetMapping("/{roleId}")
    public ResultData getRole(@PathVariable("roleId") Long id) {
        SysRole role = sysRoleService.getRoleById(id);
        return ResultData.success(ResultCode.SUCCESS, role);
    }

    /**
     * 新增角色。
     */
    @PostMapping
    public ResultData save(@RequestBody SysRole role) {
        boolean flag = sysRoleService.saveRole(role);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新角色。
     */
    @PutMapping
    public ResultData update(@RequestBody SysRole role) {
        boolean flag = sysRoleService.updateRole(role);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除角色，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = sysRoleService.removeRoleById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出角色 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<SysRole> roles = sysRoleService.listRoles(keyword, status);
        excelExportUtil.exportExcel(response, roles, SysRole.class, "角色列表", "角色列表");
    }
}
