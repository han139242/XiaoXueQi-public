package com.neuedu.ems.security.phone;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 仅从受控环境变量加载电话字段密钥，并在应用启动阶段完成格式校验。
 */
@Component
public class PhoneCryptoKeyProvider {

    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_-]{1,32}");
    private static final int AES_256_BYTES = 32;
    private static final int MIN_HMAC_BYTES = 32;

    private final String activeKeyId;
    private final Map<String, SecretKey> encryptionKeys;
    private final byte[] lookupHmacKey;
    private final boolean legacyReadEnabled;

    public PhoneCryptoKeyProvider(Environment environment) {
        activeKeyId = require(environment, "PHONE_CRYPTO_ACTIVE_KEY_ID");
        validateKeyId(activeKeyId);

        Set<String> keyIds = parseKeyIds(require(environment, "PHONE_CRYPTO_KEY_IDS"));
        if (!keyIds.contains(activeKeyId)) {
            throw new IllegalStateException("PHONE_CRYPTO_KEY_IDS must include PHONE_CRYPTO_ACTIVE_KEY_ID");
        }

        Map<String, SecretKey> keys = new LinkedHashMap<>();
        for (String keyId : keyIds) {
            String encoded = require(environment, encryptionKeyVariable(keyId));
            byte[] bytes = decodeBase64(encoded, encryptionKeyVariable(keyId));
            if (bytes.length != AES_256_BYTES) {
                throw new IllegalStateException(encryptionKeyVariable(keyId) + " must decode to 32 bytes");
            }
            keys.put(keyId, new SecretKeySpec(bytes, "AES"));
        }
        encryptionKeys = Map.copyOf(keys);

        lookupHmacKey = decodeBase64(require(environment, "PHONE_LOOKUP_HMAC_KEY"), "PHONE_LOOKUP_HMAC_KEY");
        if (lookupHmacKey.length < MIN_HMAC_BYTES) {
            throw new IllegalStateException("PHONE_LOOKUP_HMAC_KEY must decode to at least 32 bytes");
        }
        legacyReadEnabled = environment.getProperty("PHONE_CRYPTO_LEGACY_READ_ENABLED", Boolean.class, false);
    }

    public String getActiveKeyId() {
        return activeKeyId;
    }

    public SecretKey getActiveEncryptionKey() {
        return encryptionKeys.get(activeKeyId);
    }

    public SecretKey getEncryptionKey(String keyId) {
        return encryptionKeys.get(keyId);
    }

    public byte[] getLookupHmacKey() {
        return lookupHmacKey.clone();
    }

    public boolean isLegacyReadEnabled() {
        return legacyReadEnabled;
    }

    private static String require(Environment environment, String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for telephone encryption");
        }
        return value.trim();
    }

    private static Set<String> parseKeyIds(String rawKeyIds) {
        Set<String> keyIds = new LinkedHashSet<>();
        for (String candidate : rawKeyIds.split(",")) {
            String keyId = candidate.trim();
            validateKeyId(keyId);
            keyIds.add(keyId);
        }
        if (keyIds.isEmpty()) {
            throw new IllegalStateException("PHONE_CRYPTO_KEY_IDS must not be empty");
        }
        return keyIds;
    }

    private static void validateKeyId(String keyId) {
        if (!KEY_ID_PATTERN.matcher(keyId).matches()) {
            throw new IllegalStateException("PHONE_CRYPTO key id is invalid");
        }
    }

    private static String encryptionKeyVariable(String keyId) {
        return "PHONE_CRYPTO_KEY_" + keyId.toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private static byte[] decodeBase64(String encoded, String variableName) {
        try {
            return Base64.getDecoder().decode(encoded.getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(variableName + " must be valid Base64", e);
        }
    }
}
