package com.neuedu.ems.common;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * MES 业务状态编码。数据库只保存英文编码，中文文本由前端字典负责展示。
 */
public final class BusinessStatus {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,31}$");
    private static final Set<String> APPROVAL_RESULTS = Set.of(Document.APPROVED, Document.REJECTED);
    private static final Set<String> INSPECTION_RESULTS = Set.of(Quality.PASS, Quality.FAIL, Quality.CONCESSION);
    private static final Set<String> DISPOSITIONS = Set.of(
            Quality.REWORK, Quality.SCRAP, Quality.DOWNGRADE, Quality.CONCESSION);

    private BusinessStatus() {
    }

    public static final class Common {
        public static final String ENABLE = "ENABLE";
        public static final String DISABLE = "DISABLE";
        public static final String LOCKED = "LOCKED";

        private Common() {
        }
    }

    public static final class Document {
        public static final String DRAFT = "DRAFT";
        public static final String SUBMITTED = "SUBMITTED";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
        public static final String RELEASED = "RELEASED";
        public static final String CLOSED = "CLOSED";

        private Document() {
        }
    }

    public static final class Workflow {
        public static final String PROCESSING = "PROCESSING";
        public static final String RESOLVED = "RESOLVED";

        private Workflow() {
        }
    }

    public static final class Production {
        public static final String WAIT_START = "WAIT_START";
        public static final String RUNNING = "RUNNING";
        public static final String PAUSED = "PAUSED";
        public static final String FINISHED = "FINISHED";

        private Production() {
        }
    }

    public static final class Quality {
        public static final String PASS = "PASS";
        public static final String FAIL = "FAIL";
        public static final String CONCESSION = "CONCESSION";
        public static final String REWORK = "REWORK";
        public static final String SCRAP = "SCRAP";
        public static final String DOWNGRADE = "DOWNGRADE";

        private Quality() {
        }
    }

    public static final class Equipment {
        public static final String IDLE = "IDLE";
        public static final String RUNNING = "RUNNING";
        public static final String MAINTAINING = "MAINTAINING";
        public static final String REPAIRING = "REPAIRING";
        public static final String STOPPED = "STOPPED";
        public static final String FAULT = "FAULT";
        public static final String SCRAPPED = "SCRAPPED";

        private Equipment() {
        }
    }

    /**
     * 将英文状态规范为大写编码；非英文编码返回 null。
     */
    public static String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return CODE_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    public static boolean isApprovalResult(String value) {
        return value != null && APPROVAL_RESULTS.contains(value);
    }

    public static boolean isInspectionResult(String value) {
        return value != null && INSPECTION_RESULTS.contains(value);
    }

    public static boolean isDisposition(String value) {
        return value != null && DISPOSITIONS.contains(value);
    }
}
