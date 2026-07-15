package com.neuedu.ems.service.impl.sys;

import com.neuedu.ems.common.NotificationEventPayload;
import com.neuedu.ems.mapper.org.OrgDepartmentMapper;
import com.neuedu.ems.mapper.sys.SysEventOutboxMapper;
import com.neuedu.ems.mapper.sys.SysNotificationMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.service.sys.NotificationOutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResponsibilityNotificationSupportTest {

    @Test
    void missingRecipientStillWritesAuditableOutboxEvent() {
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        ResponsibilityNotificationSupport support = new ResponsibilityNotificationSupport(
                outboxService, mock(SysUserMapper.class), mock(OrgDepartmentMapper.class),
                mock(SysNotificationMapper.class), mock(SysEventOutboxMapper.class));

        support.publish("QMS:1:MISSING", "QMS_INSPECTION_ORDER", 1L, "QMS_INSPECTION_UNASSIGNED",
                "QMS", "WARNING", "缺少责任人", "等待路由审计", "QI-1",
                "/qms/inspection-orders", null, false, Collections.singletonList(null));

        ArgumentCaptor<NotificationEventPayload> payload = ArgumentCaptor.forClass(NotificationEventPayload.class);
        verify(outboxService).enqueue(eq("QMS:1:MISSING"), eq("QMS_INSPECTION_ORDER"), eq(1L),
                eq("QMS_INSPECTION_UNASSIGNED"), payload.capture());
        assertTrue(payload.getValue().recipients().isEmpty());
    }
}
