package com.neuedu.ems.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 将实际 API 路径和 HTTP 方法解析为 sys_menu.permission_code。
 */
@Component
public class PermissionCodeResolver {

    private static final Set<String> PERMISSION_EXEMPT_PATHS = Set.of(
            "/api/auth/info", "/api/auth/logout", "/api/hello");

    private static final Set<String> DOMAINS = Set.of(
            "sys", "org", "mdm", "process", "factory", "mfg", "wms", "qms", "eam", "trace");

    private static final Map<String, String> RESOURCE_ALIASES = Map.of(
            "wms/replenishment-suggestion", "replenishment");

    private static final List<SpecialRule> SPECIAL_RULES = List.of(
            rule("PUT", "/api/sys/user/[^/]+/reset-password", "sys:user:reset-password"),
            rule("PUT", "/api/sys/user/[^/]+/roles", "sys:user:assign-role"),
            rule("PUT", "/api/sys/role/[^/]+/menus", "sys:role:assign-menu"),
            rule("PUT", "/api/sys/notification/read-all", "sys:notification:read"),
            rule("PUT", "/api/sys/notification/[^/]+/read", "sys:notification:read"),
            rule("PUT", "/api/sys/notification/[^/]+/ack", "sys:notification:ack"),
            rule("GET", "/api/sys/notification/audit/page", "sys:notification:audit"),
            rule("POST", "/api/sys/file/upload", "sys:file:upload"),
            rule("GET", "/api/sys/file/download/[^/]+", "sys:file:download"),
            rule("POST", "/api/mfg/andon-alert/trigger", "mfg:andon-alert:trigger"),
            rule("PUT", "/api/mfg/andon-alert/[^/]+/close", "mfg:andon-alert:close"),
            rule("PUT", "/api/mfg/exception/[^/]+/handle", "mfg:exception:handle"),
            rule("PUT", "/api/mfg/production-order/[^/]+/release", "mfg:production-order:release"),
            rule("PUT", "/api/mfg/production-order/[^/]+/withdraw", "mfg:production-order:withdraw"),
            rule("POST", "/api/mfg/production-plan/generate/[^/]+", "mfg:production-plan:generate"),
            rule("PUT", "/api/mfg/production-plan/[^/]+/release", "mfg:production-plan:release"),
            rule("PUT", "/api/mfg/production-plan/[^/]+/withdraw", "mfg:production-plan:withdraw"),
            rule("POST", "/api/mfg/work-order/generate/[^/]+", "mfg:work-order:generate"),
            rule("POST", "/api/mfg/work-order/[^/]+/generate-material-issue", "mfg:work-order:material-issue"),
            rule("PUT", "/api/mfg/work-order/[^/]+/release", "mfg:work-order:release"),
            rule("PUT", "/api/mfg/work-order/[^/]+/withdraw", "mfg:work-order:withdraw"),
            rule("POST", "/api/mfg/dispatch-order/generate/[^/]+", "mfg:dispatch-order:generate"),
            rule("PUT", "/api/mfg/dispatch-order/[^/]+/release", "mfg:dispatch-order:release"),
            rule("PUT", "/api/mfg/dispatch-order/[^/]+/withdraw", "mfg:dispatch-order:withdraw"),
            rule("PUT", "/api/mfg/work-order/[^/]+/start", "mfg:work-order:start"),
            rule("PUT", "/api/mfg/task/[^/]+/start", "mfg:task:start"),
            rule("PUT", "/api/mfg/task/[^/]+/withdraw", "mfg:task:withdraw"),
            rule("PUT", "/api/mfg/task/[^/]+/finish", "mfg:task:finish"),
            rule("POST", "/api/mfg/work-report", "mfg:work-report:submit"),
            rule("POST", "/api/mfg/work-report/submit", "mfg:work-report:submit"),
            rule("POST", "/api/mfg/work-report/full", "mfg:work-report:submit"),
            rule("PUT", "/api/mfg/work-report/[^/]+/audit", "mfg:work-report:audit"),
            rule("PUT", "/api/mfg/work-report/[^/]+/withdraw", "mfg:work-report:withdraw"),
            rule("POST", "/api/mfg/schedule-scenario/[^/]+/validate", "mfg:schedule-scenario:validate"),
            rule("POST", "/api/mfg/schedule-scenario/[^/]+/orders/[^/]+/ai-advice", "mfg:schedule-scenario:ai-advice"),
            rule("POST", "/api/mfg/schedule-scenario/[^/]+/ai-advice", "mfg:schedule-scenario:ai-advice"),
            rule("POST", "/api/mfg/schedule-scenario/manual-board/ai-advice", "mfg:schedule-scenario:ai-advice"),
            rule("PUT", "/api/mfg/schedule-scenario/[^/]+/confirm", "mfg:schedule-scenario:confirm"),
            rule("PUT", "/api/mfg/schedule-scenario/[^/]+/items/[^/]+/confirm", "mfg:schedule-scenario:confirm"),
            rule("POST", "/api/wms/inbound-order/generate-production/[^/]+", "wms:inbound-order:create"),
            rule("PUT", "/api/wms/inbound-order/[^/]+/confirm", "wms:inbound-order:confirm"),
            rule("PUT", "/api/wms/inbound-order/[^/]+/withdraw", "wms:inbound-order:withdraw"),
            rule("PUT", "/api/wms/outbound-order/[^/]+/confirm", "wms:outbound-order:confirm"),
            rule("PUT", "/api/wms/outbound-order/[^/]+/withdraw", "wms:outbound-order:withdraw"),
            rule("PUT", "/api/wms/return-order/[^/]+/confirm", "wms:return-order:confirm"),
            rule("PUT", "/api/wms/return-order/[^/]+/withdraw", "wms:return-order:withdraw"),
            rule("PUT", "/api/wms/stocktake-order/[^/]+/finish", "wms:stocktake-order:finish"),
            rule("POST", "/api/wms/replenishment-suggestion/generate", "wms:replenishment:generate"),
            rule("PUT", "/api/qms/inspection-order/[^/]+/submit", "qms:inspection-order:submit"),
            rule("PUT", "/api/qms/inspection-order/[^/]+/withdraw", "qms:inspection-order:withdraw"),
            rule("POST", "/api/qms/inspection-result/batch", "qms:inspection-result:batch-save"),
            rule("PUT", "/api/qms/nonconforming-order/[^/]+/approve", "qms:nonconforming-order:approve"),
            rule("PUT", "/api/eam/repair-order/[^/]+/assign", "eam:repair-order:assign"),
            rule("PUT", "/api/eam/repair-order/[^/]+/close", "eam:repair-order:close"),
            rule("POST", "/api/trace/barcode/generate", "trace:barcode:generate"),
            rule("GET", "/api/trace/barcode/scan/[^/]+", "trace:barcode:scan"),
            rule("GET", "/api/trace/product-lot/lot-no/[^/]+", "trace:product-lot:trace"),
            rule("GET", "/api/trace/material-usage/batch/[^/]+", "trace:material-usage:reverse-trace")
    );

