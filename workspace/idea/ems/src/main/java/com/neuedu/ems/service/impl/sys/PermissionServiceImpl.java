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

    /**
     * 多个角色同时配置数据范围时的取值优先级，从范围最大到最小。
     */
    private static final List<String> SCOPE_PRIORITY = List.of("ALL", "DEPARTMENT", "TEAM", "SELF");

    /**
     * 查询用户、角色、菜单权限、部门和班组关系的数据访问对象。
     */
    private final SysUserMapper sysUserMapper;

    /**
     * 以用户 ID 为键的短时权限快照缓存，避免每个请求重复查询角色和菜单关系。
     */
    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 权限快照缓存时长，单位为秒；未配置时默认 60 秒。
     */
    @Value("${mes.auth.permission-cache-seconds:60}")
    private long cacheSeconds = 60;

    /**
     * 创建权限服务，并注入查询授权关系所需的 Mapper。
     *
     * @param sysUserMapper 用于查询用户状态、角色、菜单权限、数据范围、部门和班组关系的 Mapper
     */
    public PermissionServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 获取指定用户当前有效的授权上下文。
     * <p>
     * 优先读取未过期缓存；缓存不存在或过期时，重新从数据库加载用户、角色、权限和数据范围。
     * 若用户不存在、已停用或已软删除，返回 {@code null}，调用方应拒绝该请求。
     * </p>
     *
     * @param userId 当前登录用户的主键，来自已校验 JWT 中的 {@code userId} 声明
     * @return 包含角色、权限码、部门、班组和数据范围的授权快照；用户无效或 {@code userId} 为空时返回 {@code null}
     */
    @Override
    public AuthorizationContext getContext(Long userId) {
        if (userId == null) {
            return null;
        }
        long now = System.nanoTime();
        CacheEntry cached = cache.get(userId);
        // 命中且未过期时直接复用快照，避免重复访问 sys_user、角色和菜单关系表。
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

    /**
     * 清除一个用户的权限缓存。
     * <p>
     * 当该用户的账号状态、角色、角色菜单或数据范围发生变化后，应调用本方法，
     * 使其下一次请求立即从数据库重新加载权限。
     * </p>
     *
     * @param userId 需要清除权限快照的用户主键；为空时不执行任何操作
     */
    @Override
    public void evictUser(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    /**
     * 清除全部用户的权限缓存。
     * <p>
     * 适用于菜单权限、角色定义等影响多个用户的全局授权关系发生变化时。
     * </p>
     */
    @Override
    public void evictAll() {
        cache.clear();
    }

    /**
     * 从数据库构建指定用户的授权上下文，不读取缓存。
     *
     * @param userId 待加载授权信息的用户主键
     * @return 有效用户对应的授权上下文；用户不存在、停用或软删除时返回 {@code null}
     */
    private AuthorizationContext loadContext(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !"ENABLE".equals(user.getStatus())
                || (user.getDeletedFlag() != null && user.getDeletedFlag() != 0)) {
            return null;
        }
        // 权限由“用户 -> 角色 -> 菜单”关系计算，不信任前端传入的权限列表。
        Set<String> roles = new LinkedHashSet<>(sysUserMapper.listRoleCodes(userId));
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            roles.add(AuthorizationContext.SUPER_ADMIN_ROLE);
        }
        Set<String> permissions = new LinkedHashSet<>(sysUserMapper.listPermissionCodes(userId));
        List<String> scopes = sysUserMapper.listDataScopes(userId);
        // 超级管理员始终拥有全部数据范围；其他用户取其角色配置中范围最大的有效值。
        String dataScope = roles.contains(AuthorizationContext.SUPER_ADMIN_ROLE)
                ? "ALL"
                : SCOPE_PRIORITY.stream().filter(scopes::contains).findFirst().orElse("SELF");
        return new AuthorizationContext(
                userId,
                user.getEmployeeId(),
                sysUserMapper.getDepartmentIdByUserId(userId),
                dataScope,
                roles,
                permissions,
                new LinkedHashSet<>(sysUserMapper.listTeamIdsByUserId(userId)));
    }

    /**
     * 单个用户的缓存记录。
     *
     * @param context 当前用户的授权快照
     * @param expiresAtNanos 缓存失效的 {@link System#nanoTime()} 时间点，仅用于进程内时长比较
     */
    private record CacheEntry(AuthorizationContext context, long expiresAtNanos) {
    }
}
