package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neuedu.ems.common.NotificationEventPayload;
import com.neuedu.ems.entity.org.OrgDepartment;
import com.neuedu.ems.entity.sys.SysEventOutbox;
import com.neuedu.ems.entity.sys.SysNotification;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.mapper.org.OrgDepartmentMapper;
import com.neuedu.ems.mapper.sys.SysEventOutboxMapper;
import com.neuedu.ems.mapper.sys.SysNotificationMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.service.sys.NotificationOutboxService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResponsibilityNotificationSupport {

    private final NotificationOutboxService outboxService;
    private final SysUserMapper userMapper;
    private final OrgDepartmentMapper departmentMapper;
    private final SysNotificationMapper notificationMapper;
    private final SysEventOutboxMapper outboxMapper;

    public ResponsibilityNotificationSupport(NotificationOutboxService outboxService,
                                             SysUserMapper userMapper,
                                             OrgDepartmentMapper departmentMapper,
                                             SysNotificationMapper notificationMapper,
                                             SysEventOutboxMapper outboxMapper) {
        this.outboxService = outboxService;
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.notificationMapper = notificationMapper;
        this.outboxMapper = outboxMapper;
    }

    public void publish(String eventKey, String aggregateType, Long aggregateId, String eventType,
                        String category, String severity, String title, String summary,
                        String bizNo, String routePath, Long sourceUserId, boolean acknowledgmentRequired,
                        Collection<NotificationEventPayload.RecipientTarget> rawTargets) {
        Map<String, NotificationEventPayload.RecipientTarget> targets = new LinkedHashMap<>();
        if (rawTargets != null) {
            for (NotificationEventPayload.RecipientTarget target : rawTargets) {
                if (target != null && target.employeeId() != null) {
                    targets.putIfAbsent(target.employeeId() + ":" + target.requiredPermission(), target);
                }
            }
        }
        NotificationEventPayload payload = new NotificationEventPayload(
                category, severity, title, summary, aggregateType, aggregateId, bizNo, routePath,
                sourceUserId, employeeIdForUser(sourceUserId), LocalDateTime.now(), null,
                acknowledgmentRequired, List.copyOf(targets.values()));
        outboxService.enqueue(eventKey, aggregateType, aggregateId, eventType, payload);
    }

    public NotificationEventPayload.RecipientTarget target(Long employeeId, String reason, String permission) {
        return employeeId == null ? null : new NotificationEventPayload.RecipientTarget(employeeId, reason, permission);
    }

    public Long employeeIdForUser(Long userId) {
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        return user == null || Integer.valueOf(1).equals(user.getDeletedFlag()) ? null : user.getEmployeeId();
    }

    public Long departmentManager(String departmentCode) {
        OrgDepartment department = departmentMapper.selectOne(new LambdaQueryWrapper<OrgDepartment>()
                .eq(OrgDepartment::getDepartmentCode, departmentCode)
                .eq(OrgDepartment::getDeletedFlag, 0)
                .last("LIMIT 1"));
        return department == null ? null : department.getManagerId();
    }

    public boolean hasOpenEvent(String aggregateType, Long aggregateId, String eventType) {
        if (aggregateId == null) {
            return false;
        }
        long activeNotifications = notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getBizType, aggregateType)
                .eq(SysNotification::getBizId, aggregateId)
                .eq(SysNotification::getEventType, eventType)
                .eq(SysNotification::getStatus, "ACTIVE")
                .eq(SysNotification::getDeletedFlag, 0));
        if (activeNotifications > 0) {
            return true;
        }
        return outboxMapper.selectCount(new LambdaQueryWrapper<SysEventOutbox>()
                .eq(SysEventOutbox::getAggregateType, aggregateType)
                .eq(SysEventOutbox::getAggregateId, aggregateId)
                .eq(SysEventOutbox::getEventType, eventType)
                .in(SysEventOutbox::getStatus, "PENDING", "FAILED", "PROCESSING")
                .eq(SysEventOutbox::getDeletedFlag, 0)) > 0;
    }

    public void cancelEvents(String aggregateType, Long aggregateId, Collection<String> eventTypes,
                             Long sourceUserId) {
        if (aggregateId == null || eventTypes == null || eventTypes.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        SysNotification notification = new SysNotification();
        notification.setStatus("CANCELLED");
        notification.setUpdatedBy(sourceUserId);
        notification.setUpdatedTime(now);
        notificationMapper.update(notification, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getBizType, aggregateType)
                .eq(SysNotification::getBizId, aggregateId)
                .in(SysNotification::getEventType, eventTypes)
                .eq(SysNotification::getStatus, "ACTIVE")
                .eq(SysNotification::getDeletedFlag, 0));

        SysEventOutbox outbox = new SysEventOutbox();
        outbox.setStatus("CANCELLED");
        outbox.setProcessedTime(now);
        outbox.setUpdatedBy(sourceUserId);
        outbox.setUpdatedTime(now);
        outboxMapper.update(outbox, new LambdaUpdateWrapper<SysEventOutbox>()
                .eq(SysEventOutbox::getAggregateType, aggregateType)
                .eq(SysEventOutbox::getAggregateId, aggregateId)
                .in(SysEventOutbox::getEventType, eventTypes)
                .in(SysEventOutbox::getStatus, "PENDING", "FAILED", "PROCESSING")
                .eq(SysEventOutbox::getDeletedFlag, 0));
    }
}
