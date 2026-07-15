package com.neuedu.ems.service.impl.sys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuedu.ems.common.NotificationEventPayload;
import com.neuedu.ems.entity.sys.SysEventOutbox;
import com.neuedu.ems.mapper.sys.SysEventOutboxMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationOutboxServiceImplTest {

    @Test
    void enqueuePersistsStableEventKeyAndReturnsFalseForDuplicate() {
        SysEventOutboxMapper mapper = mock(SysEventOutboxMapper.class);
        NotificationOutboxServiceImpl service = new NotificationOutboxServiceImpl(
                mapper, new ObjectMapper().findAndRegisterModules());
        NotificationEventPayload payload = payload();
        when(mapper.insertIgnore(org.mockito.ArgumentMatchers.any())).thenReturn(1, 0);

        assertTrue(service.enqueue("MFG_TASK:1:ASSIGNED:11", "MFG_TASK", 1L,
                "MFG_TASK_ASSIGNED", payload));
        assertFalse(service.enqueue("MFG_TASK:1:ASSIGNED:11", "MFG_TASK", 1L,
                "MFG_TASK_ASSIGNED", payload));

        ArgumentCaptor<SysEventOutbox> captor = ArgumentCaptor.forClass(SysEventOutbox.class);
        verify(mapper, org.mockito.Mockito.times(2)).insertIgnore(captor.capture());
        assertEquals("PENDING", captor.getAllValues().get(0).getStatus());
        assertEquals("MFG_TASK:1:ASSIGNED:11", captor.getAllValues().get(0).getEventKey());
    }

    private NotificationEventPayload payload() {
        return new NotificationEventPayload("MFG", "INFO", "任务已分配", "TASK-1", "MFG_TASK",
                1L, "TASK-1", "/mfg/tasks", 2L, 11L, LocalDateTime.now(), null, false,
                List.of(new NotificationEventPayload.RecipientTarget(11L, "ASSIGNEE", "mfg:task:view")));
    }
}
