package com.neuedu.ems.service.impl.sys;

import com.neuedu.ems.common.AuthorizationContext;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionServiceImplTest {

    @Test
    void loadsAndCachesEffectivePermissionContext() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        SysUser user = enabledUser();
        when(mapper.selectById(7L)).thenReturn(user);
        when(mapper.listRoleCodes(7L)).thenReturn(List.of("PRODUCTION_SUPERVISOR"));
        when(mapper.listPermissionCodes(7L)).thenReturn(List.of("mfg:task:view", "mfg:task:start"));
        when(mapper.listDataScopes(7L)).thenReturn(List.of("SELF", "TEAM"));
        when(mapper.getDepartmentIdByUserId(7L)).thenReturn(3L);
        when(mapper.listTeamIdsByUserId(7L)).thenReturn(List.of(1L));
        PermissionServiceImpl service = new PermissionServiceImpl(mapper);

        AuthorizationContext first = service.getContext(7L);
        AuthorizationContext cached = service.getContext(7L);

        assertEquals("TEAM", first.dataScope());
        assertTrue(first.hasPermission("mfg:task:start"));
        assertEquals(List.of(1L), first.teamIds().stream().toList());
        assertEquals(first, cached);
        verify(mapper, times(1)).selectById(7L);

        service.evictUser(7L);
        service.getContext(7L);
        verify(mapper, times(2)).selectById(7L);
    }

    @Test
    void rejectsDisabledUser() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        SysUser user = enabledUser();
        user.setStatus("DISABLE");
        when(mapper.selectById(7L)).thenReturn(user);

        assertNull(new PermissionServiceImpl(mapper).getContext(7L));
    }

    @Test
    void superAdminHasAllPermissionsAndAllDataScope() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        when(mapper.selectById(7L)).thenReturn(enabledUser());
        when(mapper.listRoleCodes(7L)).thenReturn(List.of("SUPER_ADMIN"));
        when(mapper.listPermissionCodes(7L)).thenReturn(List.of());
        when(mapper.listDataScopes(7L)).thenReturn(List.of("SELF"));
        when(mapper.listTeamIdsByUserId(7L)).thenReturn(List.of());

        AuthorizationContext context = new PermissionServiceImpl(mapper).getContext(7L);

        assertTrue(context.isSuperAdmin());
        assertTrue(context.hasPermission("mfg:schedule-scenario:confirm"));
        assertEquals("ALL", context.dataScope());
    }

    @Test
    void adminUsernameHasSuperAdminPermissionsEvenWithoutRoleRelation() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        SysUser admin = enabledUser();
        admin.setUsername("admin");
        when(mapper.selectById(7L)).thenReturn(admin);
        when(mapper.listRoleCodes(7L)).thenReturn(List.of());
        when(mapper.listPermissionCodes(7L)).thenReturn(List.of());
        when(mapper.listDataScopes(7L)).thenReturn(List.of("SELF"));
        when(mapper.listTeamIdsByUserId(7L)).thenReturn(List.of());

        AuthorizationContext context = new PermissionServiceImpl(mapper).getContext(7L);

        assertTrue(context.isSuperAdmin());
        assertTrue(context.hasPermission("mfg:work-report:submit"));
        assertEquals("ALL", context.dataScope());
    }

    private SysUser enabledUser() {
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setEmployeeId(11L);
        user.setStatus("ENABLE");
        user.setDeletedFlag(0);
        return user;
    }
}
