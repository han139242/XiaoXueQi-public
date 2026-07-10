package com.neuedu.ems.service.sys;

import com.neuedu.ems.entity.sys.SysUser;

/**
 * 认证服务接口
 */
public interface AuthService {
    /**
     * 用户登录
     */
    SysUser login(String username, String password);
}
