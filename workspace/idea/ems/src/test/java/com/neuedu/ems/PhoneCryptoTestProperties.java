package com.neuedu.ems;

/** 仅供 Spring 集成测试启动上下文的固定非生产密钥属性。 */
public final class PhoneCryptoTestProperties {

    public static final String ACTIVE_KEY = "PHONE_CRYPTO_ACTIVE_KEY_ID=test-k1";
    public static final String KEY_IDS = "PHONE_CRYPTO_KEY_IDS=test-k1";
    public static final String ENCRYPTION_KEY = "PHONE_CRYPTO_KEY_TEST_K1=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";
    public static final String LOOKUP_HMAC_KEY = "PHONE_LOOKUP_HMAC_KEY=ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8=";

    private PhoneCryptoTestProperties() {
    }
}
