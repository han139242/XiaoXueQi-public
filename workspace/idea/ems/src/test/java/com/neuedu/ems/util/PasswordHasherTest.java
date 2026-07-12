package com.neuedu.ems.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void hashesAndVerifiesPasswordWithRandomSalt() {
        String first = passwordHasher.hash("123456");
        String second = passwordHasher.hash("123456");

        assertTrue(passwordHasher.isEncoded(first));
        assertTrue(passwordHasher.matches("123456", first));
        assertFalse(passwordHasher.matches("bad", first));
        assertNotEquals(first, second);
    }

    @Test
    void supportsLegacyPlaintextOnlyForMigration() {
        assertTrue(passwordHasher.matches("legacy", "legacy"));
        assertFalse(passwordHasher.matches("wrong", "legacy"));
        assertFalse(passwordHasher.isEncoded("legacy"));
    }
}
