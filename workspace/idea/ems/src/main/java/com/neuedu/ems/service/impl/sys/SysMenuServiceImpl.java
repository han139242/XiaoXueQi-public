package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysMenu;
import com.neuedu.ems.mapper.sys.SysMenuMapper;
import com.neuedu.ems.service.sys.PermissionService;
import com.neuedu.ems.service.sys.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统菜单 Service 实现类。
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private PermissionService permissionService;

    @Override
    public Page<SysMenu> pageMenus(Integer current, Integer size, String keyword, String menuType) {
        Page<SysMenu> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildMenuQuery(keyword, menuType));
    }

    @Override
    public SysMenu getMenuById(Long menuId) {
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("menu_id", menuId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveMenu(SysMenu menu) {
        if (menu.getDeletedFlag() == null) {
            menu.setDeletedFlag(0);
        }
        boolean saved = baseMapper.insert(menu) > 0;
        if (saved) {
            permissionService.evictAll();
        }
        return saved;
    }

    @Override
    public boolean updateMenu(SysMenu menu) {
        if (menu.getMenuId() == null) {
            return false;
        }
        menu.setDeletedFlag(null);
        boolean updated = baseMapper.updateById(menu) > 0;
        if (updated) {
            permissionService.evictAll();
        }
        return updated;
    }

    @Override
    public boolean removeMenuById(Long menuId) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setDeletedFlag(1);
        boolean removed = baseMapper.updateById(menu) > 0;
        if (removed) {
            permissionService.evictAll();
        }
        return removed;
    }

    @Override
    public List<SysMenu> listMenus(String keyword, String menuType) {
        return baseMapper.selectList(buildMenuQuery(keyword, menuType));
    }

    @Override
    public List<SysMenu> treeMenus(String menuType) {
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(menuType)) {
            wrapper.eq("menu_type", menuType);
        }
        wrapper.orderByAsc("parent_id").orderByAsc("menu_id");
        return baseMapper.selectList(wrapper);
    }

    private QueryWrapper<SysMenu> buildMenuQuery(String keyword, String menuType) {
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("menu_name", keyword)
                    .or()
                    .like("permission_code", keyword)
                    .or()
                    .like("route_path", keyword));
        }
        if (StringUtils.hasText(menuType)) {
            wrapper.eq("menu_type", menuType);
        }
        wrapper.orderByDesc("menu_id");
        return wrapper;
    }
}
