package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysUserRole;
import com.neuedu.ems.entity.sys.SysRole;
import com.neuedu.ems.mapper.sys.SysRoleMapper;
import com.neuedu.ems.mapper.sys.SysUserRoleMapper;
import com.neuedu.ems.service.sys.PermissionService;
import com.neuedu.ems.service.sys.SysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户角色关联 Service 实现类。
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public Page<SysUserRole> pageUserRoles(Integer current, Integer size, Long userId, Long roleId) {
        Page<SysUserRole> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildUserRoleQuery(userId, roleId));
    }

    @Override
    public SysUserRole getUserRoleById(Long id) {
        QueryWrapper<SysUserRole> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveUserRole(SysUserRole userRole) {
        if (isSuperAdminRole(userRole.getRoleId())) {
            return false;
        }
        if (userRole.getAssignedTime() == null) {
            userRole.setAssignedTime(LocalDateTime.now());
        }
        if (userRole.getCreatedTime() == null) {
            userRole.setCreatedTime(LocalDateTime.now());
        }
        if (userRole.getDeletedFlag() == null) {
            userRole.setDeletedFlag(0);
        }
        boolean saved = baseMapper.insert(userRole) > 0;
        if (saved) {
            permissionService.evictUser(userRole.getUserId());
        }
        return saved;
    }

    @Override
    public boolean updateUserRole(SysUserRole userRole) {
        if (userRole.getId() == null) {
            return false;
        }
        SysUserRole existing = getUserRoleById(userRole.getId());
        if (existing == null || isSuperAdminRole(existing.getRoleId()) || isSuperAdminRole(userRole.getRoleId())) {
            return false;
        }
        userRole.setDeletedFlag(null);
        boolean updated = baseMapper.updateById(userRole) > 0;
        if (updated) {
            permissionService.evictAll();
        }
        return updated;
    }

    @Override
    public boolean removeUserRoleById(Long id) {
        SysUserRole existing = getUserRoleById(id);
        if (existing == null || isSuperAdminRole(existing.getRoleId())) {
            return false;
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setId(id);
        userRole.setDeletedFlag(1);
        boolean removed = baseMapper.updateById(userRole) > 0;
        if (removed) {
            permissionService.evictAll();
        }
        return removed;
    }

    @Override
    public List<SysUserRole> listByUserId(Long userId) {
        QueryWrapper<SysUserRole> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("deleted_flag", 0);
        wrapper.orderByDesc("assigned_time").orderByDesc("id");
        return baseMapper.selectList(wrapper);
    }

    private QueryWrapper<SysUserRole> buildUserRoleQuery(Long userId, Long roleId) {
        QueryWrapper<SysUserRole> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (roleId != null) {
            wrapper.eq("role_id", roleId);
        }
        wrapper.orderByDesc("id");
        return wrapper;
    }

    private boolean isSuperAdminRole(Long roleId) {
        if (roleId == null) {
            return false;
        }
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", roleId)
                .eq("role_code", "SUPER_ADMIN")
                .eq("deleted_flag", 0);
        return sysRoleMapper.selectCount(wrapper) > 0;
    }
}
