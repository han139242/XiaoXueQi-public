package com.neuedu.ems.service.impl.sys;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuedu.ems.common.NotificationEventPayload;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.sys.SysEventOutbox;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.sys.SysEventOutboxMapper;
import com.neuedu.ems.service.sys.NotificationOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class NotificationOutboxServiceImpl implements NotificationOutboxService {

    private final SysEventOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    public NotificationOutboxServiceImpl(SysEventOutboxMapper outboxMapper, ObjectMapper objectMapper) {
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean enqueue(String eventKey, String aggregateType, Long aggregateId,
                           String eventType, NotificationEventPayload payload) {
        if (!StringUtils.hasText(eventKey) || eventKey.length() > 191
                || !StringUtils.hasText(aggregateType) || !StringUtils.hasText(eventType) || payload == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "通知事件参数不完整");
        }
        LocalDateTime now = payload.occurredTime() == null ? LocalDateTime.now() : payload.occurredTime();
        SysEventOutbox outbox = new SysEventOutbox();
        outbox.setEventKey(eventKey);
        outbox.setAggregateType(aggregateType);
        outbox.setAggregateId(aggregateId);
        outbox.setEventType(eventType);
        outbox.setPayloadJson(serialize(payload));
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setOccurredTime(now);
        outbox.setVersion(0);
        outbox.setCreatedBy(payload.sourceUserId());
        outbox.setCreatedTime(now);
        outbox.setUpdatedBy(payload.sourceUserId());
        outbox.setUpdatedTime(now);
        outbox.setDeletedFlag(0);
        return outboxMapper.insertIgnore(outbox) > 0;
    }

    private String serialize(NotificationEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAILED, e);
        }
    }
}
