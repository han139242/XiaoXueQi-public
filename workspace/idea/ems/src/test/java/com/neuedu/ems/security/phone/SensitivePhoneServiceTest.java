package com.neuedu.ems.security.phone;

import com.neuedu.ems.common.AuthorizationContext;
import com.neuedu.ems.exception.SensitivePhoneAccessDeniedException;
import com.neuedu.ems.exception.SensitivePhoneCryptoException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitivePhoneServiceTest {

    private SensitivePhoneCryptoService cryptoService;
    private PhoneLookupHashService lookupHashService;
    private SensitivePhoneService phoneService;

    @BeforeEach
    void setUp() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("PHONE_CRYPTO_ACTIVE_KEY_ID", "k1")
                .withProperty("PHONE_CRYPTO_KEY_IDS", "k1,k2")
                .withProperty("PHONE_CRYPTO_KEY_K1", "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=")
                .withProperty("PHONE_CRYPTO_KEY_K2", "QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW1xdXl8=")
                .withProperty("PHONE_LOOKUP_HMAC_KEY", "ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8=");
        PhoneCryptoKeyProvider keyProvider = new PhoneCryptoKeyProvider(environment);
        PhoneNormalizer normalizer = new PhoneNormalizer();
        cryptoService = new SensitivePhoneCryptoService(keyProvider);
        lookupHashService = new PhoneLookupHashService(keyProvider, normalizer);
        SensitivePhoneAccessPolicy accessPolicy = new SensitivePhoneAccessPolicy();
        phoneService = new SensitivePhoneService(cryptoService, lookupHashService, normalizer, new PhoneMasker(), accessPolicy);
        setContext(Set.of("org:employee:phone:view-full", "org:employee:phone:write"));
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void encryptsWithRandomIvAndBindsCiphertextToItsField() {
        PhoneWriteValue first = phoneService.prepareCreate(SensitivePhoneField.EMPLOYEE, "138-0000 0001");
        PhoneWriteValue second = phoneService.prepareCreate(SensitivePhoneField.EMPLOYEE, "13800000001");

        assertTrue(first.encryptedValue().startsWith("v1:k1:"));
        assertNotEquals(first.encryptedValue(), second.encryptedValue());
        assertEquals("13800000001", cryptoService.decrypt(first.encryptedValue(), SensitivePhoneField.EMPLOYEE));
        assertThrows(SensitivePhoneCryptoException.class,
                () -> cryptoService.decrypt(first.encryptedValue(), SensitivePhoneField.CUSTOMER));
        assertEquals(first.lookupHash(), second.lookupHash());
    }

    @Test
    void masksWithoutFullViewPermissionAndOnlyHashesCompletePhoneNumbers() {
        PhoneWriteValue writeValue = phoneService.prepareCreate(SensitivePhoneField.EMPLOYEE, "13800000001");
        setContext(Set.of("org:employee:phone:write"));

        assertEquals("138****001", phoneService.display(SensitivePhoneField.EMPLOYEE, writeValue.encryptedValue()));
        assertTrue(phoneService.lookupHash("138 0000-0001").isPresent());
        assertFalse(phoneService.lookupHash("0001").isPresent());
    }

    @Test
    void rejectsUnauthorizedCreateAndIgnoresUnauthorizedUpdate() {
        setContext(Set.of());

        assertThrows(SensitivePhoneAccessDeniedException.class,
                () -> phoneService.prepareCreate(SensitivePhoneField.EMPLOYEE, "13800000001"));
        assertFalse(phoneService.prepareUpdate(SensitivePhoneField.EMPLOYEE, "13800000001").changed());
    }

    @Test
    void rejectsInvalidOrCiphertextShapedInput() {
        assertThrows(IllegalStateException.class,
                () -> new PhoneCryptoKeyProvider(new MockEnvironment(), name -> null));
        assertThrows(com.neuedu.ems.exception.BusinessException.class,
                () -> phoneService.prepareCreate(SensitivePhoneField.EMPLOYEE, "138****0001"));
        assertThrows(com.neuedu.ems.exception.BusinessException.class,
                () -> phoneService.prepareCreate(SensitivePhoneField.EMPLOYEE, "v1:k1:abc"));
    }

    @Test
    void readsKeysFromExternalResolverWhenEnvironmentDoesNotExposeUserVariables() {
        java.util.Map<String, String> userVariables = java.util.Map.of(
                "PHONE_CRYPTO_ACTIVE_KEY_ID", "k1",
                "PHONE_CRYPTO_KEY_IDS", "k1",
                "PHONE_CRYPTO_KEY_K1", "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
                "PHONE_LOOKUP_HMAC_KEY", "ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8=");

        PhoneCryptoKeyProvider keyProvider = new PhoneCryptoKeyProvider(
                new MockEnvironment(), userVariables::get);

        assertEquals("k1", keyProvider.getActiveKeyId());
        assertNotNull(keyProvider.getActiveEncryptionKey());
        assertEquals(32, keyProvider.getLookupHmacKey().length);
    }

    private void setContext(Set<String> permissions) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authorizationContext", new AuthorizationContext(
                1L, 1L, 1L, "ALL", Set.of(), permissions, Set.of()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
