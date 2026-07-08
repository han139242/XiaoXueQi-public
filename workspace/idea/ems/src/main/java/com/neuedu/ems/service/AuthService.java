package com.neuedu.ems.service;

import com.neuedu.ems.entity.SysUser;

/**
 * 认证服务接口
 */
public interface AuthService {
    /**
     * 用户登录
     */
    SysUser login(String username, String password);
}
