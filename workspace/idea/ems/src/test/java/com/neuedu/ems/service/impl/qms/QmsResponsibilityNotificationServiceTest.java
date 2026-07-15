package com.neuedu.ems.service.impl.qms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.NotificationEventPayload;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.qms.QmsNonconformingOrder;
import com.neuedu.ems.service.impl.sys.ResponsibilityNotificationSupport;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QmsResponsibilityNotificationServiceTest {

    private final ResponsibilityNotificationSupport support = mock(ResponsibilityNotificationSupport.class);
    private final QmsResponsibilityNotificationService service = new QmsResponsibilityNotificationService(support);

    @Test
    void assignedInspectionTargetsOnlyInspectorWithViewPermission() {
        NotificationEventPayload.RecipientTarget target =
                new NotificationEventPayload.RecipientTarget(11L, "ASSIGNEE", "qms:inspection-order:view");
        when(support.target(11L, "ASSIGNEE", "qms:inspection-order:view")).thenReturn(target);
        QmsInspectionOrder order = new QmsInspectionOrder();
        order.setInspectionId(1L);
        order.setInspectionNo("QI-1");
        order.setInspectorId(11L);
        order.setStatus(BusinessStatus.Document.DRAFT);

        service.publishOpenInspection(order, 7L);

        verify(support).target(11L, "ASSIGNEE", "qms:inspection-order:view");
        verify(support).publish(eq("QMS_INSPECTION_ORDER:1:QMS_INSPECTION_ASSIGNED:11"), eq("QMS_INSPECTION_ORDER"),
                eq(1L), eq("QMS_INSPECTION_ASSIGNED"), eq("QMS"), eq("INFO"),
                eq("检验任务已分配"), contains("QI-1"), eq("QI-1"),
                eq("/qms/inspection-orders"), eq(7L), eq(false), anyCollection());
    }

    @Test
    void pendingNonconformingTargetsQualityDepartmentManager() {
        when(support.departmentManager("DEPT-QMS")).thenReturn(21L);
        when(support.target(21L, "RESOURCE_OWNER", "qms:nonconforming-order:view"))
                .thenReturn(new NotificationEventPayload.RecipientTarget(
                        21L, "RESOURCE_OWNER", "qms:nonconforming-order:view"));
        QmsNonconformingOrder order = new QmsNonconformingOrder();
        order.setNcrId(2L);
        order.setNcrNo("NCR-2");
        order.setApprovalStatus(BusinessStatus.Document.SUBMITTED);

        service.publishPendingNonconforming(order, null);

        verify(support).target(21L, "RESOURCE_OWNER", "qms:nonconforming-order:view");
        verify(support).publish(eq("QMS_NONCONFORMING_ORDER:2:PENDING:21"),
                eq("QMS_NONCONFORMING_ORDER"), eq(2L), eq("QMS_NONCONFORMING_PENDING_APPROVAL"),
                eq("QMS"), eq("WARNING"), eq("不合格品处理待审批"), contains("NCR-2"),
                eq("NCR-2"), eq("/qms/nonconforming-orders"), eq(null), eq(true), anyCollection());
    }
}