    public Optional<String> resolve(HttpServletRequest request) {
        return resolve(request.getMethod(), request.getRequestURI());
    }

    public Optional<String> resolve(String method, String requestUri) {
        String path = normalize(requestUri);
        for (SpecialRule rule : SPECIAL_RULES) {
            if (rule.method().equalsIgnoreCase(method) && rule.path().matcher(path).matches()) {
                return Optional.of(rule.permissionCode());
            }
        }
        if (path.equals("/api/ai") || path.startsWith("/api/ai/")) {
            return Optional.of("ai:assistant:use");
        }
        if (path.equals("/api/dashboard") || path.startsWith("/api/dashboard/")) {
            return Optional.of("dashboard:summary:view");
        }

        java.util.regex.Matcher matcher = Pattern.compile("^/api/([a-z]+)/([a-z0-9-]+)(?:/.*)?$").matcher(path);
        if (!matcher.matches() || !DOMAINS.contains(matcher.group(1))) {
            return Optional.empty();
        }
        String domain = matcher.group(1);
        String resource = RESOURCE_ALIASES.getOrDefault(domain + "/" + matcher.group(2), matcher.group(2));
        String action = resolveAction(method, path);
        return action == null ? Optional.empty() : Optional.of(domain + ":" + resource + ":" + action);
    }

    public boolean isPermissionExempt(String requestUri) {
        return PERMISSION_EXEMPT_PATHS.contains(normalize(requestUri));
    }

    private String resolveAction(String method, String path) {
        if ("GET".equalsIgnoreCase(method)) {
            return path.endsWith("/excel") ? "export" : "view";
        }
        if ("POST".equalsIgnoreCase(method)) {
            return "create";
        }
        if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return "update";
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return "delete";
        }
        return null;
    }

    private String normalize(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "";
        }
        String path = requestUri.split("\\?", 2)[0];
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private static SpecialRule rule(String method, String pathPattern, String permissionCode) {
        return new SpecialRule(method, Pattern.compile("^" + pathPattern + "$"), permissionCode);
    }

    private record SpecialRule(String method, Pattern path, String permissionCode) {
    }
}
