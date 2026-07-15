package com.neuedu.ems.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionCatalogCoverageTest {

    private static final List<String> SCHEDULING_PERMISSIONS = List.of(
            "mfg:line-product-capability:view",
            "mfg:line-product-capability:create",
            "mfg:line-product-capability:update",
            "mfg:line-product-capability:delete",
            "mfg:line-product-capability:export",
            "mfg:line-calendar-rule:view",
            "mfg:line-calendar-rule:create",
            "mfg:line-calendar-rule:update",
            "mfg:line-calendar-rule:delete",
            "mfg:line-calendar-rule:export",
            "mfg:schedule-scenario:view",
            "mfg:schedule-scenario:create",
            "mfg:schedule-scenario:update",
            "mfg:schedule-scenario:delete",
            "mfg:schedule-scenario:validate",
            "mfg:schedule-scenario:ai-advice",
            "mfg:schedule-scenario:confirm");

    private static final List<String> PRODUCTION_FLOW_PERMISSIONS = List.of(
            "mfg:production-order:release",
            "mfg:production-plan:generate",
            "mfg:work-order:generate",
            "mfg:work-order:release");

    @Test
    void schedulingPermissionsExistInBaselineAndRepairMigration() throws IOException {
        String baseline = readResource("/db/renix_enterprise_system_baseline.sql");
        String repair = readResource("/db/rbac_permission_repair_v2.sql");

        for (String permission : SCHEDULING_PERMISSIONS) {
            assertTrue(baseline.contains(permission) || baselineContainsGeneratedPermission(baseline, permission),
                    () -> permission + " missing from enterprise baseline");
            assertTrue(repair.contains(permission), () -> permission + " missing from repair migration");
        }
    }

    @Test
    void productionFlowPermissionsExistInBaselineAndRepairMigration() throws IOException {
        String baseline = readResource("/db/renix_enterprise_system_baseline.sql");
        String repair = readResource("/db/p0_production_flow_permission_repair.sql");

        for (String permission : PRODUCTION_FLOW_PERMISSIONS) {
            assertTrue(baseline.contains(permission), () -> permission + " missing from enterprise baseline");
            assertTrue(repair.contains(permission), () -> permission + " missing from P0 repair migration");
        }
    }

    private boolean baselineContainsGeneratedPermission(String baseline, String permission) {
        String resource = permission.substring("mfg:".length(), permission.lastIndexOf(':'));
        String action = permission.substring(permission.lastIndexOf(':') + 1);
        return baseline.contains("'mfg-" + resource + "'")
                && List.of("view", "create", "update", "delete", "export").contains(action);
    }

    private String readResource(String path) throws IOException {
        var stream = PermissionCatalogCoverageTest.class.getResourceAsStream(path);
        assertNotNull(stream, path + " not found");
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
