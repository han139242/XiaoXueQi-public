package com.neuedu.ems.service.impl.sys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuedu.ems.common.NotificationEventPayload;
import com.neuedu.ems.entity.sys.SysEventOutbox;
import com.neuedu.ems.entity.sys.SysNotification;
import com.neuedu.ems.mapper.sys.SysEventOutboxMapper;
import com.neuedu.ems.mapper.sys.SysNotificationMapper;
import com.neuedu.ems.mapper.sys.SysNotificationRecipientMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.service.sys.NotificationRoutePolicy;
import com.neuedu.ems.vo.sys.NotificationRecipientAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationOutboxDeliveryServiceTest {

    private final SysEventOutboxMapper outboxMapper = mock(SysEventOutboxMapper.class);
    private final SysNotificationMapper notificationMapper = mock(SysNotificationMapper.class);
    private final SysNotificationRecipientMapper recipientMapper = mock(SysNotificationRecipientMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final NotificationOutboxDeliveryService service = new NotificationOutboxDeliveryService(
            outboxMapper, notificationMapper, recipientMapper, userMapper,
            new NotificationRoutePolicy(), objectMapper);

    private SysEventOutbox outbox;

    @BeforeEach
    void setUp() throws Exception {
        NotificationEventPayload payload = new NotificationEventPayload(
                "MFG", "INFO", "任务已分配", "任务 TASK-1 已分配给您", "MFG_TASK",
                1L, "TASK-1", "/mfg/tasks", 2L, 10L, LocalDateTime.now(), null, false,
                List.of(new NotificationEventPayload.RecipientTarget(11L, "ASSIGNEE", "mfg:task:view")));
        outbox = new SysEventOutbox();
        outbox.setOutboxId(5L);
        outbox.setEventKey("MFG_TASK:1:ASSIGNED:11");
        outbox.setEventType("MFG_TASK_ASSIGNED");
        outbox.setStatus("PROCESSING");
        outbox.setDeletedFlag(0);
        outbox.setOccurredTime(LocalDateTime.now());
        outbox.setPayloadJson(objectMapper.writeValueAsString(payload));
        when(outboxMapper.selectById(5L)).thenReturn(outbox);
        when(notificationMapper.insertIgnore(any())).thenReturn(1);
        SysNotification stored = new SysNotification();
        stored.setNotificationId(8L);
        when(notificationMapper.selectOne(any())).thenReturn(stored);
        when(outboxMapper.updateById(any(SysEventOutbox.class))).thenReturn(1);
    }

    @Test
    void deliversOnlyToEnabledAccountWithRequiredBusinessPermission() {
        NotificationRecipientAccount account = new NotificationRecipientAccount();
        account.setUserId(21L);
        account.setEmployeeId(11L);
        when(userMapper.listNotificationRecipients(11L, "mfg:task:view")).thenReturn(List.of(account));
        when(recipientMapper.insertIgnore(any())).thenReturn(1);

        service.deliver(5L);

        verify(userMapper).listNotificationRecipients(11L, "mfg:task:view");
        ArgumentCaptor<com.neuedu.ems.entity.sys.SysNotificationRecipient> captor =
                ArgumentCaptor.forClass(com.neuedu.ems.entity.sys.SysNotificationRecipient.class);
        verify(recipientMapper).insertIgnore(captor.capture());
        assertEquals(21L, captor.getValue().getRecipientUserId());
        assertEquals("ASSIGNEE", captor.getValue().getRecipientReason());
        assertEquals("PROCESSED", outbox.getStatus());
    }

    @Test
    void failsRoutingWhenNoAccountHasTargetPermission() {
        when(userMapper.listNotificationRecipients(11L, "mfg:task:view")).thenReturn(List.of());
        when(recipientMapper.selectCount(any())).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> service.deliver(5L));
    }
}
