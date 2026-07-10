package com.neuedu.ems.service.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.sys.SysRole;

import java.util.List;

/**
 * 系统角色 Service 接口。
 */
public interface SysRoleService extends IService<SysRole> {

    Page<SysRole> pageRoles(Integer current, Integer size, String keyword, String status);

    SysRole getRoleById(Long roleId);

    boolean saveRole(SysRole role);

    boolean updateRole(SysRole role);

    boolean removeRoleById(Long roleId);

    List<SysRole> listRoles(String keyword, String status);
}
