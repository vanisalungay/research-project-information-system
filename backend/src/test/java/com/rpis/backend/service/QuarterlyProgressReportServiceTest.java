package com.rpis.backend.service;

import com.rpis.backend.dto.NextReportStageResponse;
import com.rpis.backend.dto.QuarterlyProgressReportRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.QuarterlyObjective;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.model.User;
import com.rpis.backend.model.WorkplanData;
import com.rpis.backend.model.WorkplanObjective;
import com.rpis.backend.repository.OvcafValidationRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.QuarterlyProgressReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.rpis.backend.storage.LocalUploadStorage;

/**
 * Unit tests for the implementation-phase reporting sequence and required-document
 * validation of {@link QuarterlyProgressReportService}. These cover the rules that
 * are per-proposal (stages 1-3 = QPR + Project Workplan, stage 4 = Terminal Report)
 * without touching a database.
 */
class QuarterlyProgressReportServiceTest {

    private static final Long PROPOSAL_ID = 42L;

    private QuarterlyProgressReportRepository reportRepository;
    private ProposalRepository proposalRepository;
    private OvcafValidationRepository ovcafValidationRepository;
    private QuarterlyProgressReportService service;

    @BeforeEach
    void setUp() {
        reportRepository = mock(QuarterlyProgressReportRepository.class);
        proposalRepository = mock(ProposalRepository.class);
        ovcafValidationRepository = mock(OvcafValidationRepository.class);
        service = new QuarterlyProgressReportService(reportRepository, proposalRepository,
                new LocalUploadStorage("uploads"), ovcafValidationRepository);

        Proposal proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setStatus("APPROVED");
        proposal.setSoNumber("SO-2026-001");
        proposal.setSoUploadedAt(LocalDateTime.of(2026, 7, 28, 23, 20));
        proposal.setProjectTitle("Mangrove Restoration");
        proposal.setProjectLeader("Andrea Mae Villanueva");
        proposal.setProjectLeaderSex("Female");
        proposal.setDuration("12");
        proposal.setStartDate(LocalDate.of(2026, 1, 1));
        proposal.setEndDate(LocalDate.of(2026, 12, 31));
        User proponent = new User();
        proponent.setId(7L);
        proposal.setProponent(proponent);
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(reportRepository.save(any(QuarterlyProgressReport.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private QuarterlyProgressReport report(String type, String period, String status) {
        QuarterlyProgressReport r = new QuarterlyProgressReport();
        r.setProposalId(PROPOSAL_ID);
        r.setReportType(type);
        r.setPeriod(period);
        r.setStatus(status);
        return r;
    }

    private void stubReports(QuarterlyProgressReport... reports) {
        when(reportRepository.findByProposalIdOrderByCreatedAtAsc(PROPOSAL_ID))
                .thenReturn(new ArrayList<>(List.of(reports)));
    }

    private QuarterlyProgressReportRequest request(String type, String period, String status) {
        QuarterlyProgressReportRequest req = new QuarterlyProgressReportRequest();
        req.setProposalId(PROPOSAL_ID);
        req.setReportType(type);
        req.setPeriod(period);
        req.setStatus(status);
        return req;
    }

    @Test
    void nextStageIsQ1WithWorkplanWhenNothingSubmitted() {
        stubReports();
        NextReportStageResponse stage = service.getNextStage(PROPOSAL_ID);
        assertEquals("QUARTERLY_PROGRESS", stage.getReportType());
        assertEquals("Q1", stage.getPeriod());
        assertTrue(stage.isRequiresWorkplan());
        assertFalse(stage.isAllStagesComplete());
    }

    @Test
    void draftDoesNotAdvanceTheSequence() {
        QuarterlyProgressReport draft = report("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        draft.setId(11L);
        stubReports(draft);
        NextReportStageResponse stage = service.getNextStage(PROPOSAL_ID);
        assertEquals("Q1", stage.getPeriod());
        assertEquals(11L, stage.getDraftReportId());
    }

    @Test
    void sequenceAdvancesOnlyForSubmittedQuarters() {
        stubReports(
                report("QUARTERLY_PROGRESS", "Q1", "SUBMITTED"),
                report("QUARTERLY_PROGRESS", "Q2", "DRAFT"));
        assertEquals("Q2", service.getNextStage(PROPOSAL_ID).getPeriod());
    }

    @Test
    void terminalStageIsRequiredAfterThreeQuarters() {
        stubReports(
                report("QUARTERLY_PROGRESS", "Q1", "SUBMITTED"),
                report("QUARTERLY_PROGRESS", "Q2", "SUBMITTED"),
                report("QUARTERLY_PROGRESS", "Q3", "SUBMITTED"));
        NextReportStageResponse stage = service.getNextStage(PROPOSAL_ID);
        assertEquals("TERMINAL_REPORT", stage.getReportType());
        assertEquals("TERMINAL", stage.getPeriod());
        assertTrue(stage.isRequiresTerminalReport());
        assertFalse(stage.isRequiresWorkplan());
    }

    @Test
    void allStagesCompleteAfterTerminalReportSubmitted() {
        stubReports(
                report("QUARTERLY_PROGRESS", "Q1", "SUBMITTED"),
                report("QUARTERLY_PROGRESS", "Q2", "SUBMITTED"),
                report("QUARTERLY_PROGRESS", "Q3", "SUBMITTED"),
                report("TERMINAL_REPORT", "TERMINAL", "SUBMITTED"));
        assertTrue(service.getNextStage(PROPOSAL_ID).isAllStagesComplete());
    }

    @Test
    void legacyRowsWithNullReportTypeCountAsQuarterlyReports() {
        QuarterlyProgressReport legacy = new QuarterlyProgressReport();
        legacy.setProposalId(PROPOSAL_ID);
        legacy.setPeriod("Q1");
        legacy.setStatus("SUBMITTED");
        legacy.setReportType(null);
        stubReports(legacy);
        assertEquals("Q2", service.getNextStage(PROPOSAL_ID).getPeriod());
    }

    @Test
    void nextStageRequiresAnEligibleProposal() {
        Proposal draft = new Proposal();
        draft.setId(99L);
        draft.setStatus("DRAFT");
        when(proposalRepository.findById(99L)).thenReturn(Optional.of(draft));
        assertThrows(IllegalStateException.class, () -> service.getNextStage(99L));
    }

    @Test
    void anApprovedProposalWithoutAnIssuedSoIsNotEligible() {
        Proposal approvedWithoutSo = new Proposal();
        approvedWithoutSo.setId(100L);
        approvedWithoutSo.setStatus("APPROVED");
        approvedWithoutSo.setSoNumber("   ");
        when(proposalRepository.findById(100L)).thenReturn(Optional.of(approvedWithoutSo));
        assertFalse(service.isEligible(approvedWithoutSo));
        assertThrows(IllegalStateException.class, () -> service.getNextStage(100L));
    }

    @Test
    void anSoDoesNotMakeAnEarlyStageProposalEligible() {
        Proposal underReview = new Proposal();
        underReview.setId(101L);
        underReview.setStatus("UNDER_REVIEW");
        underReview.setSoNumber("SO-2026-010");
        when(proposalRepository.findById(101L)).thenReturn(Optional.of(underReview));
        assertFalse(service.isEligible(underReview));
        assertThrows(IllegalStateException.class, () -> service.getNextStage(101L));
    }

    @Test
    void aReleasedProposalWithAnIssuedSoIsEligible() {
        Proposal released = new Proposal();
        released.setId(102L);
        released.setStatus("RELEASED");
        released.setSoNumber("SO-2026-011");
        released.setSoUploadedAt(LocalDateTime.of(2026, 7, 28, 23, 20));
        assertTrue(service.isEligible(released));
    }

    /**
     * IMPLEMENTATION is the implementation-phase status the rest of the application
     * (proposalStatus.js / proposalSummary.js) already treats as approved for
     * implementation. Such a project must not be silently denied reporting.
     */
    @Test
    void anImplementationPhaseProposalWithAnIssuedSoIsEligible() {
        Proposal implementing = new Proposal();
        implementing.setId(103L);
        implementing.setStatus("IMPLEMENTATION");
        implementing.setSoNumber("SO-2026-012");
        implementing.setSoUploadedAt(LocalDateTime.of(2026, 7, 28, 23, 20));
        assertTrue(service.isEligible(implementing));
    }

    @Test
    void theStatusComparisonToleratesCasingAndSurroundingWhitespace() {
        Proposal mixedCase = new Proposal();
        mixedCase.setId(104L);
        mixedCase.setStatus(" approved ");
        mixedCase.setSoNumber("SO-2026-013");
        mixedCase.setSoUploadedAt(LocalDateTime.of(2026, 7, 28, 23, 20));
        assertTrue(service.isEligible(mixedCase));
    }

    /**
     * The SO number alone is not the OC approval record: the OC final-approval
     * transaction writes the number and its time together. A value typed straight
     * into the column must not make a project eligible.
     */
    @Test
    void anSoNumberWithoutTheRecordedApprovalTimeIsNotEligible() {
        Proposal manualSo = new Proposal();
        manualSo.setId(106L);
        manualSo.setStatus("APPROVED");
        manualSo.setSoNumber("SO-2026-015");
        assertFalse(service.isEligible(manualSo));
    }

    /**
     * When the Chancellor routed the proposal through budget evaluation, the
     * persisted OVCAF approval is part of the chain; a status that merely contains
     * APPROVED is not.
     */
    @Test
    void aBudgetRoutedProposalWithoutOvcafApprovalIsNotEligible() {
        Proposal withBudget = new Proposal();
        withBudget.setId(107L);
        withBudget.setStatus("APPROVED");
        withBudget.setSoNumber("SO-2026-016");
        withBudget.setSoUploadedAt(LocalDateTime.of(2026, 7, 28, 23, 20));
        withBudget.setNeedsBudget(true);
        when(ovcafValidationRepository.existsByProposalIdAndDecisionAndBudgetEndorsedTrue(
                107L, "APPROVED_ENDORSED")).thenReturn(false);
        assertFalse(service.isEligible(withBudget));

        when(ovcafValidationRepository.existsByProposalIdAndDecisionAndBudgetEndorsedTrue(
                107L, "APPROVED_ENDORSED")).thenReturn(true);
        assertTrue(service.isEligible(withBudget));
    }

    @Test
    void aBudgetRoutedProposalAwaitingOvcafIsNotEligible() {
        Proposal awaiting = new Proposal();
        awaiting.setId(108L);
        awaiting.setStatus("FOR_OVCAF_APPROVAL");
        awaiting.setSoNumber("SO-2026-017");
        awaiting.setSoUploadedAt(LocalDateTime.of(2026, 7, 28, 23, 20));
        awaiting.setNeedsBudget(true);
        assertFalse(service.isEligible(awaiting));
    }

    @Test
    void aBlankStatusIsNotApproval() {
        Proposal blank = new Proposal();
        blank.setId(105L);
        blank.setStatus("   ");
        blank.setSoNumber("SO-2026-014");
        assertFalse(service.isEligible(blank));
    }

    @Test
    void createRejectsAStageThatIsNotTheNextRequiredOne() {
        stubReports();
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request("QUARTERLY_PROGRESS", "Q3", "DRAFT")));
    }

    @Test
    void createRejectsResubmissionOfAnAlreadySubmittedStage() {
        stubReports(report("QUARTERLY_PROGRESS", "Q1", "SUBMITTED"));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request("QUARTERLY_PROGRESS", "Q1", "DRAFT")));
    }

    @Test
    void createReusesAnExistingDraftInsteadOfDuplicatingIt() {
        stubReports(report("QUARTERLY_PROGRESS", "Q1", "DRAFT"));
        service.create(request("QUARTERLY_PROGRESS", "Q1", "DRAFT"));
        verify(reportRepository, times(1)).save(any(QuarterlyProgressReport.class));
    }

    @Test
    void quarterlySubmissionWithoutWorkplanIsRejected() {
        stubReports();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(request("QUARTERLY_PROGRESS", "Q1", "SUBMITTED")));
        assertTrue(ex.getMessage().toLowerCase().contains("workplan"));
    }

