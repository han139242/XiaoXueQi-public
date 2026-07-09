package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.SysRole;
import com.neuedu.ems.mapper.SysRoleMapper;
import com.neuedu.ems.service.SysRoleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统角色 Service 实现类。
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Override
    public Page<SysRole> pageRoles(Integer current, Integer size, String keyword, String status) {
        Page<SysRole> page = new Page<>(current, size);
        return page(page, buildRoleQuery(keyword, status));
    }

    @Override
    public SysRole getRoleById(Long roleId) {
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", roleId);
        wrapper.eq("deleted_flag", 0);
        return getOne(wrapper, false);
    }

    @Override
    public boolean saveRole(SysRole role) {
        if (role.getDeletedFlag() == null) {
            role.setDeletedFlag(0);
        }
        return save(role);
    }

    @Override
    public boolean updateRole(SysRole role) {
        if (role.getRoleId() == null) {
            return false;
        }
        role.setDeletedFlag(null);
        return updateById(role);
    }

    @Override
    public boolean removeRoleById(Long roleId) {
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setDeletedFlag(1);
        return updateById(role);
    }

    @Override
    public List<SysRole> listRoles(String keyword, String status) {
        return list(buildRoleQuery(keyword, status));
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
