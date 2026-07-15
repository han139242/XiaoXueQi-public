package com.neuedu.ems.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionCodeResolverTest {

    private final PermissionCodeResolver resolver = new PermissionCodeResolver();

    @Test
    void resolvesGenericCrudAndExportPermissions() {
        assertEquals("sys:user:view", resolver.resolve("GET", "/api/sys/user/page").orElseThrow());
        assertEquals("mdm:product:create", resolver.resolve("POST", "/api/mdm/product").orElseThrow());
        assertEquals("mfg:task:update", resolver.resolve("PUT", "/api/mfg/task").orElseThrow());
        assertEquals("qms:inspection-item:delete", resolver.resolve("DELETE", "/api/qms/inspection-item/7").orElseThrow());
        assertEquals("wms:inventory:export", resolver.resolve("GET", "/api/wms/inventory/excel").orElseThrow());
        assertEquals("wms:replenishment:view",
                resolver.resolve("GET", "/api/wms/replenishment-suggestion/page").orElseThrow());
    }

    @Test
    void resolvesHighRiskActionsBeforeGenericMethodMapping() {
        assertEquals("sys:role:assign-menu",
                resolver.resolve("PUT", "/api/sys/role/3/menus").orElseThrow());
        assertEquals("sys:notification:read",
                resolver.resolve("PUT", "/api/sys/notification/9/read").orElseThrow());
        assertEquals("sys:notification:read",
                resolver.resolve("PUT", "/api/sys/notification/read-all").orElseThrow());
        assertEquals("sys:notification:ack",
                resolver.resolve("PUT", "/api/sys/notification/9/ack").orElseThrow());
        assertEquals("sys:notification:audit",
                resolver.resolve("GET", "/api/sys/notification/audit/page").orElseThrow());
        assertEquals("mfg:task:start",
                resolver.resolve("PUT", "/api/mfg/task/12/start").orElseThrow());
        assertEquals("wms:outbound-order:confirm",
                resolver.resolve("PUT", "/api/wms/outbound-order/9/confirm").orElseThrow());
        assertEquals("qms:inspection-result:batch-save",
                resolver.resolve("POST", "/api/qms/inspection-result/batch").orElseThrow());
        assertEquals("trace:material-usage:reverse-trace",
                resolver.resolve("GET", "/api/trace/material-usage/batch/4").orElseThrow());
        assertEquals("mfg:schedule-scenario:validate",
                resolver.resolve("POST", "/api/mfg/schedule-scenario/5/validate").orElseThrow());
        assertEquals("mfg:schedule-scenario:ai-advice",
                resolver.resolve("POST", "/api/mfg/schedule-scenario/5/orders/8/ai-advice").orElseThrow());
        assertEquals("mfg:schedule-scenario:confirm",
                resolver.resolve("PUT", "/api/mfg/schedule-scenario/5/confirm").orElseThrow());
        assertEquals("mfg:production-order:release",
                resolver.resolve("PUT", "/api/mfg/production-order/5/release").orElseThrow());
        assertEquals("mfg:production-order:withdraw",
                resolver.resolve("PUT", "/api/mfg/production-order/5/withdraw").orElseThrow());
        assertEquals("mfg:production-plan:withdraw",
                resolver.resolve("PUT", "/api/mfg/production-plan/5/withdraw").orElseThrow());
        assertEquals("mfg:work-order:withdraw",
                resolver.resolve("PUT", "/api/mfg/work-order/6/withdraw").orElseThrow());
        assertEquals("mfg:dispatch-order:withdraw",
                resolver.resolve("PUT", "/api/mfg/dispatch-order/7/withdraw").orElseThrow());
        assertEquals("mfg:task:withdraw",
                resolver.resolve("PUT", "/api/mfg/task/8/withdraw").orElseThrow());
        assertEquals("mfg:work-report:withdraw",
                resolver.resolve("PUT", "/api/mfg/work-report/9/withdraw").orElseThrow());
        assertEquals("wms:outbound-order:withdraw",
                resolver.resolve("PUT", "/api/wms/outbound-order/10/withdraw").orElseThrow());
        assertEquals("qms:inspection-order:withdraw",
                resolver.resolve("PUT", "/api/qms/inspection-order/11/withdraw").orElseThrow());
        assertEquals("mfg:production-plan:generate",
                resolver.resolve("POST", "/api/mfg/production-plan/generate/5").orElseThrow());
        assertEquals("mfg:production-plan:release",
                resolver.resolve("PUT", "/api/mfg/production-plan/5/release").orElseThrow());
        assertEquals("mfg:work-order:generate",
                resolver.resolve("POST", "/api/mfg/work-order/generate/6").orElseThrow());
        assertEquals("mfg:work-order:release",
                resolver.resolve("PUT", "/api/mfg/work-order/6/release").orElseThrow());
        assertEquals("mfg:dispatch-order:generate",
                resolver.resolve("POST", "/api/mfg/dispatch-order/generate/6").orElseThrow());
        assertEquals("mfg:dispatch-order:release",
                resolver.resolve("PUT", "/api/mfg/dispatch-order/7/release").orElseThrow());
        assertEquals("mfg:work-report:submit",
                resolver.resolve("POST", "/api/mfg/work-report").orElseThrow());
        assertEquals("mfg:work-report:submit",
                resolver.resolve("POST", "/api/mfg/work-report/full").orElseThrow());
    }

    @Test
    void separatesManualSchedulingViewAndMutationPermissions() {
        assertEquals("mfg:schedule-scenario:view",
                resolver.resolve("GET", "/api/mfg/schedule-scenario/page").orElseThrow());
        assertEquals("mfg:schedule-scenario:create",
                resolver.resolve("POST", "/api/mfg/schedule-scenario/manual-draft/open").orElseThrow());
        assertEquals("mfg:schedule-scenario:view",
                resolver.resolve("GET", "/api/mfg/schedule-scenario/5/orders/8/eligible-lines").orElseThrow());
        assertEquals("mfg:schedule-scenario:create",
                resolver.resolve("POST", "/api/mfg/schedule-scenario/5/items").orElseThrow());
        assertEquals("mfg:schedule-scenario:update",
                resolver.resolve("PUT", "/api/mfg/schedule-scenario/5/items/9/placement").orElseThrow());
        assertEquals("mfg:schedule-scenario:delete",
                resolver.resolve("DELETE", "/api/mfg/schedule-scenario/5/items/9").orElseThrow());
    }

    @Test
    void taskReminderSummaryUsesTaskViewPermission() {
        assertEquals("mfg:task:view",
                resolver.resolve("GET", "/api/mfg/task/reminder-summary").orElseThrow());
    }

    @Test
    void keepsTechnicalAuthEndpointsExemptAndUnknownApiUnmapped() {
        assertTrue(resolver.isPermissionExempt("/api/auth/info"));
        assertTrue(resolver.resolve("GET", "/api/unknown/resource").isEmpty());
        assertTrue(resolver.resolve("HEAD", "/api/sys/user/page").isEmpty());
    }
}
