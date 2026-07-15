package com.neuedu.ems.service.impl.qms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.qms.QmsNonconformingOrder;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.qms.QmsNonconformingOrderMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "mes.notification.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class QmsResponsibilityNotificationScanner {

    private final QmsInspectionOrderMapper inspectionOrderMapper;
    private final QmsNonconformingOrderMapper nonconformingOrderMapper;
    private final QmsResponsibilityNotificationService notificationService;

    public QmsResponsibilityNotificationScanner(QmsInspectionOrderMapper inspectionOrderMapper,
                                                QmsNonconformingOrderMapper nonconformingOrderMapper,
                                                QmsResponsibilityNotificationService notificationService) {
        this.inspectionOrderMapper = inspectionOrderMapper;
        this.nonconformingOrderMapper = nonconformingOrderMapper;
        this.notificationService = notificationService;
    }

    @Scheduled(initialDelayString = "${mes.notification.qms-scan-initial-delay-ms:20000}",
            fixedDelayString = "${mes.notification.qms-scan-fixed-delay-ms:60000}")
    public void synchronizeOpenResponsibilities() {
        List<QmsInspectionOrder> inspections = inspectionOrderMapper.selectList(
                new LambdaQueryWrapper<QmsInspectionOrder>()
                        .eq(QmsInspectionOrder::getDeletedFlag, 0)
                        .in(QmsInspectionOrder::getStatus, BusinessStatus.Document.DRAFT,
                                BusinessStatus.Production.RUNNING)
                        .orderByAsc(QmsInspectionOrder::getInspectionId)
                        .last("LIMIT 500"));
        inspections.forEach(order -> notificationService.publishOpenInspection(order, null));

        List<QmsNonconformingOrder> nonconformingOrders = nonconformingOrderMapper.selectList(
                new LambdaQueryWrapper<QmsNonconformingOrder>()
                        .eq(QmsNonconformingOrder::getDeletedFlag, 0)
                        .eq(QmsNonconformingOrder::getApprovalStatus, BusinessStatus.Document.SUBMITTED)
                        .orderByAsc(QmsNonconformingOrder::getNcrId)
                        .last("LIMIT 500"));
        nonconformingOrders.forEach(order -> notificationService.publishPendingNonconforming(order, null));
    }
}
