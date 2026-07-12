package com.neuedu.ems.util;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 基于 JDK PBKDF2-HMAC-SHA256 的密码哈希工具，无需额外安全依赖。
 */
@Component
public class PasswordHasher {

    private static final String PREFIX = "pbkdf2-sha256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = derive(rawPassword.toCharArray(), salt, ITERATIONS, HASH_BITS);
        return String.join("$",
                PREFIX,
                String.valueOf(ITERATIONS),
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash));
    }

    /**
     * 同时兼容历史明文；历史密码仅用于首次校验，成功后由认证服务立即升级。
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (!isEncoded(storedPassword)) {
            return MessageDigest.isEqual(
                    rawPassword.getBytes(StandardCharsets.UTF_8),
                    storedPassword.getBytes(StandardCharsets.UTF_8));
        }
        try {
            String[] parts = storedPassword.split("\\$", -1);
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(rawPassword.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isEncoded(String password) {
        return password != null && password.startsWith(PREFIX + "$") && password.split("\\$", -1).length == 4;
    }

    private byte[] derive(char[] password, byte[] salt, int iterations, int hashBits) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, hashBits);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("当前 JDK 不支持 PBKDF2WithHmacSHA256", e);
        } finally {
            spec.clearPassword();
        }
    }
}
