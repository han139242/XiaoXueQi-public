package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.SysMenu;

import java.util.List;

/**
 * 系统菜单 Service 接口。
 */
public interface SysMenuService extends IService<SysMenu> {

    Page<SysMenu> pageMenus(Integer current, Integer size, String keyword, String menuType);

    SysMenu getMenuById(Long menuId);

    boolean saveMenu(SysMenu menu);

    boolean updateMenu(SysMenu menu);

    boolean removeMenuById(Long menuId);

    List<SysMenu> listMenus(String keyword, String menuType);

    List<SysMenu> treeMenus(String menuType);
}
