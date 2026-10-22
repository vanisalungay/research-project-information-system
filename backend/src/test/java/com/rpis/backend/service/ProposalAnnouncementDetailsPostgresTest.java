package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalAnnouncementRequest;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed tests for the expanded Proposal Announcement form: the RIIDE
 * priority agenda, the standard reminder selection and the optional additional
 * announcement text must be saved, restored and editable without disturbing the
 * existing name/date/status behaviour or the submission-window lookup.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProposalAnnouncementDetailsPostgresTest {

    @Autowired
    private ProposalAnnouncementService service;
    @Autowired
    private ProposalAnnouncementRepository announcementRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
    }

    private ProposalAnnouncementRequest request(String name, String start, String end, String status) {
        ProposalAnnouncementRequest request = new ProposalAnnouncementRequest();
        request.setName(name);
        request.setStartDate(LocalDate.parse(start));
        request.setEndDate(LocalDate.parse(end));
        request.setStatus(status);
        return request;
    }

    /** Reads the announcement back from the database, so assertions cover persisted values. */
    private ProposalAnnouncement reload(Long id) {
        return announcementRepository.findById(id).orElseThrow();
    }

    // 1. An announcement may be created with no RIIDE program selected.
    @Test
    void newAnnouncementWithoutAgendasIsAccepted() {
        ProposalAnnouncement created =
                service.createAnnouncement(request("No Agenda Call", "2026-10-01", "2026-10-31", "DRAFT"));

        ProposalAnnouncement saved = reload(created.getId());
        assertNotNull(saved.getPriorityAgendas());
        assertTrue(saved.getPriorityAgendas().isEmpty());
    }

    // 2. A brand-new announcement defaults to every standard reminder.
    @Test
    void newAnnouncementDefaultsToEveryStandardReminder() {
        ProposalAnnouncement created =
                service.createAnnouncement(request("Default Reminders", "2026-10-01", "2026-10-31", "DRAFT"));

        assertEquals(
                List.of("research-plan", "eligible-submitters", "collaboration"),
                reload(created.getId()).getReminderIds());
    }

    // 3. Selecting a single program saves exactly its stable identifier.
    @Test
    void selectedProgramIdentifierIsPersisted() {
        ProposalAnnouncementRequest request =
                request("Single Agenda", "2026-10-01", "2026-10-31", "DRAFT");
        request.setPriorityAgendas(List.of("dagat"));

        ProposalAnnouncement created = service.createAnnouncement(request);

        assertEquals(List.of("dagat"), reload(created.getId()).getPriorityAgendas());
    }

    // 4. Selecting multiple programs saves every identifier.
    @Test
    void multipleSelectedProgramsAreAllPersisted() {
        ProposalAnnouncementRequest request =
                request("Multi Agenda", "2026-10-01", "2026-10-31", "DRAFT");
        request.setPriorityAgendas(List.of("dagat", "negosyo", "tanglaw"));

        ProposalAnnouncement created = service.createAnnouncement(request);

        assertEquals(
                List.of("dagat", "negosyo", "tanglaw"),
                reload(created.getId()).getPriorityAgendas());
    }

    // 5. Reminders can be deselected independently of one another.
    @Test
    void remindersCanBeDeselectedIndependently() {
        ProposalAnnouncementRequest request =
                request("Partial Reminders", "2026-10-01", "2026-10-31", "DRAFT");
        request.setReminderIds(List.of("collaboration"));

        ProposalAnnouncement created = service.createAnnouncement(request);

        assertEquals(List.of("collaboration"), reload(created.getId()).getReminderIds());
    }

    // 6. An empty additional announcement is accepted.
    @Test
    void blankAdditionalAnnouncementIsAccepted() {
        ProposalAnnouncementRequest request =
                request("No Extra Text", "2026-10-01", "2026-10-31", "DRAFT");
        request.setAdditionalAnnouncement("   ");

        ProposalAnnouncement created = service.createAnnouncement(request);

        assertNull(reload(created.getId()).getAdditionalAnnouncement());
    }

    // 7. Additional announcement text is stored with its line breaks, trimmed at the ends.
    @Test
    void additionalAnnouncementTextKeepsLineBreaks() {
        ProposalAnnouncementRequest request =
                request("Extra Text", "2026-10-01", "2026-10-31", "DRAFT");
        request.setAdditionalAnnouncement(
                "\n  Submit through the RPIS portal.\r\nBudget ceiling: PHP 500,000.  \n");

        ProposalAnnouncement created = service.createAnnouncement(request);

        assertEquals(
                "Submit through the RPIS portal.\r\nBudget ceiling: PHP 500,000.",
                reload(created.getId()).getAdditionalAnnouncement());
    }

    // 8. Editing restores the saved selection/text and can replace it.
    @Test
    void editingRestoresSavedSelectionsAndText() {
        ProposalAnnouncementRequest createRequest =
                request("Editable Call", "2026-10-01", "2026-10-31", "DRAFT");
        createRequest.setPriorityAgendas(List.of("punla"));
        createRequest.setReminderIds(List.of("research-plan"));
        createRequest.setAdditionalAnnouncement("Original note.");
        ProposalAnnouncement created = service.createAnnouncement(createRequest);

        // What the Edit form loads.
        ProposalAnnouncement loaded = service.getAnnouncementById(created.getId());
        assertEquals(List.of("punla"), loaded.getPriorityAgendas());
        assertEquals(List.of("research-plan"), loaded.getReminderIds());
        assertEquals("Original note.", loaded.getAdditionalAnnouncement());

        // The user changes every new value and saves.
        ProposalAnnouncementRequest updateRequest =
                request("Editable Call", "2026-10-01", "2026-10-31", "DRAFT");
        updateRequest.setPriorityAgendas(List.of("kalikasan", "tanglaw"));
        updateRequest.setReminderIds(List.of("eligible-submitters", "collaboration"));
        updateRequest.setAdditionalAnnouncement("Updated note.\nSecond line.");
        service.updateAnnouncement(created.getId(), updateRequest);

        ProposalAnnouncement saved = reload(created.getId());
        assertEquals(List.of("kalikasan", "tanglaw"), saved.getPriorityAgendas());
        assertEquals(List.of("eligible-submitters", "collaboration"), saved.getReminderIds());
        assertEquals("Updated note.\nSecond line.", saved.getAdditionalAnnouncement());
    }

    // 9. Updating the new fields leaves the existing announcement data untouched.
    @Test
    void updatingTheNewFieldsLeavesExistingAnnouncementDataUntouched() {
        ProposalAnnouncement created =
                service.createAnnouncement(request("Untouched Call", "2026-10-01", "2026-10-31", "ACTIVE"));
        ProposalAnnouncement before = reload(created.getId());

        ProposalAnnouncementRequest updateRequest =
                request("Untouched Call", "2026-10-01", "2026-10-31", "ACTIVE");
        updateRequest.setPriorityAgendas(List.of("dagat"));
        updateRequest.setReminderIds(List.of("collaboration"));
        updateRequest.setAdditionalAnnouncement("Only the new fields change.");
        service.updateAnnouncement(created.getId(), updateRequest);

        ProposalAnnouncement after = reload(created.getId());
        assertEquals(before.getName(), after.getName());
        assertEquals(before.getStartDate(), after.getStartDate());
        assertEquals(before.getEndDate(), after.getEndDate());
        assertEquals("ACTIVE", after.getStatus());
        assertEquals(before.getCreatedAt(), after.getCreatedAt());
        assertEquals(List.of("dagat"), after.getPriorityAgendas());
    }

    // 10. An update that does not send the new fields never resets them to defaults.
    @Test
    void updatingWithoutTheNewFieldsKeepsTheSavedSelection() {
        ProposalAnnouncementRequest createRequest =
                request("Keep Selection", "2026-10-01", "2026-10-31", "DRAFT");
        createRequest.setPriorityAgendas(List.of("dagat"));
        createRequest.setReminderIds(List.of("eligible-submitters"));
        createRequest.setAdditionalAnnouncement("Keep this text.");
        ProposalAnnouncement created = service.createAnnouncement(createRequest);

        // Request carrying only the pre-existing fields (new fields left null).
        service.updateAnnouncement(created.getId(),
                request("Keep Selection", "2026-10-02", "2026-11-30", "DRAFT"));

        ProposalAnnouncement saved = reload(created.getId());
        assertEquals(List.of("dagat"), saved.getPriorityAgendas());
        assertEquals(List.of("eligible-submitters"), saved.getReminderIds());
        assertEquals("Keep this text.", saved.getAdditionalAnnouncement());
        assertEquals(LocalDate.parse("2026-10-02"), saved.getStartDate());
        assertEquals(LocalDate.parse("2026-11-30"), saved.getEndDate());
    }

    // 11. Announcements created before this enhancement (NULL columns) stay editable.
    @Test
    void announcementsCreatedBeforeTheEnhancementStayEditable() {
        jdbcTemplate.update(
                "INSERT INTO proposal_announcements (name, start_date, end_date, status) VALUES (?, ?, ?, ?)",
                "Legacy Call", LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"), "CLOSED");
        Long legacyId = jdbcTemplate.queryForObject(
                "SELECT id FROM proposal_announcements WHERE name = ?", Long.class, "Legacy Call");

        ProposalAnnouncement legacy = service.getAnnouncementById(legacyId);
        assertNotNull(legacy.getPriorityAgendas());
        assertNotNull(legacy.getReminderIds());
        assertTrue(legacy.getPriorityAgendas().isEmpty());
        assertTrue(legacy.getReminderIds().isEmpty());
        assertNull(legacy.getAdditionalAnnouncement());

        ProposalAnnouncementRequest updateRequest =
                request("Legacy Call", "2026-09-01", "2026-09-30", "CLOSED");
        updateRequest.setPriorityAgendas(List.of("tanglaw"));
        updateRequest.setReminderIds(List.of("research-plan"));
        updateRequest.setAdditionalAnnouncement("Filled in later.");
        service.updateAnnouncement(legacyId, updateRequest);

        ProposalAnnouncement saved = reload(legacyId);
        assertEquals(List.of("tanglaw"), saved.getPriorityAgendas());
        assertEquals(List.of("research-plan"), saved.getReminderIds());
        assertEquals("Filled in later.", saved.getAdditionalAnnouncement());
        assertEquals("Legacy Call", saved.getName());
    }

    // 12. Unknown/duplicated identifiers are dropped and normalised to canonical keys.
    @Test
    void unknownOrDuplicatedIdentifiersAreDroppedAndNormalized() {
        ProposalAnnouncementRequest request =
                request("Messy Selection", "2026-10-01", "2026-10-31", "DRAFT");
        request.setPriorityAgendas(List.of("DAGAT", "dagat", "not-a-program"));
        request.setReminderIds(List.of("COLLABORATION", "bogus-reminder"));

        ProposalAnnouncement created = service.createAnnouncement(request);

        ProposalAnnouncement saved = reload(created.getId());
        assertEquals(List.of("dagat"), saved.getPriorityAgendas());
        assertEquals(List.of("collaboration"), saved.getReminderIds());
    }

    // 13. The ACTIVE submission window still resolves when the new fields are set.
    @Test
    void activeSubmissionWindowStillResolvesWithTheNewFields() {
        LocalDate today = LocalDate.now();
        ProposalAnnouncementRequest request = request(
                "Open Window", today.minusDays(1).toString(), today.plusDays(1).toString(), "ACTIVE");
        request.setPriorityAgendas(List.of("kalikasan"));
        request.setReminderIds(List.of("research-plan", "collaboration"));
        request.setAdditionalAnnouncement("Window note.");
        ProposalAnnouncement created = service.createAnnouncement(request);

        assertEquals(created.getId(), service.getActiveAnnouncement().getId());
        assertNotNull(service.getActiveAnnouncementOrNull());
        assertTrue(service.hasActiveAnnouncement());
    }
}
