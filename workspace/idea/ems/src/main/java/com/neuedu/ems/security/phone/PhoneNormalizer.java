package com.neuedu.ems.security.phone;

import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 用同一规则规范化存储值和精确检索输入，避免同号不同格式产生不同哈希。
 */
@Component
public class PhoneNormalizer {

    private static final Pattern NORMALIZED_PHONE = Pattern.compile("\\+?[0-9]{7,20}");

    public String normalizeForStorage(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }
        String normalized = compact(rawPhone);
        if (!NORMALIZED_PHONE.matcher(normalized).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "联系电话格式不合法");
        }
        return normalized;
    }

    public Optional<String> normalizeForLookup(String rawKeyword) {
        if (rawKeyword == null || rawKeyword.isBlank()) {
            return Optional.empty();
        }
        String normalized = compact(rawKeyword);
        return NORMALIZED_PHONE.matcher(normalized).matches() ? Optional.of(normalized) : Optional.empty();
    }

    private String compact(String rawPhone) {
        return rawPhone.trim().replaceAll("[\\s\\-()（）]", "");
    }
}
