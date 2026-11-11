package com.rpis.backend.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
}
