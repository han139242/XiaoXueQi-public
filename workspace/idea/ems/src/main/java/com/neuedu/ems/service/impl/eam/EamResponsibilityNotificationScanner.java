package com.neuedu.ems.service.impl.eam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.eam.EamMaintenancePlan;
import com.neuedu.ems.entity.eam.EamRepairOrder;
import com.neuedu.ems.mapper.eam.EamMaintenancePlanMapper;
import com.neuedu.ems.mapper.eam.EamRepairOrderMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mes.notification.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class EamResponsibilityNotificationScanner {

    private final EamRepairOrderMapper repairOrderMapper;
    private final EamMaintenancePlanMapper maintenancePlanMapper;
    private final EamResponsibilityNotificationService notificationService;

    public EamResponsibilityNotificationScanner(EamRepairOrderMapper repairOrderMapper,
                                                EamMaintenancePlanMapper maintenancePlanMapper,
                                                EamResponsibilityNotificationService notificationService) {
        this.repairOrderMapper = repairOrderMapper;
        this.maintenancePlanMapper = maintenancePlanMapper;
        this.notificationService = notificationService;
    }

    @Scheduled(initialDelayString = "${mes.notification.eam-scan-initial-delay-ms:30000}",
            fixedDelayString = "${mes.notification.eam-scan-fixed-delay-ms:60000}")
    public void synchronizeOpenResponsibilities() {
        repairOrderMapper.selectList(new LambdaQueryWrapper<EamRepairOrder>()
                        .eq(EamRepairOrder::getDeletedFlag, 0)
                        .in(EamRepairOrder::getStatus, BusinessStatus.Repair.SUBMITTED,
                                BusinessStatus.Repair.REPAIRING, BusinessStatus.Repair.WAIT_ACCEPTANCE)
                        .orderByAsc(EamRepairOrder::getRepairId).last("LIMIT 500"))
                .forEach(order -> notificationService.publishOpenRepair(order, null));

        maintenancePlanMapper.selectList(new LambdaQueryWrapper<EamMaintenancePlan>()
                        .eq(EamMaintenancePlan::getDeletedFlag, 0)
                        .eq(EamMaintenancePlan::getStatus, BusinessStatus.Common.ENABLE)
                        .isNotNull(EamMaintenancePlan::getResponsibleId)
                        .orderByAsc(EamMaintenancePlan::getPlanId).last("LIMIT 500"))
                .forEach(plan -> notificationService.publishMaintenanceAssigned(plan, null));
    }
}
