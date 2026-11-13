package com.rpis.backend.service;

import com.rpis.backend.model.CooperatingAgency;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.repository.CooperatingAgencyRepository;
import com.rpis.backend.repository.ProposalCooperatingAgencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Rules of the shared cooperating agency directory: a new agency is normalized and
 * validated, an existing name is reported as a conflict instead of being stored
 * twice (also when the race is only lost at the database), and the selection of a
 * proposal is replaced from validated directory ids - never from names a client sent.
 */
class CooperatingAgencyServiceTest {

    private CooperatingAgencyRepository agencyRepository;
    private ProposalCooperatingAgencyRepository linkRepository;
    private CooperatingAgencyService service;

    @BeforeEach
    void setUp() {
        agencyRepository = mock(CooperatingAgencyRepository.class);
        linkRepository = mock(ProposalCooperatingAgencyRepository.class);
        service = new CooperatingAgencyService(agencyRepository, linkRepository);
    }

    private static Proposal savedProposal(Long id) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        return proposal;
    }

    @Test
    void aNewAgencyIsNormalizedBeforeItIsStored() {
        when(agencyRepository.findFirstByNameIgnoreCase("Bureau of Fisheries (BFAR)")).thenReturn(Optional.empty());
        when(agencyRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CooperatingAgency created = service.addAgency("  Bureau   of Fisheries (BFAR) ");

        assertEquals("Bureau of Fisheries (BFAR)", created.getName());
        verify(agencyRepository).saveAndFlush(any());
    }

    @Test
    void aMissingOrTooLongNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addAgency("   "));
        assertThrows(IllegalArgumentException.class, () -> service.addAgency(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.addAgency("x".repeat(CooperatingAgency.NAME_MAX_LENGTH + 1)));
        verify(agencyRepository, never()).saveAndFlush(any());
    }

    @Test
    void anExistingAgencyIsReportedAsAConflictAndNotStoredAgain() {
        // The directory lookup is case-insensitive (findFirstByNameIgnoreCase); the
        // service asks with the normalized name and reports a conflict instead of
        // storing a second row.
        when(agencyRepository.findFirstByNameIgnoreCase("dost")).thenReturn(
                Optional.of(new CooperatingAgency(1L, "Department of Science and Technology (DOST)", null)));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.addAgency("  dost "));

        assertTrue(ex.getMessage().contains("already in the cooperating agencies list"));
        verify(agencyRepository, never()).saveAndFlush(any());
    }

    @Test
    void anAgencyThatLostTheRaceToAnotherProponentIsReportedTheSameWay() {
        // The pre-check passed, but the case-insensitive unique index rejected the
        // insert because another request stored the same agency first.
        when(agencyRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(agencyRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.addAgency("DOST"));

        assertTrue(ex.getMessage().contains("already in the cooperating agencies list"));
    }

    @Test
    void theSelectionIsReplacedFromValidatedIdsInDirectoryOrder() {
        CooperatingAgency dost = new CooperatingAgency(1L, "Department of Science and Technology (DOST)", null);
        CooperatingAgency denr =
                new CooperatingAgency(2L, "Department of Environment and Natural Resources (DENR)", null);
        when(agencyRepository.findAllById(any())).thenReturn(List.of(dost, denr));

        // Duplicates and null entries in the submitted list are ignored.
        List<CooperatingAgency> selected =
                service.replaceSelection(savedProposal(5L), Arrays.asList(2L, 1L, 2L, null));

        assertEquals(
                List.of("Department of Environment and Natural Resources (DENR)",
                        "Department of Science and Technology (DOST)"),
                selected.stream().map(CooperatingAgency::getName).toList());
        assertEquals(
                "Department of Environment and Natural Resources (DENR), "
                        + "Department of Science and Technology (DOST)",
                CooperatingAgencyService.describeSelection(selected));
        verify(linkRepository).deleteByProposalId(5L);
        verify(linkRepository, times(2)).save(any());
    }

    @Test
    void anUnknownAgencyIdIsRejectedInsteadOfBeingTrusted() {
        when(agencyRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.replaceSelection(savedProposal(5L), List.of(99L)));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void clearingTheSelectionOnlyUnlinksTheAgencies() {
        assertEquals(List.of(), service.replaceSelection(savedProposal(5L), List.of()));

        verify(linkRepository).deleteByProposalId(5L);
        verify(linkRepository, never()).save(any());
        // The shared directory is never touched by a proposal change.
        verifyNoInteractions(agencyRepository);
    }

    @Test
    void aProposalMustBeSavedBeforeItsSelectionCanBeStored() {
        assertThrows(IllegalArgumentException.class, () -> service.replaceSelection(new Proposal(), List.of(1L)));
        assertThrows(IllegalArgumentException.class, () -> service.replaceSelection(null, List.of(1L)));
        verifyNoInteractions(linkRepository);
    }

    @Test
    void anEmptySelectionHasNoText() {
        assertEquals("", CooperatingAgencyService.describeSelection(List.of()));
        assertEquals("", CooperatingAgencyService.describeSelection(null));
    }

    @Test
    void agencyNamesAreTrimmedAndSingleSpaced() {
        assertEquals("DOST", CooperatingAgencyService.normalizeName("  DOST  "));
        assertEquals("Bureau of Fisheries", CooperatingAgencyService.normalizeName("Bureau   of\tFisheries"));
        assertEquals("", CooperatingAgencyService.normalizeName(null));
    }
}
