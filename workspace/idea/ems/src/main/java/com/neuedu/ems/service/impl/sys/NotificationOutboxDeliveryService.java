package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuedu.ems.common.NotificationEventPayload;
import com.neuedu.ems.entity.sys.SysEventOutbox;
import com.neuedu.ems.entity.sys.SysNotification;
import com.neuedu.ems.entity.sys.SysNotificationRecipient;
import com.neuedu.ems.mapper.sys.SysEventOutboxMapper;
import com.neuedu.ems.mapper.sys.SysNotificationMapper;
import com.neuedu.ems.mapper.sys.SysNotificationRecipientMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.service.sys.NotificationRoutePolicy;
import com.neuedu.ems.vo.sys.NotificationRecipientAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationOutboxDeliveryService {

    private final SysEventOutboxMapper outboxMapper;
    private final SysNotificationMapper notificationMapper;
    private final SysNotificationRecipientMapper recipientMapper;
    private final SysUserMapper userMapper;
    private final NotificationRoutePolicy routePolicy;
    private final ObjectMapper objectMapper;

    public NotificationOutboxDeliveryService(SysEventOutboxMapper outboxMapper,
                                             SysNotificationMapper notificationMapper,
                                             SysNotificationRecipientMapper recipientMapper,
                                             SysUserMapper userMapper,
                                             NotificationRoutePolicy routePolicy,
                                             ObjectMapper objectMapper) {
        this.outboxMapper = outboxMapper;
        this.notificationMapper = notificationMapper;
        this.recipientMapper = recipientMapper;
        this.userMapper = userMapper;
        this.routePolicy = routePolicy;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void deliver(Long outboxId) {
        SysEventOutbox outbox = outboxMapper.selectById(outboxId);
        if (outbox == null || !"PROCESSING".equals(outbox.getStatus())
                || Integer.valueOf(1).equals(outbox.getDeletedFlag())) {
            return;
        }
        NotificationEventPayload payload = deserialize(outbox.getPayloadJson());
        validatePayload(payload);
        LocalDateTime now = LocalDateTime.now();

        SysNotification notification = buildNotification(outbox, payload, now);
        notificationMapper.insertIgnore(notification);
        notification = notificationMapper.selectOne(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getEventKey, outbox.getEventKey())
                .eq(SysNotification::getDeletedFlag, 0));
        if (notification == null) {
            throw new IllegalStateException("NOTIFICATION_EVENT_NOT_PERSISTED");
        }

        int delivered = 0;
        for (NotificationEventPayload.RecipientTarget target : payload.recipients()) {
            List<NotificationRecipientAccount> accounts = userMapper.listNotificationRecipients(
                    target.employeeId(), target.requiredPermission());
            for (NotificationRecipientAccount account : accounts) {
                delivered += insertRecipient(notification.getNotificationId(), account, target.reason(),
                        payload.acknowledgmentRequired(), payload.sourceUserId(), now);
            }
        }
        if (delivered == 0 && recipientMapper.selectCount(
                new LambdaQueryWrapper<SysNotificationRecipient>()
                        .eq(SysNotificationRecipient::getNotificationId, notification.getNotificationId())
                        .eq(SysNotificationRecipient::getDeletedFlag, 0)) == 0) {
            throw new IllegalStateException("NO_VALID_NOTIFICATION_RECIPIENT");
        }

        outbox.setStatus("PROCESSED");
        outbox.setProcessedTime(now);
        outbox.setNextRetryTime(null);
        outbox.setLastError(null);
        outbox.setUpdatedBy(payload.sourceUserId());
        outbox.setUpdatedTime(now);
        if (outboxMapper.updateById(outbox) == 0) {
            throw new IllegalStateException("OUTBOX_PROCESS_STATE_CONFLICT");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(Long outboxId, String error, int maxRetries) {
        SysEventOutbox outbox = outboxMapper.selectById(outboxId);
        if (outbox == null || "PROCESSED".equals(outbox.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int retries = (outbox.getRetryCount() == null ? 0 : outbox.getRetryCount()) + 1;
        outbox.setStatus("FAILED");
        outbox.setRetryCount(retries);
        outbox.setNextRetryTime(retries >= maxRetries ? null : now.plusMinutes(Math.min(60, 1L << Math.min(retries, 5))));
        outbox.setLastError(abbreviate(error));
        outbox.setUpdatedTime(now);
        outboxMapper.updateById(outbox);
    }

    private SysNotification buildNotification(SysEventOutbox outbox, NotificationEventPayload payload,
                                              LocalDateTime now) {
        SysNotification notification = new SysNotification();
        notification.setEventKey(outbox.getEventKey());
        notification.setEventType(outbox.getEventType());
        notification.setCategory(payload.category());
        notification.setSeverity(payload.severity());
        notification.setTitle(payload.title());
        notification.setSummary(payload.summary());
        notification.setBizType(payload.bizType());
        notification.setBizId(payload.bizId());
        notification.setBizNo(payload.bizNo());
        notification.setRoutePath(payload.routePath());
        notification.setSourceUserId(payload.sourceUserId());
        notification.setSourceEmployeeId(payload.sourceEmployeeId());
        notification.setOccurredTime(payload.occurredTime() == null ? outbox.getOccurredTime() : payload.occurredTime());
        notification.setExpireTime(payload.expireTime());
        notification.setStatus("ACTIVE");
        notification.setVersion(0);
        notification.setCreatedBy(payload.sourceUserId());
        notification.setCreatedTime(now);
        notification.setUpdatedBy(payload.sourceUserId());
        notification.setUpdatedTime(now);
        notification.setDeletedFlag(0);
        return notification;
    }

    private int insertRecipient(Long notificationId, NotificationRecipientAccount account, String reason,
                                boolean acknowledgmentRequired, Long sourceUserId, LocalDateTime now) {
        SysNotificationRecipient recipient = new SysNotificationRecipient();
        recipient.setNotificationId(notificationId);
        recipient.setRecipientUserId(account.getUserId());
        recipient.setRecipientEmployeeId(account.getEmployeeId());
        recipient.setRecipientReason(reason);
        recipient.setReadStatus("UNREAD");
        recipient.setAckStatus(acknowledgmentRequired ? "PENDING" : "NOT_REQUIRED");
        recipient.setDeliveryStatus("DELIVERED");
        recipient.setDeliveredTime(now);
        recipient.setVersion(0);
        recipient.setCreatedBy(sourceUserId);
        recipient.setCreatedTime(now);
        recipient.setUpdatedBy(sourceUserId);
        recipient.setUpdatedTime(now);
        recipient.setDeletedFlag(0);
        return recipientMapper.insertIgnore(recipient);
    }

    private NotificationEventPayload deserialize(String json) {
        try {
            return objectMapper.readValue(json, NotificationEventPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("INVALID_NOTIFICATION_EVENT_PAYLOAD", e);
        }
    }

    private void validatePayload(NotificationEventPayload payload) {
        if (payload == null || !StringUtils.hasText(payload.category()) || !StringUtils.hasText(payload.severity())
                || !StringUtils.hasText(payload.title()) || !StringUtils.hasText(payload.bizType())
                || !routePolicy.isAllowed(payload.routePath()) || payload.recipients() == null
                || payload.recipients().isEmpty()) {
            throw new IllegalStateException("INVALID_NOTIFICATION_EVENT_PAYLOAD");
        }
        for (NotificationEventPayload.RecipientTarget target : payload.recipients()) {
            if (target.employeeId() == null || !StringUtils.hasText(target.reason())
                    || !StringUtils.hasText(target.requiredPermission())) {
                throw new IllegalStateException("INVALID_NOTIFICATION_RECIPIENT_TARGET");
            }
        }
    }

    private String abbreviate(String error) {
        String value = StringUtils.hasText(error) ? error.replaceAll("[\\r\\n]+", " ") : "UNKNOWN_ERROR";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
