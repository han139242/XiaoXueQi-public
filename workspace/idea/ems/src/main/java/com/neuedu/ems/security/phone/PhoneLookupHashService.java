package com.neuedu.ems.security.phone;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * AES-GCM 随机 IV 使密文不可比较，精确电话检索改用独立 HMAC。
 */
@Component
public class PhoneLookupHashService {

    private final PhoneCryptoKeyProvider keyProvider;
    private final PhoneNormalizer phoneNormalizer;

    public PhoneLookupHashService(PhoneCryptoKeyProvider keyProvider, PhoneNormalizer phoneNormalizer) {
        this.keyProvider = keyProvider;
        this.phoneNormalizer = phoneNormalizer;
    }

    public String hashNormalized(String normalizedPhone) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyProvider.getLookupHmacKey(), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(normalizedPhone.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is unavailable", e);
        }
    }

    public Optional<String> hashLookupKeyword(String keyword) {
        return phoneNormalizer.normalizeForLookup(keyword).map(this::hashNormalized);
    }
}
