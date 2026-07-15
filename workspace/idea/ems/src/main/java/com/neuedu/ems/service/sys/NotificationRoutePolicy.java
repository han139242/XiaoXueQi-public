package com.neuedu.ems.service.sys;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class NotificationRoutePolicy {

    private static final Set<String> ALLOWED_ROUTES = Set.of(
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
    );

    public boolean isAllowed(String routePath) {
        return routePath != null && ALLOWED_ROUTES.contains(routePath);
    }
}
