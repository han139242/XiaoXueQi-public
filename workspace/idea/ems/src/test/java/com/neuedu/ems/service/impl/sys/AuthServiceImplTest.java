package com.neuedu.ems.service.impl.sys;

import com.neuedu.ems.common.AuthUserInfo;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.exception.AccountStatusException;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.util.PasswordHasher;
import com.neuedu.ems.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void loginContinuesWhenRedisIsUnavailable() {
        SysUser user = userWithStatus("ENABLE");
        user.setPasswordHash("encoded-password");
        when(sysUserMapper.getByUserName("admin")).thenReturn(user);
        when(passwordHasher.matches("123456", "encoded-password")).thenReturn(true);
        when(passwordHasher.isEncoded("encoded-password")).thenReturn(true);
        when(sysUserMapper.recordLoginSuccess(eq(1L), any())).thenReturn(1);
        doThrow(new RuntimeException("Redis unavailable"))
                .when(redisUtil).set(eq("username_admin"), any(SysUser.class), eq(30L), eq(TimeUnit.MINUTES));

        SysUser result = authService.login("admin", "123456");

        assertSame(user, result);
        assertNull(result.getPasswordHash());
        assertEquals(0, result.getLoginFailCount());
        assertTrue(result.getLastLoginTime() != null);
        verify(sysUserMapper).recordLoginSuccess(eq(1L), any());
        verify(redisUtil).set("username_admin", user, 30L, TimeUnit.MINUTES);
    }

    @Test
    void loginUpgradesLegacyPlaintextPassword() {
        SysUser user = userWithStatus("ENABLE");
        user.setPasswordHash("123456");
        when(sysUserMapper.getByUserName("admin")).thenReturn(user);
        when(passwordHasher.matches("123456", "123456")).thenReturn(true);
        when(passwordHasher.isEncoded("123456")).thenReturn(false);
        when(passwordHasher.hash("123456")).thenReturn("pbkdf2-hash");
        when(sysUserMapper.recordLoginSuccess(eq(1L), any())).thenReturn(1);

        authService.login("admin", "123456");

        verify(sysUserMapper).upgradePasswordHash(1L, "pbkdf2-hash");
    }

    @Test
    void loginRecordsFailureWithoutCachingUser() {
        SysUser user = userWithStatus("ENABLE");
        user.setPasswordHash("encoded-password");
        SysUser afterFailure = userWithStatus("ENABLE");
        afterFailure.setLoginFailCount(1);
        when(sysUserMapper.getByUserName("admin")).thenReturn(user);
        when(passwordHasher.matches("bad", "encoded-password")).thenReturn(false);
        when(sysUserMapper.selectById(1L)).thenReturn(afterFailure);

        assertNull(authService.login("admin", "bad"));

        verify(sysUserMapper).recordLoginFailure(1L, 5);
        verify(redisUtil, never()).set(any(), any(), any(Long.class), any(TimeUnit.class));
    }

    @Test
    void loginReportsLockWhenFailureThresholdIsReached() {
        SysUser user = userWithStatus("ENABLE");
        user.setPasswordHash("encoded-password");
        when(sysUserMapper.getByUserName("admin")).thenReturn(user);
        when(passwordHasher.matches("bad", "encoded-password")).thenReturn(false);
        when(sysUserMapper.selectById(1L)).thenReturn(userWithStatus("LOCKED"));

        AccountStatusException exception = assertThrows(
                AccountStatusException.class,
                () -> authService.login("admin", "bad"));

        assertEquals(ResultCode.USER_LOCKED, exception.getResultCode());
    }

    @Test
    void loginDoesNotCacheUnknownUser() {
        when(sysUserMapper.getByUserName("missing")).thenReturn(null);

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

    @Test
    void returnsCurrentUserRolesAndPermissions() {
        SysUser user = userWithStatus("ENABLE");
        user.setEmployeeId(9L);
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(sysUserMapper.listRoleCodes(1L)).thenReturn(List.of("admin"));
        when(sysUserMapper.listPermissionCodes(1L)).thenReturn(List.of("sys:user:list"));
        when(sysUserMapper.listDataScopes(1L)).thenReturn(List.of("TEAM"));

        AuthUserInfo info = authService.getUserInfo(1L);

        assertEquals("admin", info.username());
        assertEquals(List.of("admin"), info.roles());
        assertEquals(List.of("sys:user:list"), info.permissions());
        assertEquals("TEAM", info.dataScope());
        assertEquals(false, info.superAdmin());
    }

    @Test
    void marksSuperAdminAndForcesAllDataScope() {
        SysUser user = userWithStatus("ENABLE");
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(sysUserMapper.listRoleCodes(1L)).thenReturn(List.of("SUPER_ADMIN"));
        when(sysUserMapper.listPermissionCodes(1L)).thenReturn(List.of());
        when(sysUserMapper.listDataScopes(1L)).thenReturn(List.of("SELF"));

        AuthUserInfo info = authService.getUserInfo(1L);

        assertTrue(info.superAdmin());
        assertEquals("ALL", info.dataScope());
    }

    private void assertLoginRejected(String status, ResultCode resultCode) {
        SysUser user = userWithStatus(status);
        when(sysUserMapper.getByUserName("admin")).thenReturn(user);

        AccountStatusException exception = assertThrows(
                AccountStatusException.class,
                () -> authService.login("admin", "123456"));

        assertEquals(resultCode, exception.getResultCode());
        verify(redisUtil, never()).set(any(), any(), any(Long.class), any(TimeUnit.class));
    }

    private SysUser userWithStatus(String status) {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUsername("admin");
        user.setPasswordHash("stored-password");
        user.setStatus(status);
        user.setDeletedFlag(0);
        return user;
    }
}
