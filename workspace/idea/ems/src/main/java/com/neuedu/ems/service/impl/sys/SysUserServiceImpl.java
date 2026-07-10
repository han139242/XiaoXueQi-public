package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.entity.sys.SysUserRole;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.mapper.sys.SysUserRoleMapper;
import com.neuedu.ems.service.sys.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统用户 Service 实现类。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public Page<SysUser> pageUsers(Integer current, Integer size, String keyword, String status) {
        Page<SysUser> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildUserQuery(keyword, status));
    }

    @Override
    public SysUser getUserById(Long userId) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveUser(SysUser user) {
        if (user.getDeletedFlag() == null) {
            user.setDeletedFlag(0);
        }
        return baseMapper.insert(user) > 0;
    }

    @Override
    public boolean updateUser(SysUser user) {
        if (user.getUserId() == null) {
            return false;
        }
        user.setDeletedFlag(null);
        return baseMapper.updateById(user) > 0;
    }

    @Override
    public boolean removeUserById(Long userId) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setDeletedFlag(1);
        return baseMapper.updateById(user) > 0;
    }

    @Override
    public List<SysUser> listUsers(String keyword, String status) {
        return baseMapper.selectList(buildUserQuery(keyword, status));
    }

    @Override
    public boolean resetPassword(Long userId, String password) {
        if (userId == null || !StringUtils.hasText(password)) {
            return false;
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPasswordHash(password);
        return baseMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return false;
        }
        Set<Long> targetRoleIds = roleIds == null ? new LinkedHashSet<>() : roleIds.stream()
                .filter(roleId -> roleId != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        QueryWrapper<SysUserRole> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<SysUserRole> existingRoles = sysUserRoleMapper.selectList(wrapper);
        LocalDateTime now = LocalDateTime.now();

        for (SysUserRole existingRole : existingRoles) {
            Long roleId = existingRole.getRoleId();
            if (targetRoleIds.remove(roleId)) {
                SysUserRole updateRole = new SysUserRole();
                updateRole.setId(existingRole.getId());
                updateRole.setDeletedFlag(0);
                updateRole.setAssignedTime(now);
                if (existingRole.getCreatedTime() == null) {
                    updateRole.setCreatedTime(now);
                }
                sysUserRoleMapper.updateById(updateRole);
            } else if (existingRole.getDeletedFlag() == null || existingRole.getDeletedFlag() == 0) {
                SysUserRole updateRole = new SysUserRole();
                updateRole.setId(existingRole.getId());
                updateRole.setDeletedFlag(1);
                sysUserRoleMapper.updateById(updateRole);
            }
        }

        for (Long roleId : targetRoleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setAssignedTime(now);
            userRole.setCreatedTime(now);
            userRole.setDeletedFlag(0);
            sysUserRoleMapper.insert(userRole);
        }
        return true;
    }

    @Override
    public SysUser getByUserName(String username) {
        return baseMapper.getByUserName(username);
    }

    @Override
    public SysUser getByUserNameAndPassword(String username, String password) {
        return baseMapper.getByUserNameAndPassword(username, password);
    }

    private QueryWrapper<SysUser> buildUserQuery(String keyword, String status) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("username", keyword).or().like("remark", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("user_id");
        return wrapper;
    }
}
