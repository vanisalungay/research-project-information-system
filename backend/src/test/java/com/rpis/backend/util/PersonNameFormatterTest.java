package com.rpis.backend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The server-side name format. The same cases are pinned in
 * {@code src/utils/personName.test.js}, so the browser and the backend cannot
 * drift apart.
 */
class PersonNameFormatterTest {

    @Test
    void uppercaseNamesBecomeTitleCase() {
        assertEquals("Juan Dela Cruz", PersonNameFormatter.format("JUAN DELA CRUZ"));
        assertEquals("Xarzha Maurine Bagares",
                PersonNameFormatter.format("XARZHA MAURINE BAGARES"));
    }

    @Test
    void lowercaseAndMixedCaseNamesBecomeTitleCase() {
        assertEquals("Juan Dela Cruz", PersonNameFormatter.format("juan dela cruz"));
        assertEquals("Juan Dela Cruz", PersonNameFormatter.format("jUaN dElA cRuZ"));
    }

    @Test
    void theStandardOrderKeepsCommasPeriodsAndSuffix() {
        assertEquals("Juan, Dela Cruz, M.", PersonNameFormatter.format("JUAN, DELA CRUZ, M."));
        assertEquals("Dela Cruz, Juan, M., Jr.",
                PersonNameFormatter.format("DELA CRUZ, JUAN, M., JR."));
        assertEquals("Dela Cruz, Juan, M., Jr.",
                PersonNameFormatter.format("dela cruz,juan,m.,jr."));
    }

    @Test
    void middleInitialAndSuffixAreOptional() {
        assertEquals("Dela Cruz, Juan", PersonNameFormatter.format("DELA CRUZ, JUAN"));
        assertEquals("Dela Cruz, Juan, Jr.", PersonNameFormatter.format("DELA CRUZ, JUAN, JR."));
    }

    @Test
    void multiWordSurnamesStayWhole() {
        assertEquals("De La Cruz, Juan", PersonNameFormatter.format("DE LA CRUZ, JUAN"));
        assertEquals("Dela Cruz, Maria Clara",
                PersonNameFormatter.format("DELA CRUZ, MARIA CLARA"));
        assertEquals("Van Der Berg, Ana", PersonNameFormatter.format("VAN DER BERG, ANA"));
    }

    @Test
    void romanNumeralSuffixesStayUppercase() {
        assertEquals("Dela Cruz, Juan, III", PersonNameFormatter.format("DELA CRUZ, JUAN, III"));
        assertEquals("Santos, Jose, IV", PersonNameFormatter.format("santos, jose, iv"));
    }

    @Test
    void hyphensAndApostrophesArePreserved() {
        assertEquals("Ana-Maria Reyes", PersonNameFormatter.format("ANA-MARIA REYES"));
        assertEquals("Jose O'Brien", PersonNameFormatter.format("JOSE O'BRIEN"));
        assertEquals("Dela Cruz, Jean-Luc", PersonNameFormatter.format("dela cruz, jean-luc"));
    }

    @Test
    void extraSpacesAreCollapsed() {
        assertEquals("Dela Cruz, Juan", PersonNameFormatter.format("  dela   cruz ,   juan  "));
    }

    @Test
    void blankAndNullValuesAreUnchanged() {
        assertEquals("", PersonNameFormatter.format(""));
        assertEquals("", PersonNameFormatter.format("   "));
        assertNull(PersonNameFormatter.format(null));
    }

    @Test
    void aMultiLineTeamIsFormattedLineByLine() {
        assertEquals(
                "Bagares, Xarzha Maurine\nLungay, Vanisa\n\nDela Cruz, Juan, M., Jr.",
                PersonNameFormatter.formatLines(
                        "BAGARES, XARZHA MAURINE\nlungay, vanisa\n\nDELA CRUZ, JUAN, M., JR."));
        assertEquals("", PersonNameFormatter.formatLines(""));
        assertNull(PersonNameFormatter.formatLines(null));
    }

    @Test
    void formattingIsIdempotent() {
        String once = PersonNameFormatter.format("DELA CRUZ, JUAN, M., JR.");
        assertEquals(once, PersonNameFormatter.format(once));
    }
}
