package com.neuedu.ems.service.impl.wms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.wms.WmsInboundOrder;
import com.neuedu.ems.entity.wms.WmsOutboundOrder;
import com.neuedu.ems.entity.wms.WmsReplenishmentSuggestion;
import com.neuedu.ems.entity.wms.WmsReturnOrder;
import com.neuedu.ems.entity.wms.WmsStocktakeOrder;
import com.neuedu.ems.entity.wms.WmsWarehouse;
import com.neuedu.ems.mapper.wms.WmsWarehouseMapper;
import com.neuedu.ems.service.impl.sys.ResponsibilityNotificationSupport;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class WmsResponsibilityNotificationService {

    private static final String WAREHOUSE_DEPARTMENT = "DEPT-WMS";
    private static final String INBOUND_ROUTE = "/wms/inbound-orders";
    private static final String OUTBOUND_ROUTE = "/wms/outbound-orders";
    private static final String RETURN_ROUTE = "/wms/return-orders";
    private static final String STOCKTAKE_ROUTE = "/wms/stocktakes";
    private static final String REPLENISHMENT_ROUTE = "/wms/replenishment";

    private final ResponsibilityNotificationSupport support;
    private final WmsWarehouseMapper warehouseMapper;

    public WmsResponsibilityNotificationService(ResponsibilityNotificationSupport support,
                                                WmsWarehouseMapper warehouseMapper) {
        this.support = support;
        this.warehouseMapper = warehouseMapper;
    }

    public void publishInboundPending(WmsInboundOrder order, Long sourceUserId) {
        if (order == null || order.getInboundId() == null
                || !BusinessStatus.Document.SUBMITTED.equals(order.getAuditStatus())) {
            return;
        }
        publishResourcePending("WMS_INBOUND_ORDER", order.getInboundId(), "WMS_INBOUND_PENDING",
                displayNo(order.getInboundNo(), order.getInboundId()), order.getInboundNo(), order.getWarehouseId(),
                INBOUND_ROUTE, "wms:inbound-order:view", "入库单待处理", sourceUserId, false);
    }

    public void publishInboundCompleted(WmsInboundOrder order, Long sourceUserId) {
        publishRequesterResult("WMS_INBOUND_ORDER", order == null ? null : order.getInboundId(),
                "WMS_INBOUND_COMPLETED", order == null ? null : order.getInboundNo(),
                order == null ? null : order.getCreatedBy(), null, INBOUND_ROUTE,
                "wms:inbound-order:view", "入库单已确认", sourceUserId);
    }

    public void cancelInboundPending(Long inboundId, Long sourceUserId) {
        cancel("WMS_INBOUND_ORDER", inboundId, "WMS_INBOUND_PENDING", sourceUserId);
    }

    public void publishOutboundPending(WmsOutboundOrder order, Long sourceUserId) {
        if (order == null || order.getOutboundId() == null
                || !BusinessStatus.Document.SUBMITTED.equals(order.getAuditStatus())) {
            return;
        }
        publishResourcePending("WMS_OUTBOUND_ORDER", order.getOutboundId(), "WMS_OUTBOUND_PENDING",
                displayNo(order.getOutboundNo(), order.getOutboundId()), order.getOutboundNo(), order.getWarehouseId(),
                OUTBOUND_ROUTE, "wms:outbound-order:view", "出库单待处理", sourceUserId, false);
    }

    public void publishOutboundCompleted(WmsOutboundOrder order, Long sourceUserId) {
        publishRequesterResult("WMS_OUTBOUND_ORDER", order == null ? null : order.getOutboundId(),
                "WMS_OUTBOUND_COMPLETED", order == null ? null : order.getOutboundNo(), null,
                order == null ? null : order.getApplicantId(), OUTBOUND_ROUTE,
                "wms:outbound-order:view", "出库单已确认", sourceUserId);
    }

    public void cancelOutboundPending(Long outboundId, Long sourceUserId) {
        cancel("WMS_OUTBOUND_ORDER", outboundId, "WMS_OUTBOUND_PENDING", sourceUserId);
    }

    public void publishReturnPending(WmsReturnOrder order, Long sourceUserId) {
        if (order == null || order.getReturnId() == null
                || !BusinessStatus.Document.SUBMITTED.equals(order.getAuditStatus())) {
            return;
        }
        Long managerId = support.departmentManager(WAREHOUSE_DEPARTMENT);
        support.publish("WMS_RETURN_ORDER:" + order.getReturnId() + ":PENDING:" + managerId,
                "WMS_RETURN_ORDER", order.getReturnId(), "WMS_RETURN_PENDING", "WMS", "INFO",
                "退料单待处理", "退料单 " + displayNo(order.getReturnNo(), order.getReturnId()) + " 等待确认",
                order.getReturnNo(), RETURN_ROUTE, sourceUserId, false,
                Collections.singletonList(support.target(managerId, "RESOURCE_OWNER", "wms:return-order:view")));
    }

    public void publishReturnCompleted(WmsReturnOrder order, Long sourceUserId) {
        publishRequesterResult("WMS_RETURN_ORDER", order == null ? null : order.getReturnId(),
                "WMS_RETURN_COMPLETED", order == null ? null : order.getReturnNo(), null,
                order == null ? null : order.getApplicantId(), RETURN_ROUTE,
                "wms:return-order:view", "退料单已确认", sourceUserId);
    }

    public void cancelReturnPending(Long returnId, Long sourceUserId) {
        cancel("WMS_RETURN_ORDER", returnId, "WMS_RETURN_PENDING", sourceUserId);
    }

    public void publishStocktakePending(WmsStocktakeOrder order, Long sourceUserId) {
        if (order == null || order.getStocktakeId() == null
                || BusinessStatus.Production.FINISHED.equals(order.getStocktakeStatus())) {
            return;
        }
        publishResourcePending("WMS_STOCKTAKE_ORDER", order.getStocktakeId(), "WMS_STOCKTAKE_PENDING",
                displayNo(order.getStocktakeNo(), order.getStocktakeId()), order.getStocktakeNo(), order.getWarehouseId(),
                STOCKTAKE_ROUTE, "wms:stocktake-order:view", "盘点任务待处理", sourceUserId, true);
    }

    public void publishStocktakeCompleted(WmsStocktakeOrder order, Long sourceUserId) {
        publishRequesterResult("WMS_STOCKTAKE_ORDER", order == null ? null : order.getStocktakeId(),
                "WMS_STOCKTAKE_COMPLETED", order == null ? null : order.getStocktakeNo(),
                order == null ? null : order.getCreatedBy(), null, STOCKTAKE_ROUTE,
                "wms:stocktake-order:view", "盘点任务已完成", sourceUserId);
    }

    public void cancelStocktakePending(Long stocktakeId, Long sourceUserId) {
        cancel("WMS_STOCKTAKE_ORDER", stocktakeId, "WMS_STOCKTAKE_PENDING", sourceUserId);
    }

    public void publishReplenishmentPending(WmsReplenishmentSuggestion suggestion, Long warehouseId,
                                            Long sourceUserId) {
        if (suggestion == null || suggestion.getSuggestionId() == null
                || !BusinessStatus.Document.SUBMITTED.equals(suggestion.getStatus())) {
            return;
        }
        Long recipient = warehouseManager(warehouseId);
        support.publish("WMS_REPLENISHMENT:" + suggestion.getSuggestionId() + ":PENDING:" + recipient,
                "WMS_REPLENISHMENT", suggestion.getSuggestionId(), "WMS_REPLENISHMENT_PENDING",
                "WMS", "WARNING", "补货建议待处理",
                "补货建议 " + displayNo(suggestion.getSuggestionNo(), suggestion.getSuggestionId()) + " 等待确认",
                suggestion.getSuggestionNo(), REPLENISHMENT_ROUTE, sourceUserId, false,
                Collections.singletonList(support.target(recipient, "RESOURCE_OWNER", "wms:replenishment:view")));
    }

    public void cancelReplenishmentPending(Long suggestionId, Long sourceUserId) {
        cancel("WMS_REPLENISHMENT", suggestionId, "WMS_REPLENISHMENT_PENDING", sourceUserId);
    }

    private void publishResourcePending(String aggregateType, Long aggregateId, String eventType,
                                        String displayNo, String bizNo, Long warehouseId, String route,
                                        String permission, String title, Long sourceUserId,
                                        boolean acknowledgmentRequired) {
        Long managerId = warehouseManager(warehouseId);
        support.publish(aggregateType + ":" + aggregateId + ":PENDING:" + warehouseId + ":" + managerId,
                aggregateType, aggregateId, eventType, "WMS", "INFO", title,
                displayNo + " 等待仓库负责人处理", bizNo, route, sourceUserId, acknowledgmentRequired,
                Collections.singletonList(support.target(managerId, "RESOURCE_OWNER", permission)));
    }

    private void publishRequesterResult(String aggregateType, Long aggregateId, String eventType,
                                        String bizNo, Long requesterUserId, Long requesterEmployeeId,
                                        String route, String permission, String title, Long sourceUserId) {
        if (aggregateId == null) {
            return;
        }
        Long recipient = requesterEmployeeId == null
                ? support.employeeIdForUser(requesterUserId) : requesterEmployeeId;
        support.publish(aggregateType + ":" + aggregateId + ":RESULT:" + recipient,
                aggregateType, aggregateId, eventType, "WMS", "INFO", title,
                displayNo(bizNo, aggregateId) + " 已完成处理", bizNo, route, sourceUserId, false,
                Collections.singletonList(support.target(recipient, "REQUESTER", permission)));
    }

    private Long warehouseManager(Long warehouseId) {
        WmsWarehouse warehouse = warehouseId == null ? null : warehouseMapper.selectById(warehouseId);
        if (warehouse != null && !Integer.valueOf(1).equals(warehouse.getDeletedFlag())
                && warehouse.getManagerId() != null) {
            return warehouse.getManagerId();
        }
        return support.departmentManager(WAREHOUSE_DEPARTMENT);
    }

    private void cancel(String aggregateType, Long aggregateId, String eventType, Long sourceUserId) {
        support.cancelEvents(aggregateType, aggregateId, List.of(eventType), sourceUserId);
    }

    private String displayNo(String number, Long id) {
        return number == null || number.isBlank() ? "#" + id : number;
    }
}
