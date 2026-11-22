package com.rpis.backend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared password policy. These are plain unit tests - no Spring context and no
 * database - so the rules are pinned independently of every controller that uses them.
 */
class PasswordPolicyTest {

    @Test
    void theExampleShownInTheInterfaceIsAccepted() {
        assertTrue(PasswordPolicy.isValid("RpisSystem@2026"));
        assertDoesNotThrow(() -> PasswordPolicy.requireValid("RpisSystem@2026"));
    }

    @Test
    void fewerThanEightCharactersIsRejected() {
        assertFalse(PasswordPolicy.isValid("Ab1!xyz"));
        assertTrue(PasswordPolicy.violations("Ab1!xyz").contains("at least 8 characters"));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.requireValid("Ab1!xyz"));
    }

    @Test
    void aPasswordWithoutAnUppercaseLetterIsRejected() {
        assertFalse(PasswordPolicy.isValid("rpis@2026"));
        assertTrue(PasswordPolicy.violations("rpis@2026")
                .contains("at least one uppercase letter (A-Z)"));
    }

    @Test
    void aPasswordWithoutALowercaseLetterIsRejected() {
        assertFalse(PasswordPolicy.isValid("RPIS@2026"));
        assertTrue(PasswordPolicy.violations("RPIS@2026")
                .contains("at least one lowercase letter (a-z)"));
    }

    @Test
    void aPasswordWithoutADigitIsRejected() {
        assertFalse(PasswordPolicy.isValid("RpisSystem@"));
        assertTrue(PasswordPolicy.violations("RpisSystem@")
                .contains("at least one number (0-9)"));
    }

    @Test
    void aPasswordWithoutASymbolIsRejected() {
        assertFalse(PasswordPolicy.isValid("RpisSystem2026"));
        assertTrue(PasswordPolicy.violations("RpisSystem2026")
                .contains("at least one special character or symbol"));
    }

    @Test
    void aPasswordMissingSeveralRequirementsReportsEachOne() {
        assertEquals(3, PasswordPolicy.violations("rpissystem").size(),
                "no uppercase, no number and no symbol");
    }

    @Test
    void nullAndEmptyPasswordsAreRejected() {
        assertFalse(PasswordPolicy.isValid(null));
        assertFalse(PasswordPolicy.isValid(""));
    }

    @Test
    void theMessageNamesRequirementsButNeverThePassword() {
        String password = "weaksecret";
        String message = assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.requireValid(password)).getMessage();

        assertTrue(message.contains("uppercase"), "the unmet requirement is named");
        assertFalse(message.contains(password), "the submitted password is never echoed");
    }
}
