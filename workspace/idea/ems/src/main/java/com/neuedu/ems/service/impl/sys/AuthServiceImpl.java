package com.neuedu.ems.service.impl.sys;

import com.neuedu.ems.common.AuthUserInfo;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.exception.AccountStatusException;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.service.sys.AuthService;
import com.neuedu.ems.util.RedisUtil;
import com.neuedu.ems.util.PasswordHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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

    @Autowired
    private PasswordHasher passwordHasher;

    @Value("${mes.auth.max-login-attempts:5}")
    private int maxLoginAttempts = 5;

    @Override
    public SysUser login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return null;
        }
        SysUser user = sysUserMapper.getByUserName(username);
        if (user == null) {
            return null;
        }
        validateAccountStatus(user);
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            sysUserMapper.recordLoginFailure(user.getUserId(), Math.max(1, maxLoginAttempts));
            SysUser failedUser = sysUserMapper.selectById(user.getUserId());
            if (failedUser != null) {
                validateAccountStatus(failedUser);
            }
            return null;
        }

        if (!passwordHasher.isEncoded(user.getPasswordHash())) {
            sysUserMapper.upgradePasswordHash(user.getUserId(), passwordHasher.hash(password));
        }
        LocalDateTime loginTime = LocalDateTime.now();
        if (sysUserMapper.recordLoginSuccess(user.getUserId(), loginTime) == 0) {
            SysUser changedUser = sysUserMapper.selectById(user.getUserId());
            if (changedUser != null) {
                validateAccountStatus(changedUser);
            }
            return null;
        }
        user.setLoginFailCount(0);
        user.setLockedTime(null);
        user.setLastLoginTime(loginTime);
        user.setPasswordHash(null);
        cacheUser(user);
        return user;
    }

    @Override
    public AuthUserInfo getUserInfo(Long userId) {
        SysUser user = userId == null ? null : sysUserMapper.selectById(userId);
        if (user == null || (user.getDeletedFlag() != null && user.getDeletedFlag() != 0)) {
            return null;
        }
        validateAccountStatus(user);
        return new AuthUserInfo(
                user.getUserId(),
                user.getUsername(),
                user.getEmployeeId(),
                user.getStatus(),
                user.getLastLoginTime(),
                sysUserMapper.listRoleCodes(userId),
                sysUserMapper.listPermissionCodes(userId));
    }

    @Override
    public void logout(Long userId) {
        if (userId == null) {
            return;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return;
        }
        try {
            redisUtil.delete("username_" + user.getUsername());
        } catch (RuntimeException e) {
            log.warn("Redis unavailable while clearing login cache: {}", e.getMessage());
        }
    }

    private void cacheUser(SysUser user) {
        try {
            redisUtil.set("username_" + user.getUsername(), user, 30, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            log.warn("Redis cache unavailable; continuing login without cached user: {}", e.getMessage());
        }
    }

    private void validateAccountStatus(SysUser user) {
        String status = user.getStatus();
        if ("ENABLE".equals(status)) {
            return;
        }
        if ("DISABLE".equals(status)) {
            throw new AccountStatusException(ResultCode.USER_DISABLED);
        }
        if ("LOCKED".equals(status)) {
            throw new AccountStatusException(ResultCode.USER_LOCKED);
        }
        throw new AccountStatusException(ResultCode.USER_STATUS_ERROR);
    }
}
