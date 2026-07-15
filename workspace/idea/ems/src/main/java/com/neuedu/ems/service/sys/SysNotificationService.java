package com.neuedu.ems.service.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.vo.sys.NotificationInboxItem;
import com.neuedu.ems.vo.sys.NotificationUnreadSummary;

public interface SysNotificationService {
    NotificationUnreadSummary unreadSummary(Long userId);

    Page<NotificationInboxItem> pageInbox(Long userId, Integer current, Integer size,
                                          String category, String severity, String readStatus);

    NotificationInboxItem getInboxItem(Long userId, Long recipientId);

    boolean markRead(Long userId, Long recipientId);

    int markAllRead(Long userId, String category);

    boolean acknowledge(Long userId, Long recipientId);

    Page<NotificationInboxItem> pageAudit(Integer current, Integer size, Long recipientUserId,
                                          String category, String deliveryStatus);
}
