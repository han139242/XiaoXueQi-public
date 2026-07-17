package com.neuedu.ems.security.phone;

/**
 * 电话更新意图：不变、更换为密文，或清空持久化列。
 */
public record PhoneWriteValue(String encryptedValue, String lookupHash, boolean changed, boolean clear) {

    public static PhoneWriteValue unchanged() {
        return new PhoneWriteValue(null, null, false, false);
    }

    public static PhoneWriteValue clearValue() {
        return new PhoneWriteValue(null, null, true, true);
    }

    public static PhoneWriteValue replace(String encryptedValue, String lookupHash) {
        return new PhoneWriteValue(encryptedValue, lookupHash, true, false);
    }
}
