package com.rpis.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The temporary password an administrator-created account is issued.
 *
 * <p>The format is {@code [first initial][surname]123}, lower case and without spaces, so the
 * administrator who creates the account can read the value out to whoever will own it. These
 * tests pin the format down; the create-account tests verify that the stored value is a BCrypt
 * hash that verifies against exactly this password.
 */
class InstitutionalCredentialsServiceTest {

    @Test
    void theFirstInitialAndTheSurnameMakeThePassword() {
        assertEquals("xbagares123",
                InstitutionalCredentialsService.temporaryPasswordFor("Xarzha Maurine Bagares"));
        assertEquals("cfabia123",
                InstitutionalCredentialsService.temporaryPasswordFor("Christian Mark Fabia"));
        assertEquals("jsantos123",
                InstitutionalCredentialsService.temporaryPasswordFor("Juan Miguel Santos"));
    }

    @Test
    void aCompoundSurnameIsKeptWhole() {
        assertEquals("mdelacruz123",
                InstitutionalCredentialsService.temporaryPasswordFor("Maria Clara Dela Cruz"));
        assertEquals("jdelacruz123",
                InstitutionalCredentialsService.temporaryPasswordFor("Juan Dela Cruz"));
        assertEquals("ldelacruz123",
                InstitutionalCredentialsService.temporaryPasswordFor("Liza De La Cruz"));
        assertEquals("rdeguzman123",
                InstitutionalCredentialsService.temporaryPasswordFor("Rosa De Guzman"));
    }

    @Test
    void itIsLowerCasedAndCarriesNoSpaces() {
        assertEquals("xbagares123",
                InstitutionalCredentialsService.temporaryPasswordFor("  XARZHA  MAURINE   BAGARES  "));
        assertTrue(InstitutionalCredentialsService.temporaryPasswordFor("Xarzha Maurine Bagares")
                        .matches("[a-z]+123"),
                "the password must be lower case throughout and end with 123");
    }

    @Test
    void accentsAndPunctuationAreStripped() {
        assertEquals("mdelacruz123",
                InstitutionalCredentialsService.temporaryPasswordFor("María Clara Dela Cruz"));
        assertEquals("jobrien123",
                InstitutionalCredentialsService.temporaryPasswordFor("Jose O'Brien"));
    }

    @Test
    void aOneWordNameIsItsOwnInitialAndSurname() {
        assertEquals("bbagares123",
                InstitutionalCredentialsService.temporaryPasswordFor("Bagares"));
        assertEquals("bbagares123",
                InstitutionalCredentialsService.temporaryPasswordFor("  bagares  "));
    }

    @Test
    void aNameWithoutAnythingUsableIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> InstitutionalCredentialsService.temporaryPasswordFor("   "));
        assertThrows(IllegalArgumentException.class,
                () -> InstitutionalCredentialsService.temporaryPasswordFor(null));
    }
}
