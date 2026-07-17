package com.neuedu.ems.security.phone;

import org.springframework.stereotype.Component;

/**
 * 不具备完整查看权限时的最小化电话号码展示规则。
 */
@Component
public class PhoneMasker {

    public String mask(String normalizedPhone) {
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            return null;
        }
        int length = normalizedPhone.length();
        if (length < 7) {
            return "*".repeat(length);
        }
        return normalizedPhone.substring(0, Math.min(3, length)) + "****"
                + normalizedPhone.substring(Math.max(3, length - 3));
    }
}
