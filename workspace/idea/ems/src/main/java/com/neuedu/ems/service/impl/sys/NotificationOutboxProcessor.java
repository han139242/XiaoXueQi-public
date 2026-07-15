package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neuedu.ems.entity.sys.SysEventOutbox;
import com.neuedu.ems.mapper.sys.SysEventOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(name = "mes.notification.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class NotificationOutboxProcessor {

    private final SysEventOutboxMapper outboxMapper;
    private final NotificationOutboxDeliveryService deliveryService;

    @Value("${mes.notification.outbox-batch-size:50}")
    private int batchSize;

    @Value("${mes.notification.outbox-max-retries:5}")
    private int maxRetries;

    public NotificationOutboxProcessor(SysEventOutboxMapper outboxMapper,
                                       NotificationOutboxDeliveryService deliveryService) {
        this.outboxMapper = outboxMapper;
        this.deliveryService = deliveryService;
    }

    @Scheduled(initialDelayString = "${mes.notification.outbox-initial-delay-ms:10000}",
            fixedDelayString = "${mes.notification.outbox-fixed-delay-ms:5000}")
    public void processPending() {
        LocalDateTime now = LocalDateTime.now();
        outboxMapper.resetStale(now.minusMinutes(5), now);
        List<SysEventOutbox> candidates = outboxMapper.selectList(new LambdaQueryWrapper<SysEventOutbox>()
                .eq(SysEventOutbox::getDeletedFlag, 0)
                .in(SysEventOutbox::getStatus, "PENDING", "FAILED")
                .lt(SysEventOutbox::getRetryCount, Math.max(1, maxRetries))
                .and(scope -> scope.isNull(SysEventOutbox::getNextRetryTime)
                        .or().le(SysEventOutbox::getNextRetryTime, now))
                .orderByAsc(SysEventOutbox::getCreatedTime)
                .last("LIMIT " + Math.min(Math.max(batchSize, 1), 200)));
        for (SysEventOutbox candidate : candidates) {
            if (outboxMapper.claim(candidate.getOutboxId(), now, Math.max(1, maxRetries)) == 0) {
                continue;
            }
            try {
                deliveryService.deliver(candidate.getOutboxId());
            } catch (RuntimeException e) {
                log.warn("Notification outbox delivery failed: outboxId={}, reason={}",
                        candidate.getOutboxId(), e.getMessage());
                deliveryService.markFailure(candidate.getOutboxId(), e.getMessage(), Math.max(1, maxRetries));
            }
        }
    }
}
