package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.SysUser;

import java.util.List;

/**
 * 系统用户 Service 接口。
 */
public interface SysUserService extends IService<SysUser> {

    Page<SysUser> pageUsers(Integer current, Integer size, String keyword, String status);

    SysUser getUserById(Long userId);

    boolean saveUser(SysUser user);

    boolean updateUser(SysUser user);

    boolean removeUserById(Long userId);

    List<SysUser> listUsers(String keyword, String status);

    boolean resetPassword(Long userId, String password);

    boolean assignRoles(Long userId, List<Long> roleIds);

    SysUser getByUserName(String username);

    SysUser getByUserNameAndPassword(String username, String password);
}
