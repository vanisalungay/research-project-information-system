package com.rpis.backend.service;

import com.rpis.backend.model.EndorsementLetterSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The funding-program registry behind the letter templates: only the Internally
 * Funded Project (IFP) letter exists, so a call for any other program must be
 * refused rather than silently rendered with IFP wording.
 */
class EndorsementLetterTemplatesTest {

    @Test
    void ifpIsTheImplementedProgram() {
        assertTrue(EndorsementLetterTemplates.isImplemented("IFP"));
        assertTrue(EndorsementLetterTemplates.isImplemented("ifp"));
        assertEquals(EndorsementLetterTemplates.PROGRAM_IFP,
                EndorsementLetterTemplates.normalize("  ifp "));
    }

    @Test
    void aMissingProgramFallsBackToIfp() {
        assertEquals(EndorsementLetterTemplates.PROGRAM_IFP,
                EndorsementLetterTemplates.normalize(null));
        assertEquals(EndorsementLetterTemplates.PROGRAM_IFP,
                EndorsementLetterTemplates.normalize("   "));
        assertEquals(EndorsementLetterTemplates.PROGRAM_IFP_LABEL,
                EndorsementLetterTemplates.labelFor(null));
    }

    @Test
    void anotherProgramHasNoLetterYetAndIsRefused() {
        assertFalse(EndorsementLetterTemplates.isImplemented("EXTERNALLY_FUNDED"));
        assertEquals("EXTERNALLY_FUNDED",
                EndorsementLetterTemplates.labelFor("externally_funded"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> EndorsementLetterTemplates.requireImplemented("EXTERNALLY_FUNDED"));
        assertTrue(error.getMessage().contains("EXTERNALLY_FUNDED"), error.getMessage());
        assertTrue(error.getMessage().contains("No official endorsement letter"),
                error.getMessage());
    }

    @Test
    void theImplementedProgramIsAcceptedByTheGuard() {
        EndorsementLetterTemplates.requireImplemented("IFP");
        EndorsementLetterTemplates.requireImplemented(null);
    }

    @Test
    void theLetterSettingsReportWhenTheDirectorIsMissing() {
        EndorsementLetterSettings settings = new EndorsementLetterSettings();
        assertFalse(settings.isComplete());

        settings.setDirectorName("Dr. Eduardo");
        assertFalse(settings.isComplete());

        settings.setDirectorSurname("Salazar");
        assertTrue(settings.isComplete());
    }
}
