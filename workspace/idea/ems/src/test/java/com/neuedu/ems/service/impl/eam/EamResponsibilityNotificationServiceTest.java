package com.neuedu.ems.service.impl.eam;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.NotificationEventPayload;
import com.neuedu.ems.entity.eam.EamMaintenancePlan;
import com.neuedu.ems.entity.eam.EamRepairOrder;
import com.neuedu.ems.service.impl.sys.ResponsibilityNotificationSupport;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EamResponsibilityNotificationServiceTest {

    private final ResponsibilityNotificationSupport support = mock(ResponsibilityNotificationSupport.class);
    private final EamResponsibilityNotificationService service = new EamResponsibilityNotificationService(support);

    @Test
    void unassignedRepairTargetsEquipmentDepartmentManager() {
        when(support.departmentManager("DEPT-EAM")).thenReturn(51L);
        when(support.target(51L, "RESOURCE_OWNER", "eam:repair-order:view"))
                .thenReturn(new NotificationEventPayload.RecipientTarget(
                        51L, "RESOURCE_OWNER", "eam:repair-order:view"));
        EamRepairOrder order = repairOrder();

        service.publishOpenRepair(order, 12L);

        verify(support).target(51L, "RESOURCE_OWNER", "eam:repair-order:view");
        verify(support).publish(eq("EAM_REPAIR_ORDER:6:EAM_REPAIR_PENDING_ASSIGNMENT:51"), eq("EAM_REPAIR_ORDER"),
                eq(6L), eq("EAM_REPAIR_PENDING_ASSIGNMENT"), eq("EAM"), eq("WARNING"),
                eq("维修单待派工"), contains("REP-6"), eq("REP-6"), eq("/eam/repair-orders"),
                eq(12L), eq(true), anyCollection());
    }

    @Test
    void enabledMaintenancePlanTargetsExplicitResponsibleEmployee() {
        when(support.target(61L, "ASSIGNEE", "eam:maintenance-plan:view"))
                .thenReturn(new NotificationEventPayload.RecipientTarget(
                        61L, "ASSIGNEE", "eam:maintenance-plan:view"));
        EamMaintenancePlan plan = new EamMaintenancePlan();
        plan.setPlanId(7L);
        plan.setResponsibleId(61L);
        plan.setStatus(BusinessStatus.Common.ENABLE);
        when(support.hasOpenEvent("EAM_MAINTENANCE_PLAN", 7L, "EAM_MAINTENANCE_ASSIGNED"))
                .thenReturn(false);

        service.publishMaintenanceAssigned(plan, 13L);

        verify(support).target(61L, "ASSIGNEE", "eam:maintenance-plan:view");
        verify(support).publish(eq("EAM_MAINTENANCE_PLAN:7:ASSIGNED:61:0"), eq("EAM_MAINTENANCE_PLAN"),
                eq(7L), eq("EAM_MAINTENANCE_ASSIGNED"), eq("EAM"), eq("INFO"),
                eq("保养计划已分配"), contains("#7"), eq("7"), eq("/eam/maintenance-plans"),
                eq(13L), eq(false), anyCollection());
    }

    @Test
    void disabledMaintenancePlanDoesNotCreateNotification() {
        EamMaintenancePlan plan = new EamMaintenancePlan();
        plan.setPlanId(7L);
        plan.setResponsibleId(61L);
        plan.setStatus(BusinessStatus.Common.DISABLE);

        service.publishMaintenanceAssigned(plan, 13L);

        verify(support, never()).publish(eq("EAM_MAINTENANCE_PLAN:7:ASSIGNED:61:0"),
                eq("EAM_MAINTENANCE_PLAN"), eq(7L), eq("EAM_MAINTENANCE_ASSIGNED"),
                eq("EAM"), eq("INFO"), eq("保养计划已分配"), contains("#7"), eq("7"),
                eq("/eam/maintenance-plans"), eq(13L), eq(false), anyCollection());
    }

    private EamRepairOrder repairOrder() {
        EamRepairOrder order = new EamRepairOrder();
        order.setRepairId(6L);
        order.setRepairNo("REP-6");
        order.setStatus(BusinessStatus.Repair.SUBMITTED);
        return order;
    }
}
