package com.neuedu.ems.vo.sys;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationInboxItem {
    private Long recipientId;
    private Long notificationId;
    private Long recipientUserId;
    private Long recipientEmployeeId;
    private String eventType;
    private String category;
    private String severity;
    private String title;
    private String summary;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String routePath;
    private LocalDateTime occurredTime;
    private LocalDateTime expireTime;
    private String notificationStatus;
    private String recipientReason;
    private String readStatus;
    private LocalDateTime readTime;
    private String ackStatus;
    private LocalDateTime ackTime;
    private String deliveryStatus;
}
