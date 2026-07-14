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
            "mfg:line-calendar-rule:view",
            "mfg:schedule-scenario:view",
            "mfg:schedule-scenario:create",
            "mfg:schedule-scenario:update",
            "mfg:schedule-scenario:delete",
            "mfg:schedule-scenario:validate",
            "mfg:schedule-scenario:ai-advice",
            "mfg:schedule-scenario:confirm");

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

    private boolean baselineContainsGeneratedPermission(String baseline, String permission) {
        String resource = permission.substring("mfg:".length(), permission.lastIndexOf(':'));
        String action = permission.substring(permission.lastIndexOf(':') + 1);
        return baseline.contains("'mfg-" + resource + "'")
                && List.of("view", "create", "update", "delete").contains(action);
    }

    private String readResource(String path) throws IOException {
        var stream = PermissionCatalogCoverageTest.class.getResourceAsStream(path);
        assertNotNull(stream, path + " not found");
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
