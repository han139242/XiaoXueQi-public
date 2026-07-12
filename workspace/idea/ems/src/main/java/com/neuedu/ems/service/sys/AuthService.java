package com.neuedu.ems.service.sys;

import com.neuedu.ems.common.AuthUserInfo;
import com.neuedu.ems.entity.sys.SysUser;

/**
 * 认证服务接口
 */
public interface AuthService {
    /**
     * 用户登录
     */
    SysUser login(String username, String password);

    AuthUserInfo getUserInfo(Long userId);

    void logout(Long userId);
}
