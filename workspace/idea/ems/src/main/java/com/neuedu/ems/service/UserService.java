package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.SysUser;

/**
 * 用户Service接口
 */
public interface UserService extends IService<SysUser> {

    // IService已提供基础CRUD方法
    // 自定义业务方法

    /**
     * 根据用户名查询用户
     */
    SysUser getByUserName(String username);

    /**
     * 根据用户名和密码查询用户（登录）
     */
    SysUser getByUserNameAndPassword(String username, String password);
}
