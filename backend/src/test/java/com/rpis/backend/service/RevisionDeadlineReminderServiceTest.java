package com.rpis.backend.service;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The reminder sweep rules: only revision-status proposals inside the window, one
 * message per deadline date, and one bad row cannot stop the others.
 */
class RevisionDeadlineReminderServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 9, 0);

    private ProposalRepository proposalRepository;
    private NotificationService notificationService;
    private RevisionDeadlineReminderService service;
    private User proponent;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        notificationService = mock(NotificationService.class);
        service = new RevisionDeadlineReminderService(proposalRepository,
                notificationService, mock(DataSource.class), 72);
        proponent = new User();
        proponent.setId(7L);
        proponent.setRole("PROPONENT");
        when(notificationService.notifyUsers(anyList(), anyString(), anyString(), anyString(),
                any(), any(NotificationEvent.class), anyString()))
                .thenReturn(List.of(new Notification()));
    }

    private Proposal proposal(Long id, String title, LocalDateTime deadline) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setProjectTitle(title);
        proposal.setRevisionDeadline(deadline);
        proposal.setProponent(proponent);
        return proposal;
    }

    @Test
    void aDueDeadlineProducesOneReminderWithAStableDedupeKey() {
        Proposal due = proposal(5L, "Coastal study", LocalDateTime.of(2026, 11, 21, 23, 59));
        when(proposalRepository.findRevisionDeadlinesDue(
                NOW, NOW.plusHours(72), RevisionDeadlineReminderService.REVISION_STATUSES))
                .thenReturn(List.of(due));

        assertEquals(1, service.runSweep(NOW));

        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).notifyUsers(
                eq(List.of(proponent)),
                org.mockito.ArgumentMatchers.contains("2026-11-21"),
                eq("Revision Deadline Reminder"),
                eq(NotificationTypes.REVISION_DEADLINE_REMINDER),
                eq(5L), event.capture(), eq("PROPONENT"));
        assertEquals("revision-deadline:5:2026-11-21", event.getValue().dedupeKey());
    }

    @Test
    void aProposalWithoutAProponentIsSkipped() {
        Proposal orphan = proposal(6L, "No owner", LocalDateTime.of(2026, 11, 21, 23, 59));
        orphan.setProponent(null);
        when(proposalRepository.findRevisionDeadlinesDue(any(), any(), anyList()))
                .thenReturn(List.of(orphan));

        assertEquals(0, service.runSweep(NOW));
        verify(notificationService, times(0)).notifyUsers(
                anyList(), anyString(), anyString(), anyString(), any(),
                any(NotificationEvent.class), anyString());
    }

    @Test
    void oneFailingRowDoesNotStopTheNextOne() {
        Proposal first = proposal(7L, "First", LocalDateTime.of(2026, 11, 20, 12, 0));
        Proposal second = proposal(8L, "Second", LocalDateTime.of(2026, 11, 22, 12, 0));
        when(proposalRepository.findRevisionDeadlinesDue(any(), any(), anyList()))
                .thenReturn(List.of(first, second));
        when(notificationService.notifyUsers(anyList(), anyString(), anyString(), anyString(),
                eq(7L), any(NotificationEvent.class), anyString()))
                .thenThrow(new RuntimeException("database hiccup"));

        assertEquals(1, service.runSweep(NOW), "the second row must still be processed");
        verify(notificationService).notifyUsers(anyList(), anyString(), anyString(), anyString(),
                eq(8L), any(NotificationEvent.class), eq("PROPONENT"));
    }

    @Test
    void aNullClockDoesNothing() {
        assertEquals(0, service.remindDue(null));
        verify(notificationService, times(0)).notifyUsers(
                anyList(), anyString(), anyString(), anyString(), any(),
                any(NotificationEvent.class), anyString());
    }

    @Test
    void theWindowAndStatusFilterArePassedToTheRepository() {
        when(proposalRepository.findRevisionDeadlinesDue(any(), any(), anyList()))
                .thenReturn(List.of());

        service.runSweep(NOW);

        verify(proposalRepository).findRevisionDeadlinesDue(
                NOW, NOW.plusHours(72),
                List.of("RPS_RETURNED", "REC_REVISION", "REVISION"));
        assertTrue(RevisionDeadlineReminderService.REVISION_STATUSES.contains("RPS_RETURNED"));
    }
}
