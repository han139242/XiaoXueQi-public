package com.neuedu.ems.service.impl.sys;

import com.neuedu.ems.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 注销令牌黑名单。Redis 不可用时使用当前进程内存作为降级方案。
 */
@Service
@Slf4j
public class TokenRevocationService {

    private static final String KEY_PREFIX = "revoked_token_";

    private final RedisUtil redisUtil;
    private final Map<String, Long> localRevokedTokens = new ConcurrentHashMap<>();

    public TokenRevocationService(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    public void revoke(String token, Date expiration) {
        if (token == null || expiration == null) {
            return;
        }
        String fingerprint = fingerprint(token);
        long expirationMillis = expiration.getTime();
        localRevokedTokens.put(fingerprint, expirationMillis);
        long ttlSeconds = Math.max(1, expiration.toInstant().getEpochSecond() - Instant.now().getEpochSecond());
        try {
            redisUtil.set(KEY_PREFIX + fingerprint, Boolean.TRUE, ttlSeconds, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            log.warn("Redis unavailable while revoking token; using local blacklist: {}", e.getMessage());
        }
    }

    public boolean isRevoked(String token) {
        if (token == null) {
            return false;
        }
        String fingerprint = fingerprint(token);
        Long expirationMillis = localRevokedTokens.get(fingerprint);
        if (expirationMillis != null) {
            if (expirationMillis > System.currentTimeMillis()) {
                return true;
            }
            localRevokedTokens.remove(fingerprint);
        }
        try {
            return redisUtil.exists(KEY_PREFIX + fingerprint);
        } catch (RuntimeException e) {
            log.debug("Redis unavailable while checking revoked token: {}", e.getMessage());
            return false;
        }
    }

    private String fingerprint(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }
}
