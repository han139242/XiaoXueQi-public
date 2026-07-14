package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysRoleMenu;
import com.neuedu.ems.entity.sys.SysRole;
import com.neuedu.ems.entity.sys.SysMenu;
import com.neuedu.ems.mapper.sys.SysMenuMapper;
import com.neuedu.ems.mapper.sys.SysRoleMapper;
import com.neuedu.ems.mapper.sys.SysRoleMenuMapper;
import com.neuedu.ems.service.sys.PermissionService;
import com.neuedu.ems.service.sys.SysRoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 角色菜单关联 Service 实现类。
 */
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private PermissionService permissionService;

    @Override
    public Page<SysRoleMenu> pageRoleMenus(Integer current, Integer size, Long roleId, Long menuId) {
        Page<SysRoleMenu> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildRoleMenuQuery(roleId, menuId));
    }

    @Override
    public SysRoleMenu getRoleMenuById(Long id) {
        QueryWrapper<SysRoleMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveRoleMenu(SysRoleMenu roleMenu) {
        if (isSuperAdminRole(roleMenu.getRoleId())) {
            return false;
        }
        if (roleMenu.getCreatedTime() == null) {
            roleMenu.setCreatedTime(LocalDateTime.now());
        }
        if (roleMenu.getDeletedFlag() == null) {
            roleMenu.setDeletedFlag(0);
        }
        boolean saved = baseMapper.insert(roleMenu) > 0;
        if (saved) {
            permissionService.evictAll();
        }
        return saved;
    }

    @Override
    public boolean updateRoleMenu(SysRoleMenu roleMenu) {
        if (roleMenu.getId() == null) {
            return false;
        }
        SysRoleMenu existing = getRoleMenuById(roleMenu.getId());
        if (existing == null || isSuperAdminRole(existing.getRoleId()) || isSuperAdminRole(roleMenu.getRoleId())) {
            return false;
        }
        roleMenu.setDeletedFlag(null);
        boolean updated = baseMapper.updateById(roleMenu) > 0;
        if (updated) {
            permissionService.evictAll();
        }
        return updated;
    }

    @Override
    public boolean removeRoleMenuById(Long id) {
        SysRoleMenu existing = getRoleMenuById(id);
        if (existing == null || isSuperAdminRole(existing.getRoleId())) {
            return false;
        }
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setId(id);
        roleMenu.setDeletedFlag(1);
        boolean removed = baseMapper.updateById(roleMenu) > 0;
        if (removed) {
            permissionService.evictAll();
        }
        return removed;
    }

    @Override
    public List<SysRoleMenu> listByRoleId(Long roleId) {
        QueryWrapper<SysRoleMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", roleId);
        wrapper.eq("deleted_flag", 0);
        wrapper.orderByAsc("menu_id").orderByDesc("id");
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceRoleMenus(Long roleId, List<Long> menuIds) {
        if (roleId == null) {
            return false;
        }
        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.eq("role_id", roleId).eq("deleted_flag", 0).eq("status", "ENABLE");
        SysRole role = sysRoleMapper.selectOne(roleQuery);
        if (role == null || "SUPER_ADMIN".equals(role.getRoleCode())) {
            return false;
        }

        Set<Long> targetMenuIds = menuIds == null
                ? new LinkedHashSet<>()
                : menuIds.stream().filter(id -> id != null).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!targetMenuIds.isEmpty()) {
            QueryWrapper<SysMenu> menuQuery = new QueryWrapper<>();
            menuQuery.in("menu_id", targetMenuIds).eq("deleted_flag", 0);
            if (sysMenuMapper.selectCount(menuQuery) != targetMenuIds.size()) {
                return false;
            }
        }

        QueryWrapper<SysRoleMenu> relationQuery = new QueryWrapper<>();
        relationQuery.eq("role_id", roleId);
        Map<Long, SysRoleMenu> existingByMenu = baseMapper.selectList(relationQuery).stream()
                .collect(Collectors.toMap(SysRoleMenu::getMenuId, Function.identity(), (first, second) -> first));
        LocalDateTime now = LocalDateTime.now();

        for (SysRoleMenu existing : existingByMenu.values()) {
            boolean shouldEnable = targetMenuIds.remove(existing.getMenuId());
            int expectedFlag = shouldEnable ? 0 : 1;
            if (existing.getDeletedFlag() == null || existing.getDeletedFlag() != expectedFlag) {
                SysRoleMenu update = new SysRoleMenu();
                update.setId(existing.getId());
                update.setDeletedFlag(expectedFlag);
                if (shouldEnable && existing.getCreatedTime() == null) {
                    update.setCreatedTime(now);
                }
                if (baseMapper.updateById(update) != 1) {
                    throw new IllegalStateException("角色权限关系更新失败");
                }
            }
        }

        for (Long menuId : targetMenuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            relation.setCreatedTime(now);
            relation.setDeletedFlag(0);
            if (baseMapper.insert(relation) != 1) {
                throw new IllegalStateException("角色权限关系新增失败");
            }
        }
        permissionService.evictAll();
        return true;
    }

    private QueryWrapper<SysRoleMenu> buildRoleMenuQuery(Long roleId, Long menuId) {
        QueryWrapper<SysRoleMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (roleId != null) {
            wrapper.eq("role_id", roleId);
        }
        if (menuId != null) {
            wrapper.eq("menu_id", menuId);
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
