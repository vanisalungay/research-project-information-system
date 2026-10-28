package com.rpis.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the password encoder and the legacy-value handling.
 *
 * <p>These run without a Spring context, so they cover the part of the change that is
 * pure logic: what counts as a hash, that a hash never equals its own input, and that a
 * value which is not a hash is reported as legacy so the caller can upgrade it.
 *
 * <p>The database-backed behaviour (registration, login, the migration pass) is covered
 * by {@link PasswordSecurityPostgresTest}.
 */
class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService(new BCryptPasswordEncoder());

    @Test
    void aHashedPasswordIsRecognisedAsHashed() {
        String hash = passwordService.hash("correct horse battery staple");

        assertTrue(passwordService.isHashed(hash));
        // The stored value must not be the password itself.
        assertNotEquals("correct horse battery staple", hash);
        assertTrue(hash.startsWith("$2"), "a BCrypt hash starts with $2");
    }

    @Test
    void aPlainTextValueIsNotMistakenForAHash() {
        assertFalse(passwordService.isHashed("proponentpassword1234"));
        assertFalse(passwordService.isHashed("LoadTest#2026"));
        // A value that merely starts like a hash is still not one.
        assertFalse(passwordService.isHashed("$2a$10$tooshort"));
        assertFalse(passwordService.isHashed("$1a$10$" + "x".repeat(53)));
        assertFalse(passwordService.isHashed(null));
    }

    @Test
    void hashingTheSamePasswordTwiceProducesDifferentValues() {
        // A fresh salt each time, which is the whole point of using the encoder rather
        // than a bare digest.
        String first = passwordService.hash("same-password");
        String second = passwordService.hash("same-password");

        assertNotEquals(first, second);
        assertTrue(passwordService.verify(first, "same-password").matches());
        assertTrue(passwordService.verify(second, "same-password").matches());
    }

    @Test
    void aHashVerifiesOnlyItsOwnPassword() {
        String hash = passwordService.hash("the-right-one");

        PasswordService.Verification correct = passwordService.verify(hash, "the-right-one");
        assertTrue(correct.matches());
        assertFalse(correct.legacy(), "a hash is not a legacy value");

        assertFalse(passwordService.verify(hash, "the-wrong-one").matches());
        assertFalse(passwordService.verify(hash, "").matches());
        assertFalse(passwordService.verify(hash, "the-right-one ").matches());
    }

    @Test
    void aLegacyPlainTextValueVerifiesAndIsReportedAsLegacy() {
        // Exactly the shape the column had before hashing existed.
        PasswordService.Verification correct = passwordService.verify("proponentpassword1234", "proponentpassword1234");

        assertTrue(correct.matches());
        assertTrue(correct.legacy(), "the caller must be told this row still needs hashing");

        PasswordService.Verification wrong = passwordService.verify("proponentpassword1234", "nope");
        assertFalse(wrong.matches());
        assertTrue(wrong.legacy());
    }

    @Test
    void nullAndBlankInputsAreRefusedRatherThanStored() {
        assertThrows(IllegalArgumentException.class, () -> passwordService.hash(null));
        assertThrows(IllegalArgumentException.class, () -> passwordService.hash(""));
        assertThrows(IllegalArgumentException.class, () -> passwordService.hash("   "));

        // hashIfPresent is the "leave it alone" variant used by partial updates.
        org.junit.jupiter.api.Assertions.assertNull(passwordService.hashIfPresent(null));
        org.junit.jupiter.api.Assertions.assertNull(passwordService.hashIfPresent("  "));
        assertTrue(passwordService.isHashed(passwordService.hashIfPresent("value")));
    }

    @Test
    void aMissingStoredOrPresentedValueNeverMatches() {
        assertFalse(passwordService.verify(null, "anything").matches());
        assertFalse(passwordService.verify("anything", null).matches());
        assertFalse(passwordService.verify(null, null).matches());
    }
}