    @Test
    void workplanRequirementIsFormBasedAndDoesNotMentionAFileUpload() {
        stubReports();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(request("QUARTERLY_PROGRESS", "Q1", "SUBMITTED")));
        String message = ex.getMessage().toLowerCase();
        assertTrue(message.contains("workplan"));
        assertFalse(message.contains("file"));
    }

    @Test
    void quarterlySubmissionWithWorkplanIsAccepted() {
        QuarterlyProgressReport draft = report("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        draft.setWorkplanFilePath("uploads/reports/workplan.pdf");
        stubReports(draft);
        QuarterlyProgressReport saved = service.create(request("QUARTERLY_PROGRESS", "Q1", "SUBMITTED"));
        assertEquals("SUBMITTED", saved.getStatus());
    }

    @Test
    void terminalSubmissionWithoutTerminalReportIsRejected() {
        stubReports(
                report("QUARTERLY_PROGRESS", "Q1", "SUBMITTED"),
                report("QUARTERLY_PROGRESS", "Q2", "SUBMITTED"),
                report("QUARTERLY_PROGRESS", "Q3", "SUBMITTED"));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(request("TERMINAL_REPORT", "TERMINAL", "SUBMITTED")));
        assertTrue(ex.getMessage().toLowerCase().contains("terminal"));
    }

    @Test
    void terminalSubmissionWithTerminalReportIsAccepted() {
        QuarterlyProgressReport draft = report("TERMINAL_REPORT", "TERMINAL", "DRAFT");
        draft.setTerminalReportFilePath("uploads/reports/terminal.pdf");
        stubReports(
                report("QUARTERLY_PROGRESS", "Q1", "SUBMITTED"),
                report("QUARTERLY_PROGRESS", "Q2", "SUBMITTED"),
                report("QUARTERLY_PROGRESS", "Q3", "SUBMITTED"),
                draft);
        QuarterlyProgressReport saved = service.create(request("TERMINAL_REPORT", "TERMINAL", "SUBMITTED"));
        assertEquals("SUBMITTED", saved.getStatus());
        assertEquals("TERMINAL_REPORT", saved.getReportType());
    }

    @Test
    void submittedReportCannotBeEdited() {
        QuarterlyProgressReport submitted = report("QUARTERLY_PROGRESS", "Q1", "SUBMITTED");
        submitted.setId(5L);
        when(reportRepository.findById(5L)).thenReturn(Optional.of(submitted));
        assertThrows(IllegalStateException.class,
                () -> service.update(5L, request("QUARTERLY_PROGRESS", "Q1", "DRAFT")));
    }

    @Test
    void quarterlySubmissionWithStructuredWorkplanIsAccepted() {
        QuarterlyProgressReport draft = report("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        draft.setWorkplan(workplan("Identify the different fishes at market and landing sites"));
        stubReports(draft);
        QuarterlyProgressReport saved = service.create(request("QUARTERLY_PROGRESS", "Q1", "SUBMITTED"));
        assertEquals("SUBMITTED", saved.getStatus());
    }

    @Test
    void structuredWorkplanIsPersistedThroughCreate() {
        stubReports();
        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        req.setWorkplan(workplan("Objective one"));
        QuarterlyProgressReport saved = service.create(req);
        assertNotNull(saved.getWorkplan());
        assertEquals("Objective one", saved.getWorkplan().getObjectives().get(0).getObjective());
        assertEquals("100", saved.getWorkplan().getObjectives().get(0).getTotal(),
                "the client-sent total is replaced by the derived average");
        assertEquals("2026-03-31", saved.getWorkplan().getAsOf());
    }

    @Test
    void projectIdentificationComeFromTheStoredProposalNotTheRequest() {
        stubReports();
        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        req.setProjectTitle("A fabricated title");
        req.setProjectLeader("A fabricated leader");
        req.setLeaderGender("Other");
        req.setProjectDuration("99");
        req.setProjectStartDate("2000-01-01");
        req.setProjectEndDate("2000-12-31");

        QuarterlyProgressReport saved = service.create(req);

        assertEquals("Mangrove Restoration", saved.getProjectTitle());
        assertEquals("Andrea Mae Villanueva", saved.getProjectLeader());
        assertEquals("Female", saved.getLeaderGender());
        assertEquals("12", saved.getProjectDuration());
        assertEquals(LocalDate.of(2026, 1, 1), saved.getProjectStartDate());
        assertEquals(LocalDate.of(2026, 12, 31), saved.getProjectEndDate());
    }

    @Test
    void aSavedProjectSnapshotIsNotOverwrittenByAnUpdate() {
        QuarterlyProgressReport draft = report("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        draft.setId(9L);
        draft.setProjectTitle("Title recorded when the report was filed");
        when(reportRepository.findById(9L)).thenReturn(Optional.of(draft));

        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        req.setProjectTitle("A later title");
        service.update(9L, req);

        assertEquals("Title recorded when the report was filed", draft.getProjectTitle());
    }

    @Test
    void accomplishmentPercentagesAreStoredWithoutTheSign() {
        stubReports();
        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        QuarterlyObjective row = new QuarterlyObjective();
        row.setObjective("Objective one");
        row.setPercentagePeriod("80%");
        row.setPercentageCumulative("40.50%");
        req.setObjectives(List.of(row));

        QuarterlyProgressReport saved = service.create(req);

        assertEquals("80", saved.getObjectives().get(0).getPercentagePeriod());
        assertEquals("40.5", saved.getObjectives().get(0).getPercentageCumulative());
    }

    @Test
    void anInvalidPercentageRejectsTheSaveInsteadOfBeingStored() {
        stubReports();
        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        QuarterlyObjective row = new QuarterlyObjective();
        row.setPercentagePeriod("101");
        req.setObjectives(List.of(row));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(req));
        assertTrue(ex.getMessage().contains("0 to 100"));
    }

    @Test
    void workplanAverageIsDerivedAndTheClientTotalIsIgnored() {
        stubReports();
        WorkplanData wp = new WorkplanData();
        wp.setObjectives(List.of(
                new WorkplanObjective("Objective one",
                        List.of("50", "70", "", "", "", "", "", "", "", "", "", ""), "999")));
        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        req.setWorkplan(wp);

        WorkplanObjective saved = service.create(req).getWorkplan().getObjectives().get(0);

        assertEquals("60", saved.getTotal());
        assertEquals(12, saved.getMonths().size());
        assertEquals("", saved.getMonths().get(2));
    }

    @Test
    void workplanWithoutAnyObjectiveDoesNotSatisfyTheStageRequirement() {
        QuarterlyProgressReport draft = report("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        WorkplanData blank = new WorkplanData();
        blank.setObjectives(List.of(new WorkplanObjective("   ", List.of(""), "")));
        draft.setWorkplan(blank);
        stubReports(draft);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(request("QUARTERLY_PROGRESS", "Q1", "SUBMITTED")));
        assertTrue(ex.getMessage().toLowerCase().contains("workplan"));
    }

    @Test
    void unrelatedUpdateDoesNotWipeAnExistingWorkplan() {
        QuarterlyProgressReport draft = report("QUARTERLY_PROGRESS", "Q1", "DRAFT");
        draft.setId(9L);
        draft.setWorkplan(workplan("Existing objective"));
        when(reportRepository.findById(9L)).thenReturn(Optional.of(draft));
        // An update that carries no workplan (e.g. a terminal-stage style payload).
        service.update(9L, request("QUARTERLY_PROGRESS", "Q1", "DRAFT"));
        assertNotNull(draft.getWorkplan());
        assertEquals("Existing objective", draft.getWorkplan().getObjectives().get(0).getObjective());
    }

    // ===== Project completion after the required reporting is finished =====

    /** A quarterly draft that carries the Project Workplan needed to submit it. */
    private QuarterlyProgressReport workplanDraft(String period) {
        QuarterlyProgressReport draft = report("QUARTERLY_PROGRESS", period, "DRAFT");
        draft.setWorkplanFilePath("uploads/reports/workplan.pdf");
        return draft;
    }

    private QuarterlyProgressReport submittedQuarter(String period) {
        return report("QUARTERLY_PROGRESS", period, "SUBMITTED");
    }

    private QuarterlyProgressReport terminalDraft() {
        QuarterlyProgressReport draft = report(QuarterlyProgressReportService.TYPE_TERMINAL,
                "TERMINAL", "DRAFT");
        draft.setTerminalReportFilePath("uploads/reports/terminal.pdf");
        return draft;
    }

    private Proposal storedProposal() {
        return proposalRepository.findById(PROPOSAL_ID).orElseThrow();
    }

    /**
     * Scenario 1: no reporting stage is complete. Saving a draft (and submitting no
     * report at all) must leave the project at APPROVED - "Implementation Approved".
     */
    @Test
    void noCompletedReportLeavesTheProjectApproved() {
        stubReports(workplanDraft("Q1"));

        service.create(request("QUARTERLY_PROGRESS", "Q1", "DRAFT"));

        assertEquals("APPROVED", storedProposal().getStatus());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    /**
     * Scenario 2: some stages are complete while others are unfinished. Submitting Q2
     * while Q3 and the Terminal Report are still outstanding keeps APPROVED.
     */
    @Test
    void someCompletedReportsLeaveTheProjectApproved() {
        stubReports(submittedQuarter("Q1"), workplanDraft("Q2"));

        service.create(request("QUARTERLY_PROGRESS", "Q2", "SUBMITTED"));

        assertEquals("APPROVED", storedProposal().getStatus());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    /**
     * Scenario 3: all three quarterly reports are submitted but the Terminal Report is
     * still outstanding. The project must remain APPROVED.
     */
    @Test
    void allQuarterlyReportsWithoutTheTerminalReportLeaveTheProjectApproved() {
        stubReports(submittedQuarter("Q1"), submittedQuarter("Q2"), workplanDraft("Q3"));

        service.create(request("QUARTERLY_PROGRESS", "Q3", "SUBMITTED"));

        assertEquals("APPROVED", storedProposal().getStatus());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    /** An unfinished Terminal Report (a saved draft) is not a final report either. */
    @Test
    void aSavedTerminalDraftDoesNotCompleteTheProject() {
        stubReports(submittedQuarter("Q1"), submittedQuarter("Q2"),
                submittedQuarter("Q3"), terminalDraft());

        service.create(request(QuarterlyProgressReportService.TYPE_TERMINAL, "TERMINAL", "DRAFT"));

        assertEquals("APPROVED", storedProposal().getStatus());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    /**
     * Scenario 4: every required stage - Q1, Q2, Q3 and the Terminal Report - is
     * submitted. The project is persisted as COMPLETED.
     */
    @Test
    void theFinalTerminalReportMovesAnApprovedProjectToCompleted() {
        stubReports(submittedQuarter("Q1"), submittedQuarter("Q2"),
                submittedQuarter("Q3"), terminalDraft());

        QuarterlyProgressReport saved = service.create(
                request(QuarterlyProgressReportService.TYPE_TERMINAL, "TERMINAL", "SUBMITTED"));

        assertEquals("SUBMITTED", saved.getStatus());
        assertEquals("COMPLETED", storedProposal().getStatus());
        verify(proposalRepository).save(any(Proposal.class));

        // The transition is audited alongside the status change.
        assertEquals(1, storedProposal().getStatusHistory().size());
        assertEquals("APPROVED", storedProposal().getStatusHistory().get(0).getFromStatus());
        assertEquals("COMPLETED", storedProposal().getStatusHistory().get(0).getToStatus());
    }

    /** Submitting the final draft through the update path completes the project too. */
    @Test
    void submittingTheFinalTerminalDraftThroughUpdateAlsoCompletesTheProject() {
        QuarterlyProgressReport terminal = terminalDraft();
        terminal.setId(77L);
        stubReports(submittedQuarter("Q1"), submittedQuarter("Q2"),
                submittedQuarter("Q3"), terminal);
        when(reportRepository.findById(77L)).thenReturn(Optional.of(terminal));

        service.update(77L,
                request(QuarterlyProgressReportService.TYPE_TERMINAL, "TERMINAL", "SUBMITTED"));

        assertEquals("COMPLETED", storedProposal().getStatus());
    }

    /**
     * A repeated submission path on an already-completed project must not append a
     * second COMPLETED row: the transition is audited once, when it actually happens.
     */
    @Test
    void resubmittingAfterCompletionAddsNoDuplicateHistoryRow() {
        stubReports(submittedQuarter("Q1"), submittedQuarter("Q2"),
                submittedQuarter("Q3"), terminalDraft());
        service.create(request(QuarterlyProgressReportService.TYPE_TERMINAL, "TERMINAL", "SUBMITTED"));
        assertEquals(1, storedProposal().getStatusHistory().size());

        // A leftover draft for the same final stage is submitted after completion.
        QuarterlyProgressReport leftover = terminalDraft();
        leftover.setId(88L);
        when(reportRepository.findById(88L)).thenReturn(Optional.of(leftover));
        service.update(88L,
                request(QuarterlyProgressReportService.TYPE_TERMINAL, "TERMINAL", "SUBMITTED"));

        assertEquals("COMPLETED", storedProposal().getStatus());
        assertEquals(1, storedProposal().getStatusHistory().size(),
                "the completion transition is recorded exactly once");
    }

    /**
     * Scenario 5/6: a completed project keeps its stage resolvable and its reports
     * listed, while the creation rule still refuses a completed project.
     */
    @Test
    void aCompletedProjectStaysReportableButCannotStartAnotherStage() {
        Proposal completed = storedProposal();
        completed.setStatus("COMPLETED");
        assertFalse(service.isEligible(completed), "no new report may be created");
        assertTrue(service.isReportable(completed), "its reports stay accessible");

        stubReports(submittedQuarter("Q1"), submittedQuarter("Q2"),
                submittedQuarter("Q3"), report(QuarterlyProgressReportService.TYPE_TERMINAL,
                        "TERMINAL", "SUBMITTED"));
        assertTrue(service.getNextStage(PROPOSAL_ID).isAllStagesComplete());
    }

    @Test
    void completedProjectsStayInTheReportingList() {
        Proposal completed = storedProposal();
        completed.setStatus("COMPLETED");
        when(proposalRepository.findEligibleForQuarterlyReport(
                QuarterlyProgressReportService.REPORTABLE_STATUSES))
                .thenReturn(List.of(completed));

        assertEquals(List.of(completed), service.getEligibleProjects());
    }

    private WorkplanData workplan(String objective) {
        WorkplanData wp = new WorkplanData();
        wp.setAsOf("2026-03-31");
        wp.setProjectTitle("Test Project");
        wp.setProjectLeader("Juan Dela Cruz");
        wp.setProjectDuration("12");
        wp.setObjectives(List.of(new WorkplanObjective(objective, List.of("100", ""), "999")));
        wp.setRemarks("On track.");
        return wp;
    }

    // ===== "Create a Copy" traceability =====

    /** A submitted Q1 report that a copy may be started from. */
    private QuarterlyProgressReport submittedQuarterOne() {
        QuarterlyProgressReport q1 = report("QUARTERLY_PROGRESS", "Q1", "SUBMITTED");
        q1.setId(11L);
        q1.setProjectTitle("Q1 title");
        when(reportRepository.findById(11L)).thenReturn(Optional.of(q1));
        stubReports(q1);
        return q1;
    }

    @Test
    void createStoresTheReportACopyWasStartedFrom() {
        submittedQuarterOne();
        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q2", "DRAFT");
        req.setCopiedFromReportId(11L);

        QuarterlyProgressReport saved = service.create(req);

        assertEquals("Q2", saved.getPeriod(), "the copy targets the next required stage");
        assertEquals("DRAFT", saved.getStatus());
        assertEquals(11L, saved.getCopiedFromReportId());
    }

    @Test
    void aReportWrittenFromScratchHasNoCopyReference() {
        stubReports();
        QuarterlyProgressReport saved = service.create(request("QUARTERLY_PROGRESS", "Q1", "DRAFT"));
        assertNull(saved.getCopiedFromReportId());
    }

    @Test
    void aCopyReferenceToAnotherProjectsReportIsIgnored() {
        QuarterlyProgressReport foreign = submittedQuarterOne();
        foreign.setProposalId(999L);

        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q2", "DRAFT");
        req.setCopiedFromReportId(11L);

        assertNull(service.create(req).getCopiedFromReportId());
    }

    @Test
    void anUnknownCopyReferenceIsIgnored() {
        submittedQuarterOne();
        when(reportRepository.findById(404L)).thenReturn(Optional.empty());

        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q2", "DRAFT");
        req.setCopiedFromReportId(404L);

        assertNull(service.create(req).getCopiedFromReportId());
    }

    @Test
    void copyingCreatesANewRecordAndNeverTouchesTheSourceReport() {
        QuarterlyProgressReport source = submittedQuarterOne();

        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q2", "DRAFT");
        req.setCopiedFromReportId(11L);
        req.setProjectTitle("Q2 title");
        service.create(req);

        // The source keeps its own status, period and content.
        assertEquals("SUBMITTED", source.getStatus());
        assertEquals("Q1", source.getPeriod());
        assertEquals("Q1 title", source.getProjectTitle());
        // Only the new report is saved.
        verify(reportRepository, times(1)).save(any(QuarterlyProgressReport.class));
    }

    @Test
    void anExistingDraftIsResumedAndKeepsItsCopyReference() {
        QuarterlyProgressReport q1 = submittedQuarterOne();
        QuarterlyProgressReport draft = report("QUARTERLY_PROGRESS", "Q2", "DRAFT");
        draft.setId(20L);
        draft.setCopiedFromReportId(11L);
        // Q1 is submitted, so the next required stage is Q2 - where a draft already exists.
        stubReports(q1, draft);
        when(reportRepository.findById(20L)).thenReturn(Optional.of(draft));

        QuarterlyProgressReportRequest req = request("QUARTERLY_PROGRESS", "Q2", "DRAFT");
        req.setCopiedFromReportId(11L);
        QuarterlyProgressReport saved = service.create(req);

        assertEquals(20L, saved.getId(), "the existing draft is reused, never duplicated");
        assertEquals(11L, saved.getCopiedFromReportId(), "its recorded source report stays");
    }
}

