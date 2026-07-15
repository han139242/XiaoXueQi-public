package com.neuedu.ems.service.impl.eam;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.eam.EamMaintenancePlan;
import com.neuedu.ems.entity.eam.EamRepairOrder;
import com.neuedu.ems.service.impl.sys.ResponsibilityNotificationSupport;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class EamResponsibilityNotificationService {

    private static final String EQUIPMENT_DEPARTMENT = "DEPT-EAM";
    private static final String REPAIR_ROUTE = "/eam/repair-orders";
    private static final String MAINTENANCE_ROUTE = "/eam/maintenance-plans";
    private static final String REPAIR_VIEW = "eam:repair-order:view";
    private static final String MAINTENANCE_VIEW = "eam:maintenance-plan:view";

    private final ResponsibilityNotificationSupport support;

    public EamResponsibilityNotificationService(ResponsibilityNotificationSupport support) {
        this.support = support;
    }

    public void publishOpenRepair(EamRepairOrder order, Long sourceUserId) {
        if (order == null || order.getRepairId() == null || BusinessStatus.Repair.CLOSED.equals(order.getStatus())) {
            return;
        }
        boolean waitingAcceptance = BusinessStatus.Repair.WAIT_ACCEPTANCE.equals(order.getStatus());
        boolean assigned = !waitingAcceptance && order.getRepairerId() != null;
        Long recipient = waitingAcceptance ? order.getReportedBy() : order.getRepairerId();
        if (recipient == null) {
            recipient = support.departmentManager(EQUIPMENT_DEPARTMENT);
        }
        String eventType = waitingAcceptance ? "EAM_REPAIR_WAIT_ACCEPTANCE"
                : assigned ? "EAM_REPAIR_ASSIGNED" : "EAM_REPAIR_PENDING_ASSIGNMENT";
        String title = waitingAcceptance ? "维修单待验收" : assigned ? "维修任务已分配" : "维修单待派工";
        String reason = waitingAcceptance ? "REQUESTER" : assigned ? "ASSIGNEE" : "RESOURCE_OWNER";
        support.publish("EAM_REPAIR_ORDER:" + order.getRepairId() + ":" + eventType + ":" + recipient,
                "EAM_REPAIR_ORDER", order.getRepairId(), eventType, "EAM",
                "WARNING", title,
                "维修单 " + displayNo(order.getRepairNo(), order.getRepairId())
                        + (waitingAcceptance ? " 等待报修人验收" : assigned ? " 等待处理" : " 尚未指定维修人"),
                order.getRepairNo(), REPAIR_ROUTE, sourceUserId, !assigned,
                Collections.singletonList(support.target(recipient, reason, REPAIR_VIEW)));
    }

    public void publishRepairClosed(EamRepairOrder order, Long sourceUserId) {
        if (order == null || order.getRepairId() == null) {
            return;
        }
        support.publish("EAM_REPAIR_ORDER:" + order.getRepairId() + ":CLOSED:" + order.getReportedBy(),
                "EAM_REPAIR_ORDER", order.getRepairId(), "EAM_REPAIR_CLOSED", "EAM", "INFO",
                "维修单已关闭",
                "维修单 " + displayNo(order.getRepairNo(), order.getRepairId()) + " 已完成维修并关闭",
                order.getRepairNo(), REPAIR_ROUTE, sourceUserId, false,
                Collections.singletonList(support.target(order.getReportedBy(), "REQUESTER", REPAIR_VIEW)));
    }

    public void cancelOpenRepair(Long repairId, Long sourceUserId) {
        support.cancelEvents("EAM_REPAIR_ORDER", repairId,
                List.of("EAM_REPAIR_PENDING_ASSIGNMENT", "EAM_REPAIR_ASSIGNED",
                        "EAM_REPAIR_WAIT_ACCEPTANCE"), sourceUserId);
    }

    public void publishMaintenanceAssigned(EamMaintenancePlan plan, Long sourceUserId) {
        if (plan == null || plan.getPlanId() == null || plan.getResponsibleId() == null
                || !BusinessStatus.Common.ENABLE.equals(plan.getStatus())) {
            return;
        }
        if (support.hasOpenEvent("EAM_MAINTENANCE_PLAN", plan.getPlanId(), "EAM_MAINTENANCE_ASSIGNED")) {
            return;
        }
        support.publish("EAM_MAINTENANCE_PLAN:" + plan.getPlanId() + ":ASSIGNED:"
                        + plan.getResponsibleId() + ":" + responsibilityVersion(plan),
                "EAM_MAINTENANCE_PLAN", plan.getPlanId(), "EAM_MAINTENANCE_ASSIGNED",
                "EAM", "INFO", "保养计划已分配",
                "保养计划 #" + plan.getPlanId() + " 已分配，请按计划执行",
                String.valueOf(plan.getPlanId()), MAINTENANCE_ROUTE, sourceUserId, false,
                Collections.singletonList(support.target(plan.getResponsibleId(), "ASSIGNEE", MAINTENANCE_VIEW)));
    }

    public void cancelMaintenanceAssignment(Long planId, Long sourceUserId) {
        support.cancelEvents("EAM_MAINTENANCE_PLAN", planId,
                List.of("EAM_MAINTENANCE_ASSIGNED"), sourceUserId);
    }

    private String displayNo(String number, Long id) {
        return number == null || number.isBlank() ? "#" + id : number;
    }

    private String responsibilityVersion(EamMaintenancePlan plan) {
        if (plan.getUpdatedTime() != null) {
            return plan.getUpdatedTime().toString();
        }
        if (plan.getCreatedTime() != null) {
            return plan.getCreatedTime().toString();
        }
        return String.valueOf(plan.getVersion() == null ? 0 : plan.getVersion());
    }
}
