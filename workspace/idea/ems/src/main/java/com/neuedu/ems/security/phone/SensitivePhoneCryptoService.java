package com.neuedu.ems.security.phone;

import com.neuedu.ems.exception.SensitivePhoneCryptoException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 电话密文信封，负责版本、密钥路由和认证标签校验。
 */
@Component
public class SensitivePhoneCryptoService {

    private static final String VERSION = "v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PhoneCryptoKeyProvider keyProvider;

    public SensitivePhoneCryptoService(PhoneCryptoKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public String encrypt(String normalizedPhone, SensitivePhoneField field) {
        if (normalizedPhone == null) {
            return null;
        }
        byte[] iv = new byte[IV_BYTES];
        SECURE_RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keyProvider.getActiveEncryptionKey(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(field.aad().getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(normalizedPhone.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return VERSION + ":" + keyProvider.getActiveKeyId() + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new SensitivePhoneCryptoException(e);
        }
    }

    public String decrypt(String storedValue, SensitivePhoneField field) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }
        if (!isEnvelope(storedValue)) {
            if (keyProvider.isLegacyReadEnabled()) {
                return storedValue;
            }
            throw new SensitivePhoneCryptoException();
        }
        try {
            String[] parts = storedValue.split(":", -1);
            if (parts.length != 3 || !VERSION.equals(parts[0]) || parts[1].isBlank() || parts[2].isBlank()) {
                throw new SensitivePhoneCryptoException();
            }
            SecretKey key = keyProvider.getEncryptionKey(parts[1]);
            if (key == null) {
                throw new SensitivePhoneCryptoException();
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[2]);
            if (payload.length <= IV_BYTES + 16) {
                throw new SensitivePhoneCryptoException();
            }
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(field.aad().getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (SensitivePhoneCryptoException e) {
            throw e;
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new SensitivePhoneCryptoException(e);
        }
    }

    public boolean isEnvelope(String value) {
        return value != null && value.startsWith(VERSION + ":");
    }
}
