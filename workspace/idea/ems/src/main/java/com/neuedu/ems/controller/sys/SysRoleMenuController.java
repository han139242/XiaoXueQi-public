package com.neuedu.ems.controller.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.sys.SysRoleMenu;
import com.neuedu.ems.service.sys.SysRoleMenuService;
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
@RequestMapping("/api/sys/role-menu")
public class SysRoleMenuController {

    @Autowired
    private SysRoleMenuService sysRoleMenuService;

    /**
     * 分页查询角色菜单关联列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long roleId,
                           @RequestParam(required = false) Long menuId) {
        Page<SysRoleMenu> result = sysRoleMenuService.pageRoleMenus(current, size, roleId, menuId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据角色查询菜单关联。
     */
    @GetMapping("/role/{roleId}")
    public ResultData listByRole(@PathVariable("roleId") Long roleId) {
        List<SysRoleMenu> result = sysRoleMenuService.listByRoleId(roleId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询角色菜单关联。
     */
    @GetMapping("/{id}")
    public ResultData getRoleMenu(@PathVariable("id") Long id) {
        SysRoleMenu roleMenu = sysRoleMenuService.getRoleMenuById(id);
        return ResultData.success(ResultCode.SUCCESS, roleMenu);
    }

    /**
     * 新增角色菜单关联。
     */
    @PostMapping
    public ResultData save(@RequestBody SysRoleMenu roleMenu) {
        boolean flag = sysRoleMenuService.saveRoleMenu(roleMenu);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新角色菜单关联。
     */
    @PutMapping
    public ResultData update(@RequestBody SysRoleMenu roleMenu) {
        boolean flag = sysRoleMenuService.updateRoleMenu(roleMenu);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除角色菜单关联，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = sysRoleMenuService.removeRoleMenuById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }
}
