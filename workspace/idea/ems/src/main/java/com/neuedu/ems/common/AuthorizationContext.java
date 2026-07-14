package com.neuedu.ems.common;

import java.util.Set;

/**
 * 当前请求的应用权限和数据范围快照。
 */
public record AuthorizationContext(
        Long userId,
        Long employeeId,
        Long departmentId,
        String dataScope,
        Set<String> roles,
        Set<String> permissions,
        Set<Long> teamIds
) {
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    public AuthorizationContext {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        teamIds = teamIds == null ? Set.of() : Set.copyOf(teamIds);
    }

    public boolean hasPermission(String permissionCode) {
        return permissionCode != null && (isSuperAdmin() || permissions.contains(permissionCode));
    }

    public boolean isSuperAdmin() {
        return roles.contains(SUPER_ADMIN_ROLE);
    }
}
