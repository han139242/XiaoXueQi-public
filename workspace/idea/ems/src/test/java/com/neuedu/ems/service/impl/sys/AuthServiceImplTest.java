package com.neuedu.ems.service.impl.sys;

import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.exception.AccountStatusException;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void loginContinuesWhenRedisIsUnavailable() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUsername("admin");
        user.setPasswordHash("plain-text-password");
        user.setStatus("ENABLE");
        when(sysUserMapper.getByUserNameAndPassword("admin", "123456")).thenReturn(user);
        doThrow(new RuntimeException("Redis unavailable"))
                .when(redisUtil).set(eq("username_admin"), any(SysUser.class), eq(30L), eq(TimeUnit.MINUTES));

        SysUser result = authService.login("admin", "123456");

        assertSame(user, result);
        assertNull(result.getPasswordHash());
        verify(redisUtil).set("username_admin", user, 30L, TimeUnit.MINUTES);
    }

    @Test
    void loginDoesNotCacheUnknownUser() {
        when(sysUserMapper.getByUserNameAndPassword("missing", "bad")).thenReturn(null);

        assertNull(authService.login("missing", "bad"));

        verify(redisUtil, never()).set(any(), any(), any(Long.class), any(TimeUnit.class));
    }

    @Test
    void loginRejectsDisabledAccount() {
        assertLoginRejected("DISABLE", ResultCode.USER_DISABLED);
    }

    @Test
    void loginRejectsLockedAccount() {
        assertLoginRejected("LOCKED", ResultCode.USER_LOCKED);
    }

    @Test
    void loginRejectsUnknownAccountStatus() {
        assertLoginRejected(null, ResultCode.USER_STATUS_ERROR);
    }

    private void assertLoginRejected(String status, ResultCode resultCode) {
        SysUser user = userWithStatus(status);
        when(sysUserMapper.getByUserNameAndPassword("admin", "123456")).thenReturn(user);

        AccountStatusException exception = assertThrows(
                AccountStatusException.class,
                () -> authService.login("admin", "123456")
        );

        assertEquals(resultCode, exception.getResultCode());
        verify(redisUtil, never()).set(any(), any(), any(Long.class), any(TimeUnit.class));
    }

    private SysUser userWithStatus(String status) {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUsername("admin");
        user.setPasswordHash("plain-text-password");
        user.setStatus(status);
        return user;
    }
}
