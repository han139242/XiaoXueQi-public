package com.neuedu.ems.service.impl.sys;

import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.service.sys.AuthService;
import com.neuedu.ems.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public SysUser login(String username, String password) {
        SysUser user = sysUserMapper.getByUserNameAndPassword(username, password);
        if (user != null) {
            user.setPasswordHash(null);
            String redisKey = "username_" + username;
            // 登录成功，将用户信息保存到Redis中：键："username:zhangsan"
            try {
                redisUtil.set(redisKey, user, 30, TimeUnit.MINUTES);
            } catch (RuntimeException e) {
                log.warn("Redis cache unavailable; continuing login without cached user: {}", e.getMessage());
            }
            return user;
        }
        return null;
    }
}
