package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalAnnouncementRequest;
import com.rpis.backend.dto.ProposalAnnouncementUpdateRequest;
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

    /**
     * The payload of an update: only the submission window and the status. The name,
     * priority agenda, reminders and additional announcement cannot be sent at all.
     */
    private ProposalAnnouncementUpdateRequest updateRequest(String start, String end, String status) {
        ProposalAnnouncementUpdateRequest request = new ProposalAnnouncementUpdateRequest();
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

    // 8. An update keeps the saved selection and text: they are shown read-only and can no
    //    longer be replaced through the update endpoint.
    @Test
    void anUpdateKeepsTheSavedSelectionsAndText() {
        ProposalAnnouncementRequest createRequest =
                request("Editable Call", "2026-10-01", "2026-10-31", "DRAFT");
        createRequest.setPriorityAgendas(List.of("punla"));
        createRequest.setReminderIds(List.of("research-plan"));
        createRequest.setAdditionalAnnouncement("Original note.");
        ProposalAnnouncement created = service.createAnnouncement(createRequest);

        // The Edit form loads the saved selection and text.
        ProposalAnnouncement loaded = service.getAnnouncementById(created.getId());
        assertEquals(List.of("punla"), loaded.getPriorityAgendas());
        assertEquals(List.of("research-plan"), loaded.getReminderIds());
        assertEquals("Original note.", loaded.getAdditionalAnnouncement());

        // An update moves the submission window only: the selection and the text keep the
        // values the call was created with.
        service.updateAnnouncement(created.getId(),
                updateRequest("2026-10-01", "2026-11-15", "DRAFT"));

        ProposalAnnouncement saved = reload(created.getId());
        assertEquals(List.of("punla"), saved.getPriorityAgendas());
        assertEquals(List.of("research-plan"), saved.getReminderIds());
        assertEquals("Original note.", saved.getAdditionalAnnouncement());
        assertEquals(LocalDate.parse("2026-11-15"), saved.getEndDate());
    }

    // 9. An update applies the window and the status and leaves the announcement data
    //    (name, selection, text, creation timestamp) untouched.
    @Test
    void updatingTheWindowLeavesExistingAnnouncementDataUntouched() {
        ProposalAnnouncementRequest createRequest =
                request("Untouched Call", "2026-10-01", "2026-10-31", "ACTIVE");
        createRequest.setPriorityAgendas(List.of("dagat"));
        createRequest.setReminderIds(List.of("collaboration"));
        createRequest.setAdditionalAnnouncement("Keep this body.");
        ProposalAnnouncement created = service.createAnnouncement(createRequest);
        ProposalAnnouncement before = reload(created.getId());

        service.updateAnnouncement(created.getId(),
                updateRequest("2026-10-01", "2026-12-20", "CLOSED"));

        ProposalAnnouncement after = reload(created.getId());
        assertEquals(before.getName(), after.getName());
        assertEquals(before.getStartDate(), after.getStartDate());
        assertEquals(before.getCreatedAt(), after.getCreatedAt());
        assertEquals(List.of("dagat"), after.getPriorityAgendas());
        assertEquals(List.of("collaboration"), after.getReminderIds());
        assertEquals("Keep this body.", after.getAdditionalAnnouncement());
        // The permitted fields did change.
        assertEquals(LocalDate.parse("2026-12-20"), after.getEndDate());
        assertEquals("CLOSED", after.getStatus());
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

        // The update carries only the submission window and the status.
        service.updateAnnouncement(created.getId(),
                updateRequest("2026-10-02", "2026-11-30", "DRAFT"));

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

        // An update only moves the submission window: a legacy row keeps its empty
        // selections and its (absent) additional text.
        service.updateAnnouncement(legacyId,
                updateRequest("2026-09-01", "2026-10-15", "CLOSED"));

        ProposalAnnouncement saved = reload(legacyId);
        assertTrue(saved.getPriorityAgendas().isEmpty());
        assertTrue(saved.getReminderIds().isEmpty());
        assertNull(saved.getAdditionalAnnouncement());
        assertEquals("Legacy Call", saved.getName());
        assertEquals(LocalDate.parse("2026-10-15"), saved.getEndDate());
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
