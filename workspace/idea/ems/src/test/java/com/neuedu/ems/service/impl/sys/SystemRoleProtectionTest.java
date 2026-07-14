package com.neuedu.ems.service.impl.sys;

import com.neuedu.ems.entity.sys.SysRole;
import com.neuedu.ems.entity.sys.SysRoleMenu;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.entity.sys.SysUserRole;
import com.neuedu.ems.mapper.sys.SysRoleMapper;
import com.neuedu.ems.mapper.sys.SysRoleMenuMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.mapper.sys.SysUserRoleMapper;
import com.neuedu.ems.service.sys.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemRoleProtectionTest {

    @Test
    void preventsDisablingOrDeletingSuperAdminRole() {
        SysRoleMapper mapper = mock(SysRoleMapper.class);
        SysRoleServiceImpl service = new SysRoleServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "permissionService", mock(PermissionService.class));
        when(mapper.selectOne(any())).thenReturn(superAdminRole());

        SysRole update = new SysRole();
        update.setRoleId(1L);
        update.setStatus("DISABLE");

        assertFalse(service.updateRole(update));
        assertFalse(service.removeRoleById(1L));
        verify(mapper, never()).updateById(any(SysRole.class));
    }

    @Test
    void preventsReplacingSuperAdminPermissions() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuServiceImpl service = new SysRoleMenuServiceImpl();
        ReflectionTestUtils.setField(service, "sysRoleMapper", roleMapper);
        when(roleMapper.selectOne(any())).thenReturn(superAdminRole());

        assertFalse(service.replaceRoleMenus(1L, List.of(9L)));
    }

    @Test
    void preventsDeletingOrDemotingSuperAdminUser() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserServiceImpl service = new SysUserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        ReflectionTestUtils.setField(service, "sysUserRoleMapper", mock(SysUserRoleMapper.class));
        ReflectionTestUtils.setField(service, "sysRoleMapper", mock(SysRoleMapper.class));
        ReflectionTestUtils.setField(service, "permissionService", mock(PermissionService.class));
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setUsername("admin");
        user.setDeletedFlag(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.listRoleCodes(7L)).thenReturn(List.of("SUPER_ADMIN"));

        assertFalse(service.removeUserById(7L));
        assertFalse(service.assignRoles(7L, List.of()));
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void preventsAssigningSuperAdminRoleToAnotherUser() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserServiceImpl service = new SysUserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        ReflectionTestUtils.setField(service, "sysUserRoleMapper", userRoleMapper);
        ReflectionTestUtils.setField(service, "sysRoleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "permissionService", mock(PermissionService.class));
        when(userMapper.listRoleCodes(8L)).thenReturn(List.of("PLAN_SCHEDULER"));
        when(roleMapper.selectCount(any())).thenReturn(1L);

        assertFalse(service.assignRoles(8L, List.of(1L)));
        verify(userRoleMapper, never()).selectList(any());
    }

    @Test
    void preventsGenericRelationServicesFromChangingSuperAdminGrants() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        when(roleMapper.selectCount(any())).thenReturn(1L);

        SysUserRoleServiceImpl userRoleService = new SysUserRoleServiceImpl();
        ReflectionTestUtils.setField(userRoleService, "sysRoleMapper", roleMapper);
        ReflectionTestUtils.setField(userRoleService, "permissionService", mock(PermissionService.class));
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        ReflectionTestUtils.setField(userRoleService, "baseMapper", userRoleMapper);
        SysUserRole userRole = new SysUserRole();
        userRole.setRoleId(1L);

        SysRoleMenuServiceImpl roleMenuService = new SysRoleMenuServiceImpl();
        ReflectionTestUtils.setField(roleMenuService, "sysRoleMapper", roleMapper);
        ReflectionTestUtils.setField(roleMenuService, "permissionService", mock(PermissionService.class));
        ReflectionTestUtils.setField(roleMenuService, "baseMapper", mock(SysRoleMenuMapper.class));
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(1L);

        assertFalse(userRoleService.saveUserRole(userRole));
        assertFalse(roleMenuService.saveRoleMenu(roleMenu));
        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
    }

    private SysRole superAdminRole() {
        SysRole role = new SysRole();
        role.setRoleId(1L);
        role.setRoleCode("SUPER_ADMIN");
        role.setStatus("ENABLE");
        role.setDataScope("ALL");
        role.setDeletedFlag(0);
        return role;
    }
}
