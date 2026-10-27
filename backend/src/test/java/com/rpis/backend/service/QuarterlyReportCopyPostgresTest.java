package com.rpis.backend.service;

import com.rpis.backend.dto.NextReportStageResponse;
import com.rpis.backend.dto.QuarterlyProgressReportRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.QuarterlyObjective;
import com.rpis.backend.model.QuarterlyOutput;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.model.User;
import com.rpis.backend.model.WorkplanData;
import com.rpis.backend.model.WorkplanObjective;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.QuarterlyProgressReportRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PostgreSQL-backed tests for the "Create a Copy" starting point of a quarterly progress
 * report: the copy is a brand-new record (its own identifier, its own reporting period,
 * saved as a draft by the existing workflow) that only references the report it was
 * started from, while the previous report keeps its own status, content and submission
 * details - and a draft that already exists for the target period is continued instead of
 * being duplicated.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuarterlyReportCopyPostgresTest {

    @Autowired
    private QuarterlyProgressReportService service;
    @Autowired
    private QuarterlyProgressReportRepository reportRepository;
    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Proposal proposal;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE quarterly_progress_reports RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        User proponent = new User();
        proponent.setName("Copy Proponent");
        proponent.setEmail("copy.proponent@msunaawan.edu.ph");
        proponent.setPassword("secret");
        proponent.setRole("PROPONENT");
        proponent.setStatus("APPROVED");
        User savedProponent = userRepository.saveAndFlush(proponent);

        Proposal p = new Proposal();
        p.setProjectTitle("Fish Landing Survey");
        p.setProponent(savedProponent);
        p.setStatus("APPROVED");
        p.setSoNumber("SO-2026-001");
        proposal = proposalRepository.saveAndFlush(p);
    }

    /** A submitted Q1 report with content, stored the way the Q1 workflow stores it. */
    private QuarterlyProgressReport submittedQuarterOne() {
        QuarterlyProgressReport q1 = new QuarterlyProgressReport();
        q1.setProposalId(proposal.getId());
        q1.setReportType(QuarterlyProgressReportService.TYPE_QUARTERLY);
        q1.setPeriod("Q1");
        q1.setYear(2026);
        q1.setStatus("SUBMITTED");
        q1.setProjectTitle("Q1 title");
        q1.setCatchUpPlan("Q1 catch-up plan");
        q1.setObjectives(List.of(new QuarterlyObjective("Objective one", "100%", "80%", "80%", "40%")));
        q1.setOutputs(List.of(new QuarterlyOutput("Publications", "One article", "Draft only")));
        q1.setWorkplan(workplan());
        q1.setSubmittedBy(proposal.getProponent().getId());
        q1.setSubmittedAt(LocalDateTime.now().minusDays(30));
        return reportRepository.saveAndFlush(q1);
    }

    private WorkplanData workplan() {
        WorkplanData wp = new WorkplanData();
        wp.setAsOf("2026-03-31");
        wp.setProjectTitle("Fish Landing Survey");
        wp.setProjectLeader("Copy Proponent");
        wp.setProjectDuration("12");
        wp.setObjectives(List.of(new WorkplanObjective("Objective one", List.of("Process PRs"), "100%")));
        wp.setRemarks("On track.");
        return wp;
    }

    /** The payload the existing report form sends for a draft started from a copy. */
    private QuarterlyProgressReportRequest copyRequest(Long sourceId, String title) {
        QuarterlyProgressReportRequest req = new QuarterlyProgressReportRequest();
        req.setProposalId(proposal.getId());
        req.setReportType(QuarterlyProgressReportService.TYPE_QUARTERLY);
        req.setPeriod("Q2");
        req.setStatus("DRAFT");
        req.setCopiedFromReportId(sourceId);
        req.setProjectTitle(title);
        req.setCatchUpPlan("Q1 catch-up plan");
        req.setObjectives(List.of(new QuarterlyObjective("Objective one", "100%", "", "", "")));
        req.setWorkplan(workplan());
        return req;
    }

    /** The stored row of a report, read straight from the database. */
    private Map<String, Object> storedRow(Long id) {
        // Flush the persistence context first, so this raw read really reflects the
        // database state instead of an unflushed in-memory entity.
        reportRepository.flush();
        return jdbcTemplate.queryForMap(
                "SELECT status, period, report_type, project_title, submitted_at, copied_from_report_id "
                        + "FROM quarterly_progress_reports WHERE id = ?",
                id);
    }

    // 1. A copy becomes its own draft for the next required reporting period, with its own
    //    identifier, and records the report it was started from.
    @Test
    void aCopyBecomesItsOwnDraftForTheNextReportingPeriod() {
        QuarterlyProgressReport q1 = submittedQuarterOne();

        QuarterlyProgressReport q2 = service.create(copyRequest(q1.getId(), "Q2 title"));

        assertNotEquals(q1.getId(), q2.getId(), "the copy has its own identifier");
        assertEquals(proposal.getId(), q2.getProposalId(), "the copy belongs to the same project");
        assertEquals("Q2", q2.getPeriod(), "the next reporting period comes from the workflow");
        assertEquals("DRAFT", q2.getStatus(), "a copy is never submitted automatically");
        assertNull(q2.getSubmittedAt());
        assertEquals(q1.getId(), q2.getCopiedFromReportId(), "the source report is recorded");

        // The next required stage really is Q2: the copy never reuses the source period.
        NextReportStageResponse stage = service.getNextStage(proposal.getId());
        assertEquals("Q2", stage.getPeriod());
        assertEquals(q2.getId(), stage.getDraftReportId());
    }

    // 2. The previous report keeps its own status, content and submission details, and the
    //    copy keeps its own content when it is edited.
    @Test
    void theSourceReportIsNeverChangedByTheCopyOrByEditingTheCopy() {
        QuarterlyProgressReport q1 = submittedQuarterOne();
        QuarterlyProgressReport q2 = service.create(copyRequest(q1.getId(), "Q2 title"));

        QuarterlyProgressReportRequest edit = copyRequest(q1.getId(), "Q2 revised title");
        edit.setObjectives(
                List.of(new QuarterlyObjective("Objective one", "100%", "25%", "25%", "12%")));
        service.update(q2.getId(), edit);

        Map<String, Object> source = storedRow(q1.getId());
        assertEquals("SUBMITTED", source.get("status"), "the source stays submitted");
        assertEquals("Q1", source.get("period"));
        assertEquals("Q1 title", source.get("project_title"));
        assertNotNull(source.get("submitted_at"), "its submission date is intact");
        assertNull(source.get("copied_from_report_id"), "the source is not a copy of anything");

        Map<String, Object> copy = storedRow(q2.getId());
        assertEquals("DRAFT", copy.get("status"));
        assertEquals("Q2", copy.get("period"));
        assertEquals("Q2 revised title", copy.get("project_title"));
        assertEquals(q1.getId(), ((Number) copy.get("copied_from_report_id")).longValue());

        // The stored source content is untouched as well.
        QuarterlyProgressReport reloaded = reportRepository.findById(q1.getId()).orElseThrow();
        assertEquals("80%", reloaded.getObjectives().get(0).getActual());
        assertEquals("Objective one", reloaded.getWorkplan().getObjectives().get(0).getObjective());
    }

    // 3. Submitting the copied report follows the existing workflow: it becomes SUBMITTED
    //    with its own submission details, and the source stays exactly as it was.
    @Test
    void submittingTheCopiedReportKeepsItsOwnData() {
        QuarterlyProgressReport q1 = submittedQuarterOne();
        QuarterlyProgressReport q2 = service.create(copyRequest(q1.getId(), "Q2 title"));

        QuarterlyProgressReportRequest submit = copyRequest(q1.getId(), "Q2 title");
        submit.setStatus("SUBMITTED");
        QuarterlyProgressReport submitted = service.update(q2.getId(), submit);

        assertEquals("SUBMITTED", submitted.getStatus());
        assertNotNull(submitted.getSubmittedAt());
        assertEquals(q1.getId(), submitted.getCopiedFromReportId());
        assertEquals("SUBMITTED", storedRow(q1.getId()).get("status"));
        assertEquals("Q1", storedRow(q1.getId()).get("period"));
    }


    // 4. A draft that already exists for the target period is continued instead of being
    //    duplicated, and it keeps the reference recorded the first time.
    @Test
    void anExistingDraftForTheTargetPeriodIsContinuedInsteadOfDuplicated() {
        QuarterlyProgressReport q1 = submittedQuarterOne();

        QuarterlyProgressReport first = service.create(copyRequest(q1.getId(), "Q2 title"));
        QuarterlyProgressReport second = service.create(copyRequest(q1.getId(), "Q2 title again"));

        assertEquals(first.getId(), second.getId(), "the existing draft is reused");
        assertEquals(
                1,
                reportRepository.findByProposalIdOrderByCreatedAtAsc(proposal.getId()).stream()
                        .filter(r -> "Q2".equals(r.getPeriod()))
                        .count(),
                "no duplicate Q2 row");
        assertEquals(q1.getId(), storedRow(first.getId()).get("copied_from_report_id"));
    }

    // 5. Only a report of the same project is ever referenced.
    @Test
    void aCopyReferenceToAnotherProjectsReportIsNotStored() {
        QuarterlyProgressReport q1 = submittedQuarterOne();

        Proposal other = new Proposal();
        other.setProjectTitle("Another project");
        other.setProponent(proposal.getProponent());
        other.setStatus("APPROVED");
        other.setSoNumber("SO-2026-002");
        Proposal otherProposal = proposalRepository.saveAndFlush(other);

        QuarterlyProgressReport foreign = new QuarterlyProgressReport();
        foreign.setProposalId(otherProposal.getId());
        foreign.setReportType(QuarterlyProgressReportService.TYPE_QUARTERLY);
        foreign.setPeriod("Q1");
        foreign.setYear(2026);
        foreign.setStatus("SUBMITTED");
        QuarterlyProgressReport foreignReport = reportRepository.saveAndFlush(foreign);

        QuarterlyProgressReportRequest req = copyRequest(foreignReport.getId(), "Q2 title");
        req.setProposalId(proposal.getId());
        QuarterlyProgressReport q2 = service.create(req);

        assertNull(q2.getCopiedFromReportId(), "another project's report is never referenced");
        assertEquals(q1.getProposalId(), q2.getProposalId());
        // The copy is still created normally, it simply carries no traceability reference.
        assertNotEquals(q1.getId(), q2.getId());
        assertEquals("Q2", q2.getPeriod());
    }
}
