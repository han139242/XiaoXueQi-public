package com.neuedu.ems.common;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationEventPayload(
        String category,
        String severity,
        String title,
        String summary,
        String bizType,
        Long bizId,
        String bizNo,
        String routePath,
        Long sourceUserId,
        Long sourceEmployeeId,
        LocalDateTime occurredTime,
        LocalDateTime expireTime,
        boolean acknowledgmentRequired,
        List<RecipientTarget> recipients) {

    public record RecipientTarget(Long employeeId, String reason, String requiredPermission) {
    }
}
