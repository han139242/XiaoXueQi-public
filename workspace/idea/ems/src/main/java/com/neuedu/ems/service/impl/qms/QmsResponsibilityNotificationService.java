package com.neuedu.ems.service.impl.qms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.qms.QmsNonconformingOrder;
import com.neuedu.ems.service.impl.sys.ResponsibilityNotificationSupport;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class QmsResponsibilityNotificationService {

    private static final String QUALITY_DEPARTMENT = "DEPT-QMS";
    private static final String INSPECTION_ROUTE = "/qms/inspection-orders";
    private static final String NCR_ROUTE = "/qms/nonconforming-orders";
    private static final String INSPECTION_VIEW = "qms:inspection-order:view";
    private static final String NCR_VIEW = "qms:nonconforming-order:view";

    private final ResponsibilityNotificationSupport support;

    public QmsResponsibilityNotificationService(ResponsibilityNotificationSupport support) {
        this.support = support;
    }

    public void publishOpenInspection(QmsInspectionOrder order, Long sourceUserId) {
        if (order == null || order.getInspectionId() == null
                || BusinessStatus.Production.FINISHED.equals(order.getStatus())) {
            return;
        }
        Long recipient = order.getInspectorId();
        boolean assigned = recipient != null;
        if (!assigned) {
            recipient = support.departmentManager(QUALITY_DEPARTMENT);
        }
        String eventType = assigned ? "QMS_INSPECTION_ASSIGNED" : "QMS_INSPECTION_UNASSIGNED";
        String reason = assigned ? "ASSIGNEE" : "RESOURCE_OWNER";
        support.publish("QMS_INSPECTION_ORDER:" + order.getInspectionId() + ":" + eventType + ":" + recipient,
                "QMS_INSPECTION_ORDER", order.getInspectionId(), eventType,
                "QMS", "INFO", assigned ? "检验任务已分配" : "检验任务待分配",
                "检验单 " + displayNo(order.getInspectionNo(), order.getInspectionId())
                        + (assigned ? " 等待检验" : " 尚未指定质检员"),
                order.getInspectionNo(), INSPECTION_ROUTE, sourceUserId, false,
                Collections.singletonList(support.target(recipient, reason, INSPECTION_VIEW)));
    }

    public void publishInspectionCompleted(QmsInspectionOrder order, String inspectionResult, Long sourceUserId) {
        if (order == null || order.getInspectionId() == null) {
            return;
        }
        Long creatorEmployeeId = support.employeeIdForUser(order.getCreatedBy());
        support.publish("QMS_INSPECTION_ORDER:" + order.getInspectionId() + ":COMPLETED:" + inspectionResult,
                "QMS_INSPECTION_ORDER", order.getInspectionId(), "QMS_INSPECTION_COMPLETED",
                "QMS", BusinessStatus.Quality.FAIL.equals(inspectionResult) ? "WARNING" : "INFO",
                "检验任务已完成",
                "检验单 " + displayNo(order.getInspectionNo(), order.getInspectionId())
                        + " 检验结果为 " + inspectionResult,
                order.getInspectionNo(), INSPECTION_ROUTE, sourceUserId, false,
                Collections.singletonList(support.target(creatorEmployeeId, "REQUESTER", INSPECTION_VIEW)));
    }

    public void cancelOpenInspection(Long inspectionId, Long sourceUserId) {
        support.cancelEvents("QMS_INSPECTION_ORDER", inspectionId,
                List.of("QMS_INSPECTION_ASSIGNED", "QMS_INSPECTION_UNASSIGNED"), sourceUserId);
    }

    public void publishPendingNonconforming(QmsNonconformingOrder order, Long sourceUserId) {
        if (order == null || order.getNcrId() == null
                || !BusinessStatus.Document.SUBMITTED.equals(order.getApprovalStatus())) {
            return;
        }
        Long managerId = support.departmentManager(QUALITY_DEPARTMENT);
        support.publish("QMS_NONCONFORMING_ORDER:" + order.getNcrId() + ":PENDING:" + managerId,
                "QMS_NONCONFORMING_ORDER", order.getNcrId(), "QMS_NONCONFORMING_PENDING_APPROVAL",
                "QMS", "WARNING", "不合格品处理待审批",
                "处理单 " + displayNo(order.getNcrNo(), order.getNcrId()) + " 等待审批",
                order.getNcrNo(), NCR_ROUTE, sourceUserId, true,
                Collections.singletonList(support.target(managerId, "RESOURCE_OWNER", NCR_VIEW)));
    }

    public void publishNonconformingApprovalResult(QmsNonconformingOrder order, String approvalResult,
                                                   Long sourceUserId) {
        if (order == null || order.getNcrId() == null) {
            return;
        }
        Long creatorEmployeeId = support.employeeIdForUser(order.getCreatedBy());
        support.publish("QMS_NONCONFORMING_ORDER:" + order.getNcrId() + ":APPROVAL:" + approvalResult,
                "QMS_NONCONFORMING_ORDER", order.getNcrId(), "QMS_NONCONFORMING_APPROVAL_RESULT",
                "QMS", BusinessStatus.Document.REJECTED.equals(approvalResult) ? "WARNING" : "INFO",
                BusinessStatus.Document.REJECTED.equals(approvalResult) ? "不合格品处理被驳回" : "不合格品处理已批准",
                "处理单 " + displayNo(order.getNcrNo(), order.getNcrId()) + " 审批结果为 " + approvalResult,
                order.getNcrNo(), NCR_ROUTE, sourceUserId, false,
                Collections.singletonList(support.target(creatorEmployeeId, "REQUESTER", NCR_VIEW)));
    }

    public void cancelPendingNonconforming(Long ncrId, Long sourceUserId) {
        support.cancelEvents("QMS_NONCONFORMING_ORDER", ncrId,
                List.of("QMS_NONCONFORMING_PENDING_APPROVAL"), sourceUserId);
    }

    private String displayNo(String number, Long id) {
        return number == null || number.isBlank() ? "#" + id : number;
    }
}
