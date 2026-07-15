package com.neuedu.ems.controller.sys;

import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.service.sys.SysNotificationService;
import com.neuedu.ems.vo.sys.NotificationInboxItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sys/notification")
public class SysNotificationController {

    private final SysNotificationService notificationService;

    public SysNotificationController(SysNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/unread-count")
    public ResultData unreadCount(@RequestAttribute("userId") Long userId) {
        return ResultData.success(ResultCode.SUCCESS, notificationService.unreadSummary(userId));
    }

    @GetMapping("/page")
    public ResultData page(@RequestAttribute("userId") Long userId,
                           @RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "20") Integer size,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) String severity,
                           @RequestParam(required = false) String readStatus) {
        return ResultData.success(ResultCode.SUCCESS,
                notificationService.pageInbox(userId, current, size, category, severity, readStatus));
    }

    @GetMapping("/{recipientId}")
    public ResultData detail(@RequestAttribute("userId") Long userId,
                             @PathVariable Long recipientId) {
        NotificationInboxItem item = notificationService.getInboxItem(userId, recipientId);
        return item == null ? ResultData.fail(ResultCode.NOT_FOUND)
                : ResultData.success(ResultCode.SUCCESS, item);
    }

    @PutMapping("/{recipientId}/read")
    public ResultData markRead(@RequestAttribute("userId") Long userId,
                               @PathVariable Long recipientId) {
        return notificationService.markRead(userId, recipientId)
                ? ResultData.success(ResultCode.SUCCESS)
                : ResultData.fail(ResultCode.NOT_FOUND);
    }

    @PutMapping("/read-all")
    public ResultData markAllRead(@RequestAttribute("userId") Long userId,
                                  @RequestParam(required = false) String category) {
        return ResultData.success(ResultCode.SUCCESS, notificationService.markAllRead(userId, category));
    }

    @PutMapping("/{recipientId}/ack")
    public ResultData acknowledge(@RequestAttribute("userId") Long userId,
                                  @PathVariable Long recipientId) {
        return notificationService.acknowledge(userId, recipientId)
                ? ResultData.success(ResultCode.SUCCESS)
                : ResultData.fail(ResultCode.NOT_FOUND);
    }

    @GetMapping("/audit/page")
    public ResultData auditPage(@RequestParam(defaultValue = "1") Integer current,
                                @RequestParam(defaultValue = "20") Integer size,
                                @RequestParam(required = false) Long recipientUserId,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String deliveryStatus) {
        return ResultData.success(ResultCode.SUCCESS,
                notificationService.pageAudit(current, size, recipientUserId, category, deliveryStatus));
    }
}
