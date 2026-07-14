package com.neuedu.ems.service.impl.sys;

import com.neuedu.ems.common.AuthorizationContext;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.service.sys.PermissionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从有效用户、角色和菜单关系构建短时权限快照。
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private static final List<String> SCOPE_PRIORITY = List.of("ALL", "DEPARTMENT", "TEAM", "SELF");

    private final SysUserMapper sysUserMapper;
    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${mes.auth.permission-cache-seconds:60}")
    private long cacheSeconds = 60;

    public PermissionServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public AuthorizationContext getContext(Long userId) {
        if (userId == null) {
            return null;
        }
        long now = System.nanoTime();
        CacheEntry cached = cache.get(userId);
        if (cached != null && cached.expiresAtNanos() > now) {
            return cached.context();
        }
        AuthorizationContext context = loadContext(userId);
        if (context == null) {
            cache.remove(userId);
            return null;
        }
        long ttlNanos = Duration.ofSeconds(Math.max(1, cacheSeconds)).toNanos();
        cache.put(userId, new CacheEntry(context, now + ttlNanos));
        return context;
    }

    @Override
    public void evictUser(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    @Override
    public void evictAll() {
        cache.clear();
    }

    private AuthorizationContext loadContext(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !"ENABLE".equals(user.getStatus())
                || (user.getDeletedFlag() != null && user.getDeletedFlag() != 0)) {
            return null;
        }
        Set<String> roles = new LinkedHashSet<>(sysUserMapper.listRoleCodes(userId));
        Set<String> permissions = new LinkedHashSet<>(sysUserMapper.listPermissionCodes(userId));
        List<String> scopes = sysUserMapper.listDataScopes(userId);
        String dataScope = SCOPE_PRIORITY.stream().filter(scopes::contains).findFirst().orElse("SELF");
        return new AuthorizationContext(
                userId,
                user.getEmployeeId(),
                sysUserMapper.getDepartmentIdByUserId(userId),
                dataScope,
                roles,
                permissions,
                new LinkedHashSet<>(sysUserMapper.listTeamIdsByUserId(userId)));
    }

    private record CacheEntry(AuthorizationContext context, long expiresAtNanos) {
    }
}
