package com.neuedu.ems.vo.sys;

import java.util.Map;

public record NotificationUnreadSummary(long total, Map<String, Long> categories) {
}
