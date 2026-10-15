package com.rpis.backend.service;

import com.rpis.backend.dto.FieldRemarkRequest;
import com.rpis.backend.dto.ProposalSectionRemarksResponse;
import com.rpis.backend.dto.SectionRemarkDTO;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalSectionRemark;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalSectionRemarkRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies field-level revision remarks: a remark is bound to the correct proposal,
 * section and field, saving the same field updates it instead of duplicating it,
 * repeated entries are addressed individually, one revision cycle is one round,
 * history is retained when the round is closed, and only reviewers may write.
 * <p>
 * The repository is mocked with a small in-memory store so the round bookkeeping and
 * upsert identity are exercised exactly as they run against the database.
 */
class ProposalSectionRemarkServiceTest {

    private final List<ProposalSectionRemark> store = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong();
    private ProposalSectionRemarkService service;
    private Proposal proposal;

    @BeforeEach
    void setUp() {
        store.clear();
        ids.set(0);
        ProposalSectionRemarkRepository repository = mock(ProposalSectionRemarkRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        service = new ProposalSectionRemarkService(repository, userRepository, new CurrentUser());

        when(repository.save(any(ProposalSectionRemark.class))).thenAnswer(inv -> {
            ProposalSectionRemark row = inv.getArgument(0);
            if (row.getId() == null) {
                row.setId(ids.incrementAndGet());
                store.add(row);
            }
            return row;
        });
        when(repository.saveAll(anyList())).thenAnswer(inv -> {
            List<ProposalSectionRemark> rows = inv.getArgument(0);
            for (ProposalSectionRemark row : rows) {
                if (row.getId() == null) {
                    row.setId(ids.incrementAndGet());
                    if (store.stream().noneMatch(existing -> existing == row)) {
                        store.add(row);
                    }
                }
            }
            return rows;
        });
        when(repository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return store.stream().filter(row -> id.equals(row.getId())).findFirst();
        });
        // Mockito's delete() is a no-op on a mock, so mirror it into the in-memory store.
        org.mockito.Mockito.doAnswer(inv -> {
            ProposalSectionRemark row = inv.getArgument(0);
            store.removeIf(existing -> existing == row);
            return null;
        }).when(repository).delete(any(ProposalSectionRemark.class));
        when(repository.findByProposalIdAndStatus(anyLong(), anyString())).thenAnswer(inv -> {
            Long proposalId = inv.getArgument(0);
            String status = inv.getArgument(1);
            return store.stream()
                    .filter(r -> proposalId.equals(r.getProposal().getId()) && status.equals(r.getStatus()))
                    .toList();
        });
        when(repository.findMaxRound(anyLong())).thenAnswer(inv -> {
            Long proposalId = inv.getArgument(0);
            return store.stream()
                    .filter(r -> proposalId.equals(r.getProposal().getId()))
                    .map(ProposalSectionRemark::getRound)
                    .max(Integer::compareTo)
                    .orElse(null);
        });
        when(repository.findByProposalIdOrderByRoundDescIdAsc(anyLong())).thenAnswer(inv -> {
            Long proposalId = inv.getArgument(0);
            return store.stream()
                    .filter(r -> proposalId.equals(r.getProposal().getId()))
                    .sorted(Comparator.comparingInt(ProposalSectionRemark::getRound).reversed())
                    .toList();
        });

        User reviewer = new User();
        reviewer.setId(9L);
        reviewer.setName("Dr. Reviewer");
        reviewer.setRole("REC");
        when(userRepository.findById(9L)).thenReturn(Optional.of(reviewer));

        proposal = new Proposal();
        proposal.setId(1L);
        proposal.setProjectTitle("Water Filtration");
        authenticate(9L, "REC");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private FieldRemarkRequest field(String sectionKey, String fieldKey, String remark) {
        FieldRemarkRequest request = new FieldRemarkRequest();
        request.setSectionKey(sectionKey);
        request.setFieldKey(fieldKey);
        request.setRemark(remark);
        return request;
    }

    private SectionRemarkDTO sectionRemark(String sectionKey, String remark) {
        SectionRemarkDTO dto = new SectionRemarkDTO();
        dto.setSectionKey(sectionKey);
        dto.setRemark(remark);
        return dto;
    }

    /** A remark on a repeated entry: key, entry position and snapshot. */
    private FieldRemarkRequest entry(String sectionKey, String fieldKey, int entryIndex, String remark) {
        FieldRemarkRequest request = field(sectionKey, fieldKey, remark);
        request.setEntryIndex(entryIndex);
        request.setEntrySnapshot("{\"position\":\"Leader\"}");
        return request;
    }

    @Test
    void remarkIsStoredAgainstTheCorrectProposalSectionAndField() {
        ProposalSectionRemarksResponse.Item saved =
                service.upsertFieldRemark(proposal, field("01", "projectTitle", "Shorten the title."));

        assertEquals(1, store.size());
        assertEquals("01", saved.getSectionKey());
        assertEquals("Project Profile", saved.getSectionLabel());
        assertEquals("projectTitle", saved.getFieldKey());
        assertEquals("Shorten the title.", saved.getRemark());
        assertEquals(1, store.get(0).getRound());
        assertEquals(ProposalSectionRemark.STATUS_ACTIVE, store.get(0).getStatus());
        // Reviewer identity comes from the security context, not the request body.
        assertEquals("REC", saved.getReviewerRole());
        assertEquals(9L, saved.getReviewerId());
        assertEquals("Dr. Reviewer", saved.getReviewerName());
    }

    @Test
    void savingTheSameFieldAgainUpdatesItInsteadOfDuplicating() {
        service.upsertFieldRemark(proposal, field("01", "projectTitle", "First version."));
        service.upsertFieldRemark(proposal, field("01", "projectTitle", "Second version."));

        assertEquals(1, store.size());
        assertEquals("Second version.", store.get(0).getRemark());
    }

    @Test
    void remarksOnDifferentFieldsOfTheSameSectionAreKeptSeparately() {
        service.upsertFieldRemark(proposal, field("01", "projectTitle", "Title remark."));
        service.upsertFieldRemark(proposal, field("01", "duration", "Duration remark."));

        assertEquals(2, store.size());
        ProposalSectionRemarksResponse response = service.getRemarksForProposal(1L);
        assertEquals(1, response.getActiveRound());
        assertEquals(2, response.getRounds().get(0).getRemarks().size());
    }

    @Test
    void repeatedEntriesAreAddressedIndividuallyByTheirPosition() {
        service.upsertFieldRemark(proposal, entry("24", "personnel[0].time", 0, "Increase to 50%."));
        service.upsertFieldRemark(proposal, entry("24", "personnel[1].time", 1, "Reduce to 10%."));

        assertEquals(2, store.size());
        assertEquals(0, store.get(0).getEntryIndex());
        assertEquals(1, store.get(1).getEntryIndex());
    }

    @Test
    void sectionLevelAndFieldLevelRemarksCoexistInTheSameRound() {
        service.upsertFieldRemark(proposal, field("12", "methodology", "Clarify the sampling."));
        service.recordReturnRemarks(proposal, List.of(sectionRemark("12", "General: revise the method.")),
                "REC", 9L, "Dr. Reviewer");

        assertEquals(2, store.size());
        assertEquals("methodology", store.get(0).getFieldKey());
        assertNull(store.get(1).getFieldKey());
        assertEquals(1, store.get(0).getRound());
        assertEquals(1, store.get(1).getRound());
    }

    @Test
    void closingTheRoundRetainsHistoryAndTheNextRemarkStartsANewRound() {
        service.upsertFieldRemark(proposal, field("01", "projectTitle", "Round one."));
        service.closeActiveRound(proposal);
        service.upsertFieldRemark(proposal, field("01", "projectTitle", "Round two."));

        ProposalSectionRemarksResponse response = service.getRemarksForProposal(1L);
        assertEquals(2, response.getActiveRound());
        assertEquals(2, response.getRounds().size());
        assertTrue(response.getRounds().get(0).isActive());
        assertEquals("Round two.", response.getRounds().get(0).getRemarks().get(0).getRemark());
        assertEquals("Round one.", response.getRounds().get(1).getRemarks().get(0).getRemark());
        assertEquals(2, store.size(), "the previous round is retained, never overwritten");
    }

    @Test
    void aProponentMayNotWriteOrDeleteRemarks() {
        authenticate(5L, "PROPONENT");

        assertThrows(ResponseStatusException.class,
                () -> service.upsertFieldRemark(proposal, field("01", "projectTitle", "Nope")));
        assertThrows(ResponseStatusException.class, () -> service.deleteRemark(1L, 1L));
        assertTrue(store.isEmpty());
    }

    @Test
    void aReviewerCanDeleteTheirOwnRemark() {
        ProposalSectionRemarksResponse.Item saved =
                service.upsertFieldRemark(proposal, field("01", "projectTitle", "Remove me."));

        service.deleteRemark(1L, saved.getId());

        assertTrue(store.isEmpty());
    }

    @Test
    void aRemarkCannotBeDeletedThroughAnotherProposal() {
        ProposalSectionRemarksResponse.Item saved =
                service.upsertFieldRemark(proposal, field("01", "projectTitle", "Keep me."));

        assertThrows(ResponseStatusException.class, () -> service.deleteRemark(999L, saved.getId()));
        assertEquals(1, store.size());
    }

    @Test
    void invalidInputIsRejected() {
        // Blank remark.
        assertThrows(IllegalArgumentException.class,
                () -> service.upsertFieldRemark(proposal, field("01", "projectTitle", "   ")));
        // Unknown section.
        assertThrows(IllegalArgumentException.class,
                () -> service.upsertFieldRemark(proposal, field("99", "projectTitle", "Text")));
        // Malformed field identifier (must be a stable data path, not free text).
        assertThrows(IllegalArgumentException.class,
                () -> service.upsertFieldRemark(proposal, field("01", "Project Title!", "Text")));
        assertTrue(store.isEmpty());
    }

    @Test
    void blankRemarksAreIgnoredWhenReturningOrForwarding() {
        service.recordReturnRemarks(proposal, List.of(sectionRemark("12", "  ")), "REC", 9L, "R");
        service.recordForwardRemarks(proposal, null, "RPS", 2L, "R");

        assertTrue(store.isEmpty());
        assertFalse(service.hasUsableRemarks(List.of()));
        assertFalse(service.hasUsableRemarks(List.of(sectionRemark("12", "  "))));
    }

    @Test
    void theOpenRoundIsReusedByEveryReviewerInTheSameCycle() {
        service.upsertFieldRemark(proposal, field("01", "projectTitle", "REC remark."));
        service.recordForwardRemarks(proposal, List.of(sectionRemark("01", "RPS note.")), "RPS", 2L, "RPS Staff");

        assertEquals(2, store.size());
        assertEquals(1, store.get(0).getRound());
        assertEquals(1, store.get(1).getRound());
        ProposalSectionRemarksResponse response = service.getRemarksForProposal(1L);
        assertEquals(1, response.getRounds().size());
    }
}
