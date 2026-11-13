package com.rpis.backend.service;

import java.util.List;
import java.util.Locale;

/**
 * Registry of the official endorsement-letter templates, keyed by the funding
 * program (call type) of the Call for Proposal the request answers.
 *
 * <p>The RPIS only has the official wording of the <strong>Internally Funded
 * Project Proposal (IFP)</strong> endorsement letter. A Call for Proposal whose
 * funding program has no template yet must not silently be endorsed with the IFP
 * letter, so {@link #requireImplemented(String)} refuses it with an explicit
 * message and the UI reports the same state. When the university supplies the
 * wording of another program it is added here (and the wording is rendered by
 * {@code EndorsementLetterService}); nothing else in the workflow changes.
 */
public final class EndorsementLetterTemplates {

    /** The funding program whose official endorsement letter is implemented. */
    public static final String PROGRAM_IFP = "IFP";

    /** Display name of the IFP program, as printed on the official letter. */
    public static final String PROGRAM_IFP_LABEL = "Internally Funded Project Proposal (IFP)";

    /** Label used when a Call for Proposal carries no funding program. */
    public static final String UNSPECIFIED_LABEL = "Unspecified funding program";

    /** The funding programs an administrator may select today. */
    private static final List<String> IMPLEMENTED = List.of(PROGRAM_IFP);

    private EndorsementLetterTemplates() {
    }

    /** The funding program of a call, defaulting to IFP when none is stored. */
    public static String normalize(String fundingProgram) {
        if (fundingProgram == null || fundingProgram.isBlank()) {
            return PROGRAM_IFP;
        }
        return fundingProgram.trim().toUpperCase(Locale.ROOT);
    }

    /** True when an official endorsement letter exists for the funding program. */
    public static boolean isImplemented(String fundingProgram) {
        return IMPLEMENTED.contains(normalize(fundingProgram));
    }

    /** Human-readable name of a funding program, for the UI and the letter. */
    public static String labelFor(String fundingProgram) {
        String key = normalize(fundingProgram);
        if (PROGRAM_IFP.equals(key)) {
            return PROGRAM_IFP_LABEL;
        }
        if (fundingProgram == null || fundingProgram.isBlank()) {
            return UNSPECIFIED_LABEL;
        }
        return key;
    }

    /** The programs that currently have an official endorsement letter. */
    public static List<String> implementedPrograms() {
        return IMPLEMENTED;
    }

    /**
     * Fails when no official endorsement letter is configured for the funding
     * program, so the IFP wording can never be issued for a different program.
     */
    public static void requireImplemented(String fundingProgram) {
        if (!isImplemented(fundingProgram)) {
            throw new IllegalStateException(
                    "No official endorsement letter is configured for the funding program \""
                            + labelFor(fundingProgram)
                            + "\" yet. Only " + PROGRAM_IFP_LABEL
                            + " may be endorsed until the university supplies the official wording "
                            + "of the other program.");
        }
    }
}
