package com.neuedu.ems.service.sys;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationRoutePolicyTest {

    private final NotificationRoutePolicy policy = new NotificationRoutePolicy();

    @Test
    void allowsOnlyRegisteredBusinessRoutes() {
        List.of(
                "/mfg/tasks",
                "/mfg/work-reports",
                "/qms/inspection-orders",
                "/qms/nonconforming-orders",
                "/wms/inbound-orders",
                "/wms/outbound-orders",
                "/wms/return-orders",
                "/wms/stocktakes",
                "/wms/replenishment",
                "/eam/repair-orders",
                "/eam/maintenance-plans"
        ).forEach(route -> assertTrue(policy.isAllowed(route), route));

        assertFalse(policy.isAllowed("https://example.com"));
        assertFalse(policy.isAllowed("/sys/users"));
        assertFalse(policy.isAllowed(null));
    }
}
