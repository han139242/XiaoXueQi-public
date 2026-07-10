package com.neuedu.ems.service.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.sys.SysUserRole;

import java.util.List;

/**
 * 用户角色关联 Service 接口。
 */
public interface SysUserRoleService extends IService<SysUserRole> {

    Page<SysUserRole> pageUserRoles(Integer current, Integer size, Long userId, Long roleId);

    SysUserRole getUserRoleById(Long id);

    boolean saveUserRole(SysUserRole userRole);

    boolean updateUserRole(SysUserRole userRole);

    boolean removeUserRoleById(Long id);

    List<SysUserRole> listByUserId(Long userId);
}
