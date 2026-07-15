package com.neuedu.ems.common;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前用户的生产任务提醒摘要。阶段一仅覆盖任务责任人和班组公共待办。
 */
public record TaskReminderSummary(
        boolean enabled,
        String audience,
        int total,
        int taskTotal,
        int waiting,
        int unassigned,
        int paused,
        int overdue,
        int auditPending,
        String signature,
        List<TaskReminderItem> tasks
) {

    public static TaskReminderSummary disabled() {
        return new TaskReminderSummary(false, "NONE", 0, 0, 0, 0, 0, 0, 0, "NONE", List.of());
    }

    public record TaskReminderItem(
            Long taskId,
            String taskNo,
            String status,
            List<String> categories,
            LocalDateTime dueTime,
            LocalDateTime updatedTime
    ) {
    }
}
