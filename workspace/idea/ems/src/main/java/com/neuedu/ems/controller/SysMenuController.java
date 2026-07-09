package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.SysMenu;
import com.neuedu.ems.service.SysMenuService;
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
@RequestMapping("/api/sys/menu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询菜单列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String menuType) {
        Page<SysMenu> result = sysMenuService.pageMenus(current, size, keyword, menuType);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 查询菜单树。
     */
    @GetMapping("/tree")
    public ResultData tree(@RequestParam(required = false) String menuType) {
        List<SysMenu> result = sysMenuService.treeMenus(menuType);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询菜单。
     */
    @GetMapping("/{menuId}")
    public ResultData getMenu(@PathVariable("menuId") Long id) {
        SysMenu menu = sysMenuService.getMenuById(id);
        return ResultData.success(ResultCode.SUCCESS, menu);
    }

    /**
     * 新增菜单。
     */
    @PostMapping
    public ResultData save(@RequestBody SysMenu menu) {
        boolean flag = sysMenuService.saveMenu(menu);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新菜单。
     */
    @PutMapping
    public ResultData update(@RequestBody SysMenu menu) {
        boolean flag = sysMenuService.updateMenu(menu);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除菜单，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = sysMenuService.removeMenuById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出菜单 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String menuType) {
        List<SysMenu> menus = sysMenuService.listMenus(keyword, menuType);
        excelExportUtil.exportExcel(response, menus, SysMenu.class, "菜单列表", "菜单列表");
    }
}
