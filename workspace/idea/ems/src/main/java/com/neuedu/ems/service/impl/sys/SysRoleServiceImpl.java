package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysRole;
import com.neuedu.ems.mapper.sys.SysRoleMapper;
import com.neuedu.ems.service.sys.PermissionService;
import com.neuedu.ems.service.sys.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统角色 Service 实现类。
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private PermissionService permissionService;

    @Override
    public Page<SysRole> pageRoles(Integer current, Integer size, String keyword, String status) {
        Page<SysRole> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildRoleQuery(keyword, status));
    }

    @Override
    public SysRole getRoleById(Long roleId) {
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", roleId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveRole(SysRole role) {
        if (role.getDeletedFlag() == null) {
            role.setDeletedFlag(0);
        }
        boolean saved = baseMapper.insert(role) > 0;
        if (saved) {
            permissionService.evictAll();
        }
        return saved;
    }

    @Override
    public boolean updateRole(SysRole role) {
        if (role.getRoleId() == null) {
            return false;
        }
        role.setDeletedFlag(null);
        boolean updated = baseMapper.updateById(role) > 0;
        if (updated) {
            permissionService.evictAll();
        }
        return updated;
    }

    @Override
    public boolean removeRoleById(Long roleId) {
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setDeletedFlag(1);
        boolean removed = baseMapper.updateById(role) > 0;
        if (removed) {
            permissionService.evictAll();
        }
        return removed;
    }

    @Override
    public List<SysRole> listRoles(String keyword, String status) {
        return baseMapper.selectList(buildRoleQuery(keyword, status));
    }

    private QueryWrapper<SysRole> buildRoleQuery(String keyword, String status) {
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("role_code", keyword).or().like("role_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("role_id");
        return wrapper;
    }
}
