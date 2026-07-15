package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.sys.SysNotificationRecipientMapper;
import com.neuedu.ems.service.sys.SysNotificationService;
import com.neuedu.ems.vo.sys.NotificationCategoryCount;
import com.neuedu.ems.vo.sys.NotificationInboxItem;
import com.neuedu.ems.vo.sys.NotificationUnreadSummary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class SysNotificationServiceImpl implements SysNotificationService {

    private static final Set<String> CATEGORIES = Set.of("SYSTEM", "MFG", "PLAN", "QMS", "WMS", "EAM");
    private static final Set<String> SEVERITIES = Set.of("INFO", "WARNING", "HIGH", "CRITICAL");
    private static final Set<String> READ_STATUSES = Set.of("UNREAD", "READ");
    private static final Set<String> DELIVERY_STATUSES = Set.of("DELIVERED", "FAILED");

    private final SysNotificationRecipientMapper recipientMapper;

    public SysNotificationServiceImpl(SysNotificationRecipientMapper recipientMapper) {
        this.recipientMapper = recipientMapper;
    }

    @Override
    public NotificationUnreadSummary unreadSummary(Long userId) {
        requireUser(userId);
        Map<String, Long> categories = new LinkedHashMap<>();
        long total = 0;
        for (NotificationCategoryCount count : recipientMapper.countUnreadByCategory(userId)) {
            long value = count.getTotal() == null ? 0 : count.getTotal();
            categories.put(count.getCategory(), value);
            total += value;
        }
        return new NotificationUnreadSummary(total, Map.copyOf(categories));
    }

    @Override
    public Page<NotificationInboxItem> pageInbox(Long userId, Integer current, Integer size,
                                                 String category, String severity, String readStatus) {
        requireUser(userId);
        return recipientMapper.selectInboxPage(new Page<>(pageNo(current), pageSize(size)), userId,
                normalize(category, CATEGORIES, "通知分类"),
                normalize(severity, SEVERITIES, "通知级别"),
                normalize(readStatus, READ_STATUSES, "已读状态"));
    }

    @Override
    public NotificationInboxItem getInboxItem(Long userId, Long recipientId) {
        requireIdentity(userId, recipientId);
        return recipientMapper.selectInboxItem(userId, recipientId);
    }

    @Override
    public boolean markRead(Long userId, Long recipientId) {
        requireIdentity(userId, recipientId);
        return recipientMapper.markRead(userId, recipientId) > 0;
    }

    @Override
    public int markAllRead(Long userId, String category) {
        requireUser(userId);
        return recipientMapper.markAllRead(userId, normalize(category, CATEGORIES, "通知分类"));
    }

    @Override
    public boolean acknowledge(Long userId, Long recipientId) {
        requireIdentity(userId, recipientId);
        return recipientMapper.acknowledge(userId, recipientId) > 0;
    }

    @Override
    public Page<NotificationInboxItem> pageAudit(Integer current, Integer size, Long recipientUserId,
                                                 String category, String deliveryStatus) {
        return recipientMapper.selectAuditPage(new Page<>(pageNo(current), pageSize(size)), recipientUserId,
                normalize(category, CATEGORIES, "通知分类"),
                normalize(deliveryStatus, DELIVERY_STATUSES, "投递状态"));
    }

    private String normalize(String value, Set<String> allowed, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), label + "不合法");
        }
        return normalized;
    }

    private long pageNo(Integer current) {
        return current == null || current < 1 ? 1 : current;
    }

    private long pageSize(Integer size) {
        return size == null ? 20 : Math.min(Math.max(size, 1), 100);
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    private void requireIdentity(Long userId, Long recipientId) {
        requireUser(userId);
        if (recipientId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
    }
}
