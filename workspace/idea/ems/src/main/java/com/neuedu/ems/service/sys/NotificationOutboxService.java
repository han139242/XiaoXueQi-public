package com.neuedu.ems.service.sys;

import com.neuedu.ems.common.NotificationEventPayload;

public interface NotificationOutboxService {
    boolean enqueue(String eventKey, String aggregateType, Long aggregateId,
                    String eventType, NotificationEventPayload payload);
}
