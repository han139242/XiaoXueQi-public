package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.SysRoleMenu;

import java.util.List;

/**
 * 角色菜单关联 Service 接口。
 */
public interface SysRoleMenuService extends IService<SysRoleMenu> {

    Page<SysRoleMenu> pageRoleMenus(Integer current, Integer size, Long roleId, Long menuId);

    SysRoleMenu getRoleMenuById(Long id);

    boolean saveRoleMenu(SysRoleMenu roleMenu);

    boolean updateRoleMenu(SysRoleMenu roleMenu);

    boolean removeRoleMenuById(Long id);

    List<SysRoleMenu> listByRoleId(Long roleId);
}
