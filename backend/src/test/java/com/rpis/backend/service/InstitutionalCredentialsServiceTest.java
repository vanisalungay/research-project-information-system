package com.rpis.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void theStandardLastFirstNameOrderIsUnderstood() {
        // The form asks for "Dela Cruz, Juan, M., Jr."; the given names still come first
        // in the address and the whole surname is used by the password.
        assertEquals("jdelacruz123",
                InstitutionalCredentialsService.temporaryPasswordFor("Dela Cruz, Juan, M., Jr."));
        assertEquals("xbagares123",
                InstitutionalCredentialsService.temporaryPasswordFor("Bagares, Xarzha Maurine"));
        assertEquals("mdelacruz123",
                InstitutionalCredentialsService.temporaryPasswordFor("De La Cruz, Maria Clara"));
        // Middle initial omitted: the third component is then the suffix.
        assertEquals("jdelacruz123",
                InstitutionalCredentialsService.temporaryPasswordFor("Dela Cruz, Juan, Jr."));
    }

    @Test
    void theStandardOrderDerivesTheAddressFromItsComponents() {
        assertEquals("juan.delacruz",
                InstitutionalCredentialsService.localPart("Dela Cruz, Juan"));
        assertEquals("juanm.delacruz",
                InstitutionalCredentialsService.localPart("Dela Cruz, Juan, M., Jr."));
        assertEquals("xarzhamaurine.bagares",
                InstitutionalCredentialsService.localPart("Bagares, Xarzha Maurine"));
        // A simple surname resolves the same way in either order.
        assertEquals(InstitutionalCredentialsService.localPart("Juan Santos"),
                InstitutionalCredentialsService.localPart("Santos, Juan"));
    }

    @Test
    void aCompoundSurnameDerivesTheSameAddressInEitherOrder() {
        // The Dean form asks for "First Name Last Name". The surname particles keep
        // "Dela Cruz" whole, so the same person gets the same address and password whether
        // the name is entered in the Dean form's order or the standard comma order.
        assertEquals("juan.delacruz",
                InstitutionalCredentialsService.localPart("Juan Dela Cruz"));
        assertEquals("juan.delacruz",
                InstitutionalCredentialsService.localPart("Dela Cruz, Juan"));
        assertEquals("jdelacruz123",
                InstitutionalCredentialsService.temporaryPasswordFor("Juan Dela Cruz"));
        assertEquals("maria.delacruz",
                InstitutionalCredentialsService.localPart("Maria Dela Cruz"));
        assertEquals("liza.delacruz",
                InstitutionalCredentialsService.localPart("Liza De La Cruz"));
        // Case never reaches the address.
        assertEquals("juan.delacruz",
                InstitutionalCredentialsService.localPart("JUAN DELA CRUZ"));
    }

    @Test
    void generatedAddressesUseTheCorrectUniversityDomain() {
        assertEquals("@msunaawan.edu.ph", InstitutionalCredentialsService.INSTITUTIONAL_DOMAIN);
        assertEquals("xarzhamaurine.bagares@msunaawan.edu.ph",
                InstitutionalCredentialsService.emailFor("Xarzha Maurine Bagares"));
        assertTrue(InstitutionalCredentialsService.isInstitutionalAddress(
                "vanisa.lungay@msunaawan.edu.ph"));
        assertFalse(InstitutionalCredentialsService.isInstitutionalAddress(
                "vanisa.lungay@msunawaan.edu.ph"),
                "the retired misspelling must no longer be accepted for an account");
    }

    @Test
    void aSimpleFirstAndLastNameIsStillReadAsGivenThenSurname() {
        assertEquals("juan.santos",
                InstitutionalCredentialsService.localPart("Juan Santos"));
        assertEquals("jsantos123",
                InstitutionalCredentialsService.temporaryPasswordFor("Juan Santos"));
        assertEquals("xarzhamaurine.bagares",
                InstitutionalCredentialsService.localPart("Xarzha Maurine Bagares"));
    }
}
