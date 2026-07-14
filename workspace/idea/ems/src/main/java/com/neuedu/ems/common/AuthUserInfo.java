package com.neuedu.ems.common;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前登录用户信息。
 */
public record AuthUserInfo(
        Long userId,
        String username,
        Long employeeId,
        String status,
        LocalDateTime lastLoginTime,
        List<String> roles,
        List<String> permissions,
        boolean superAdmin,
        String dataScope
) {
}
