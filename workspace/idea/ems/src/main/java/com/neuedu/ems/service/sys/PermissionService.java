package com.neuedu.ems.service.sys;

import com.neuedu.ems.common.AuthorizationContext;

/**
 * 应用层 RBAC 权限快照和缓存管理。
 */
public interface PermissionService {

    AuthorizationContext getContext(Long userId);

    default boolean hasPermission(Long userId, String permissionCode) {
        AuthorizationContext context = getContext(userId);
        return context != null && context.hasPermission(permissionCode);
    }

    void evictUser(Long userId);

    void evictAll();
}
