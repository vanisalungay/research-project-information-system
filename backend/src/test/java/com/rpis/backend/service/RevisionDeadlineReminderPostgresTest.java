package com.rpis.backend.service;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.NotificationPreferenceKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The real advisory lock and unique dedupe key: two sweeps over the same deadline
 * produce exactly one row, and a preference that is off produces none.
 */
@SpringBootTest
@ActiveProfiles("test")
class RevisionDeadlineReminderPostgresTest {

    @Autowired private RevisionDeadlineReminderService reminderService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private User proponent;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE notifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = new User();
        proponent.setName("Deadline Proponent");
        proponent.setEmail("deadline-" + System.nanoTime() + "@example.edu.ph");
        proponent.setPassword("secret");
        proponent.setRole("PROPONENT");
        proponent.setStatus("APPROVED");
        proponent.setNotifyPreferencesConfigured(true);
        proponent.setNotifyProposals(List.of());
        proponent.setNotifyDeadlines(List.of(NotificationPreferenceKeys.DEADLINE_REVISION));
        proponent = userRepository.saveAndFlush(proponent);
    }

    private Proposal dueProposal() {
        Proposal proposal = new Proposal();
        proposal.setProjectTitle("Deadline project");
        proposal.setProponent(proponent);
        proposal.setStatus("RPS_RETURNED");
        proposal.setRevisionDeadline(LocalDateTime.of(2026, 11, 21, 23, 59));
        return proposalRepository.saveAndFlush(proposal);
    }

    @Test
    void twoSweepsOverTheSameDeadlineProduceOneRow() {
        dueProposal();

        reminderService.remindDue(LocalDateTime.of(2026, 11, 19, 9, 0));
        reminderService.remindDue(LocalDateTime.of(2026, 11, 19, 10, 0));

        List<Notification> rows = notificationRepository.findByUserOrderByCreatedAtDesc(proponent);
        assertEquals(1, rows.size());
        assertEquals("REVISION_DEADLINE_REMINDER", rows.get(0).getType());
        assertEquals("Revision Deadline Reminder", rows.get(0).getTitle());
    }

    @Test
    void anAccountWithoutTheDeadlinePreferenceGetsNoRow() {
        proponent.setNotifyPreferencesConfigured(true);
        proponent.setNotifyDeadlines(List.of());
        userRepository.saveAndFlush(proponent);
        dueProposal();

        reminderService.remindDue(LocalDateTime.of(2026, 11, 19, 9, 0));

        assertEquals(0, notificationRepository.findByUserOrderByCreatedAtDesc(proponent).size());
    }
}
