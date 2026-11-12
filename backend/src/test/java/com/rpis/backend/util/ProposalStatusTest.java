package com.rpis.backend.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Proponent-to-ORPS submission status: a submission is stored as FOR REVIEW, and the
 * legacy SUBMITTED spelling is the same workflow state.
 */
class ProposalStatusTest {

    @Test
    void bothSpellingsMeanTheSameSubmittedState() {
        assertTrue(ProposalStatus.isForReview("FOR REVIEW"));
        assertTrue(ProposalStatus.isForReview("for review"));
        assertTrue(ProposalStatus.isForReview("  For Review  "));
        assertTrue(ProposalStatus.isForReview("SUBMITTED"));
        assertTrue(ProposalStatus.isForReview("submitted"));
        assertTrue(ProposalStatus.isSubmissionRequest("SUBMITTED"));
        assertTrue(ProposalStatus.isSubmissionRequest("FOR REVIEW"));
    }

    @Test
    void otherStatusesAreNotSubmissions() {
        assertFalse(ProposalStatus.isForReview("DRAFT"));
        assertFalse(ProposalStatus.isForReview("RPS_RETURNED"));
        assertFalse(ProposalStatus.isForReview("SUBMITTED_FOR_REVIEW"));
        assertFalse(ProposalStatus.isForReview(""));
        assertFalse(ProposalStatus.isForReview(null));
    }

    @Test
    void aSubmissionRequestIsAlwaysStoredAsForReview() {
        assertEquals("FOR REVIEW", ProposalStatus.toStoredStatus("SUBMITTED"));
        assertEquals("FOR REVIEW", ProposalStatus.toStoredStatus("submitted"));
        assertEquals("FOR REVIEW", ProposalStatus.toStoredStatus("FOR REVIEW"));
        assertEquals("FOR REVIEW", ProposalStatus.toStoredStatus("  for review "));
    }

    @Test
    void everyOtherStatusKeepsThePreviousUppercaseBehaviour() {
        assertEquals("DRAFT", ProposalStatus.toStoredStatus("draft"));
        assertEquals("RPS_RETURNED", ProposalStatus.toStoredStatus(" rps_returned "));
        assertNull(ProposalStatus.toStoredStatus(null));
        assertNull(ProposalStatus.toStoredStatus("   "));
    }

    @Test
    void aFilterForEitherSpellingReturnsBothValues() {
        assertEquals(List.of("SUBMITTED", "FOR REVIEW"),
                ProposalStatus.withForReviewAliases(List.of("submitted")));
        assertEquals(List.of("FOR REVIEW", "SUBMITTED"),
                ProposalStatus.withForReviewAliases(List.of("for review")));
        assertEquals(List.of("SUBMITTED", "FOR REVIEW", "APPROVED"),
                ProposalStatus.withForReviewAliases(List.of("SUBMITTED", "APPROVED")));
    }

    @Test
    void aliasExpansionDeduplicatesAndDropsBlankEntries() {
        assertEquals(List.of("FOR REVIEW", "SUBMITTED", "APPROVED"),
                ProposalStatus.withForReviewAliases(
                        List.of("FOR REVIEW", "SUBMITTED", "", "  ", "approved")));
        assertEquals(List.of(), ProposalStatus.withForReviewAliases(null));
        assertEquals(List.of(), ProposalStatus.withForReviewAliases(List.of()));
    }

    @Test
    void everyWorkflowStatusIsAcceptedAndKeptCanonical() {
        for (String status : ProposalStatus.SUPPORTED_STORED_STATUSES) {
            assertEquals(status, ProposalStatus.requireSupportedStatus(status),
                    "supported status must survive validation unchanged: " + status);
            assertEquals(status, ProposalStatus.requireSupportedStatus("  " + status.toLowerCase() + " "),
                    "case and surrounding whitespace are normalized: " + status);
        }
        assertEquals(28, ProposalStatus.SUPPORTED_STORED_STATUSES.size(),
                "the 27 non-legacy lookup codes from migration-03 plus the current FOR REVIEW spelling");
    }

    @Test
    void theLegacySubmittedAliasIsAcceptedButStoredAsForReview() {
        assertEquals("FOR REVIEW", ProposalStatus.requireSupportedStatus("SUBMITTED"));
        assertEquals("FOR REVIEW", ProposalStatus.requireSupportedStatus("submitted"));
        assertFalse(ProposalStatus.SUPPORTED_STORED_STATUSES.contains("SUBMITTED"),
                "SUBMITTED is an input alias, never a newly stored status");
    }

    @Test
    void anUnknownStatusIsRejectedWithTheInvalidValueInTheMessage() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ProposalStatus.requireSupportedStatus("NOT_A_PROPOSAL_STATUS"));
        assertTrue(error.getMessage().contains("NOT_A_PROPOSAL_STATUS"), error.getMessage());
        assertTrue(error.getMessage().contains("Invalid proposal status"), error.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> ProposalStatus.requireSupportedStatus("SUBMITTED_FOR_REVIEW"));
    }

    @Test
    void aMissingStatusIsRejectedByTheRequiredVariantButAllowedAsOptional() {
        assertThrows(IllegalArgumentException.class, () -> ProposalStatus.requireSupportedStatus(null));
        assertThrows(IllegalArgumentException.class, () -> ProposalStatus.requireSupportedStatus("   "));

        assertNull(ProposalStatus.optionalSupportedStatus(null));
        assertNull(ProposalStatus.optionalSupportedStatus("   "));
        assertEquals("DRAFT", ProposalStatus.optionalSupportedStatus(" draft "));
        assertEquals("FOR REVIEW", ProposalStatus.optionalSupportedStatus("SUBMITTED"));
        assertThrows(IllegalArgumentException.class,
                () -> ProposalStatus.optionalSupportedStatus("NOT_A_PROPOSAL_STATUS"));
    }
}
