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
        assertEquals("mfg:task:start",
                resolver.resolve("PUT", "/api/mfg/task/12/start").orElseThrow());
        assertEquals("wms:outbound-order:confirm",
                resolver.resolve("PUT", "/api/wms/outbound-order/9/confirm").orElseThrow());
        assertEquals("qms:inspection-result:batch-save",
                resolver.resolve("POST", "/api/qms/inspection-result/batch").orElseThrow());
        assertEquals("trace:material-usage:reverse-trace",
                resolver.resolve("GET", "/api/trace/material-usage/batch/4").orElseThrow());
    }

    @Test
    void keepsTechnicalAuthEndpointsExemptAndUnknownApiUnmapped() {
        assertTrue(resolver.isPermissionExempt("/api/auth/info"));
        assertTrue(resolver.resolve("GET", "/api/unknown/resource").isEmpty());
        assertTrue(resolver.resolve("HEAD", "/api/sys/user/page").isEmpty());
    }
}
