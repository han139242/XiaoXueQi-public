package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.wms.WmsInboundOrder;
import com.neuedu.ems.entity.wms.WmsOutboundOrder;
import com.neuedu.ems.entity.wms.WmsReplenishmentSuggestion;
import com.neuedu.ems.entity.wms.WmsReturnOrder;
import com.neuedu.ems.entity.wms.WmsStocktakeOrder;
import com.neuedu.ems.mapper.wms.WmsInboundOrderMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundOrderMapper;
import com.neuedu.ems.mapper.wms.WmsReplenishmentSuggestionMapper;
import com.neuedu.ems.mapper.wms.WmsReturnOrderMapper;
import com.neuedu.ems.mapper.wms.WmsStocktakeOrderMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mes.notification.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class WmsResponsibilityNotificationScanner {

    private final WmsInboundOrderMapper inboundMapper;
    private final WmsOutboundOrderMapper outboundMapper;
    private final WmsReturnOrderMapper returnMapper;
    private final WmsStocktakeOrderMapper stocktakeMapper;
    private final WmsReplenishmentSuggestionMapper replenishmentMapper;
    private final WmsResponsibilityNotificationService notificationService;

    public WmsResponsibilityNotificationScanner(WmsInboundOrderMapper inboundMapper,
                                                WmsOutboundOrderMapper outboundMapper,
                                                WmsReturnOrderMapper returnMapper,
                                                WmsStocktakeOrderMapper stocktakeMapper,
                                                WmsReplenishmentSuggestionMapper replenishmentMapper,
                                                WmsResponsibilityNotificationService notificationService) {
        this.inboundMapper = inboundMapper;
        this.outboundMapper = outboundMapper;
        this.returnMapper = returnMapper;
        this.stocktakeMapper = stocktakeMapper;
        this.replenishmentMapper = replenishmentMapper;
        this.notificationService = notificationService;
    }

    @Scheduled(initialDelayString = "${mes.notification.wms-scan-initial-delay-ms:25000}",
            fixedDelayString = "${mes.notification.wms-scan-fixed-delay-ms:60000}")
    public void synchronizeOpenResponsibilities() {
        inboundMapper.selectList(new LambdaQueryWrapper<WmsInboundOrder>()
                        .eq(WmsInboundOrder::getDeletedFlag, 0)
                        .eq(WmsInboundOrder::getAuditStatus, BusinessStatus.Document.SUBMITTED)
                        .orderByAsc(WmsInboundOrder::getInboundId).last("LIMIT 500"))
                .forEach(order -> notificationService.publishInboundPending(order, null));
        outboundMapper.selectList(new LambdaQueryWrapper<WmsOutboundOrder>()
                        .eq(WmsOutboundOrder::getDeletedFlag, 0)
                        .eq(WmsOutboundOrder::getAuditStatus, BusinessStatus.Document.SUBMITTED)
                        .orderByAsc(WmsOutboundOrder::getOutboundId).last("LIMIT 500"))
                .forEach(order -> notificationService.publishOutboundPending(order, null));
        returnMapper.selectList(new LambdaQueryWrapper<WmsReturnOrder>()
                        .eq(WmsReturnOrder::getDeletedFlag, 0)
                        .eq(WmsReturnOrder::getAuditStatus, BusinessStatus.Document.SUBMITTED)
                        .orderByAsc(WmsReturnOrder::getReturnId).last("LIMIT 500"))
                .forEach(order -> notificationService.publishReturnPending(order, null));
        stocktakeMapper.selectList(new LambdaQueryWrapper<WmsStocktakeOrder>()
                        .eq(WmsStocktakeOrder::getDeletedFlag, 0)
                        .and(scope -> scope.isNull(WmsStocktakeOrder::getStocktakeStatus)
                                .or().ne(WmsStocktakeOrder::getStocktakeStatus, BusinessStatus.Production.FINISHED))
                        .orderByAsc(WmsStocktakeOrder::getStocktakeId).last("LIMIT 500"))
                .forEach(order -> notificationService.publishStocktakePending(order, null));
        replenishmentMapper.selectList(new LambdaQueryWrapper<WmsReplenishmentSuggestion>()
                        .eq(WmsReplenishmentSuggestion::getDeletedFlag, 0)
                        .eq(WmsReplenishmentSuggestion::getStatus, BusinessStatus.Document.SUBMITTED)
                        .orderByAsc(WmsReplenishmentSuggestion::getSuggestionId).last("LIMIT 500"))
                .forEach(order -> notificationService.publishReplenishmentPending(order, null, null));
    }
}
