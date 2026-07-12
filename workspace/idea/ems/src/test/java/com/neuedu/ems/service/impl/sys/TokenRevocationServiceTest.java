package com.neuedu.ems.service.impl.sys;

import com.neuedu.ems.util.RedisUtil;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TokenRevocationServiceTest {

    @Test
    void revokedTokenIsImmediatelyRejectedAndStoredWithTtl() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        TokenRevocationService service = new TokenRevocationService(redisUtil);
        Date expiration = new Date(System.currentTimeMillis() + 60_000);

        service.revoke("jwt-token", expiration);

        assertTrue(service.isRevoked("jwt-token"));
        verify(redisUtil).set(anyString(), eq(Boolean.TRUE), org.mockito.ArgumentMatchers.longThat(value -> value > 0), eq(TimeUnit.SECONDS));
    }
}
