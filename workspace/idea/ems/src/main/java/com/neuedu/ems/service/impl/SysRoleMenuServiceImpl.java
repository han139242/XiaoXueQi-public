package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.SysRoleMenu;
import com.neuedu.ems.mapper.SysRoleMenuMapper;
import com.neuedu.ems.service.SysRoleMenuService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色菜单关联 Service 实现类。
 */
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {

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
        if (roleMenu.getCreatedTime() == null) {
            roleMenu.setCreatedTime(LocalDateTime.now());
        }
        if (roleMenu.getDeletedFlag() == null) {
            roleMenu.setDeletedFlag(0);
        }
        return baseMapper.insert(roleMenu) > 0;
    }

    @Override
    public boolean updateRoleMenu(SysRoleMenu roleMenu) {
        if (roleMenu.getId() == null) {
            return false;
        }
        roleMenu.setDeletedFlag(null);
        return baseMapper.updateById(roleMenu) > 0;
    }

    @Override
    public boolean removeRoleMenuById(Long id) {
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setId(id);
        roleMenu.setDeletedFlag(1);
        return baseMapper.updateById(roleMenu) > 0;
    }

    @Override
    public List<SysRoleMenu> listByRoleId(Long roleId) {
        QueryWrapper<SysRoleMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", roleId);
        wrapper.eq("deleted_flag", 0);
        wrapper.orderByAsc("menu_id").orderByDesc("id");
        return baseMapper.selectList(wrapper);
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
}
