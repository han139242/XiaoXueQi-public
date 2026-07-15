package com.neuedu.ems.service.impl.wms;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.NotificationEventPayload;
import com.neuedu.ems.entity.wms.WmsInboundOrder;
import com.neuedu.ems.entity.wms.WmsOutboundOrder;
import com.neuedu.ems.entity.wms.WmsWarehouse;
import com.neuedu.ems.mapper.wms.WmsWarehouseMapper;
import com.neuedu.ems.service.impl.sys.ResponsibilityNotificationSupport;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsResponsibilityNotificationServiceTest {

    private final ResponsibilityNotificationSupport support = mock(ResponsibilityNotificationSupport.class);
    private final WmsWarehouseMapper warehouseMapper = mock(WmsWarehouseMapper.class);
    private final WmsResponsibilityNotificationService service =
            new WmsResponsibilityNotificationService(support, warehouseMapper);

    @Test
    void inboundPendingTargetsWarehouseManagerWithInboundViewPermission() {
        WmsWarehouse warehouse = new WmsWarehouse();
        warehouse.setWarehouseId(5L);
        warehouse.setManagerId(31L);
        warehouse.setDeletedFlag(0);
        when(warehouseMapper.selectById(5L)).thenReturn(warehouse);
        when(support.target(31L, "RESOURCE_OWNER", "wms:inbound-order:view"))
                .thenReturn(new NotificationEventPayload.RecipientTarget(
                        31L, "RESOURCE_OWNER", "wms:inbound-order:view"));
        WmsInboundOrder order = new WmsInboundOrder();
        order.setInboundId(3L);
        order.setInboundNo("IN-3");
        order.setWarehouseId(5L);
        order.setAuditStatus(BusinessStatus.Document.SUBMITTED);

        service.publishInboundPending(order, 9L);

        verify(support).target(31L, "RESOURCE_OWNER", "wms:inbound-order:view");
        verify(support).publish(eq("WMS_INBOUND_ORDER:3:PENDING:5:31"), eq("WMS_INBOUND_ORDER"),
                eq(3L), eq("WMS_INBOUND_PENDING"), eq("WMS"), eq("INFO"), eq("入库单待处理"),
                contains("IN-3"), eq("IN-3"), eq("/wms/inbound-orders"), eq(9L), eq(false), anyCollection());
    }

    @Test
    void outboundCompletionTargetsApplicantEmployee() {
        when(support.target(41L, "REQUESTER", "wms:outbound-order:view"))
                .thenReturn(new NotificationEventPayload.RecipientTarget(
                        41L, "REQUESTER", "wms:outbound-order:view"));
        WmsOutboundOrder order = new WmsOutboundOrder();
        order.setOutboundId(4L);
        order.setOutboundNo("OUT-4");
        order.setApplicantId(41L);

        service.publishOutboundCompleted(order, 10L);

        verify(support).target(41L, "REQUESTER", "wms:outbound-order:view");
        verify(support).publish(eq("WMS_OUTBOUND_ORDER:4:RESULT:41"), eq("WMS_OUTBOUND_ORDER"),
                eq(4L), eq("WMS_OUTBOUND_COMPLETED"), eq("WMS"), eq("INFO"), eq("出库单已确认"),
                contains("OUT-4"), eq("OUT-4"), eq("/wms/outbound-orders"), eq(10L), eq(false), anyCollection());
    }
}
