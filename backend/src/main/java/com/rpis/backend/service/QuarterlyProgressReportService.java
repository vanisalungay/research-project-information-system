package com.rpis.backend.service;

import com.rpis.backend.dto.NextReportStageResponse;
import com.rpis.backend.dto.QuarterlyProgressReportRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalStatusHistory;
import com.rpis.backend.model.QuarterlyObjective;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.model.TerminalReportData;
import com.rpis.backend.model.TerminalReportSection;
import com.rpis.backend.model.WorkplanData;
import com.rpis.backend.model.WorkplanObjective;
import com.rpis.backend.repository.OvcafValidationRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.QuarterlyProgressReportRepository;
import com.rpis.backend.storage.UploadStorage;
import com.rpis.backend.util.PercentageValue;
import com.rpis.backend.util.ProposalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuarterlyProgressReportService {

    /**
     * The proposal statuses that mean "approved for implementation". IMPLEMENTATION is
     * the implementation-phase status the rest of the application already treats as
     * approved (see {@code proposalStatus.js} / {@code proposalSummary.js}); it is kept
     * here so a project carrying it is not silently denied reporting.
     */
    public static final List<String> IMPLEMENTATION_STATUSES =
            List.of("APPROVED", "RELEASED", "IMPLEMENTATION");

    /**
     * The proposal status that means the project's required reporting is finished. It is
     * written by this service only, when the last required reporting stage (the Terminal
     * Report) is submitted, and it is never set by creating or saving a draft.
     */
    public static final String PROPOSAL_COMPLETED = "COMPLETED";

    /**
     * Statuses whose reports stay readable and whose reporting stage may still be
     * resolved. It adds {@link #PROPOSAL_COMPLETED} to {@link #IMPLEMENTATION_STATUSES}
     * so a project does not lose access to the reports it already filed after its final
     * report moves it to COMPLETED. Creating a new report still requires an
     * {@link #IMPLEMENTATION_STATUSES} project, so a completed project can never start
     * another reporting stage.
     */
    public static final List<String> REPORTABLE_STATUSES =
            List.of("APPROVED", "RELEASED", "IMPLEMENTATION", PROPOSAL_COMPLETED);

    /** Twelve month cells per Project Workplan row (January-December). */
    private static final int WORKPLAN_MONTHS = 12;

    /** Reporting stage 1-3: Quarterly Progress Report + Project Workplan. */
    public static final String TYPE_QUARTERLY = "QUARTERLY_PROGRESS";
    /** Reporting stage 4: Terminal Report only. */
    public static final String TYPE_TERMINAL = "TERMINAL_REPORT";
    /** Ordered quarterly reporting stages (the first three required stages). */
    private static final List<String> QUARTERLY_PERIODS = List.of("Q1", "Q2", "Q3");
    /** Period marker used for the terminal reporting stage. */
    private static final String TERMINAL_PERIOD = "TERMINAL";
    /** One-page limit of the Terminal Report abstract (official ORPS format). */
    public static final int MAX_ABSTRACT_WORDS = 350;

    /** Document type keys accepted by the download endpoint. */
    public static final String DOC_WORKPLAN = "workplan";
    public static final String DOC_TERMINAL = "terminal";

    /** Existing file storage convention for project reports. */
    /** The folder reporting-stage documents are stored in, inside the storage root. */
    private static final String REPORTS_FOLDER = "reports";

    private final QuarterlyProgressReportRepository reportRepository;
    private final ProposalRepository proposalRepository;
    private final UploadStorage uploadStorage;
    private final OvcafValidationRepository ovcafValidationRepository;

    public List<QuarterlyProgressReport> getReportsByProposal(Long proposalId) {
        return reportRepository.findByProposalIdOrderByCreatedAtDesc(proposalId);
    }

    public QuarterlyProgressReport getReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quarterly progress report not found: " + id));
    }

    public List<Proposal> getEligibleProjects() {
        // The repository query only narrows by status and SO number; the authoritative
        // rule (including the recorded OC approval and the OVCAF decision where budget
        // evaluation was required) is applied here so the list cannot disagree with
        // the per-proposal check.
        // Completed projects stay on the list so their filed reports remain viewable; the
        // per-proposal creation check still uses IMPLEMENTATION_STATUSES, so a completed
        // project cannot start another stage.
        return proposalRepository.findEligibleForQuarterlyReport(REPORTABLE_STATUSES).stream()
                .filter(this::isReportable)
                .toList();
    }

    /**
     * Next required reporting stage for a proposal, derived from that proposal's
     * own submitted reports (never a global count and never affected by another
     * proposal's records). Only SUBMITTED reports count as completed stages, so
     * drafts, failed uploads and retries never advance the sequence.
     */
    public NextReportStageResponse getNextStage(Long proposalId) {
        // A completed project still resolves its stage (as "all stages complete") so the
        // reporting page keeps showing the reports it filed.
        Proposal proposal = getReportableProposal(proposalId);
        return computeNextStage(proposal.getId());
    }

    private NextReportStageResponse computeNextStage(Long proposalId) {
        List<QuarterlyProgressReport> reports = reportRepository.findByProposalIdOrderByCreatedAtAsc(proposalId);
        NextReportStageResponse response = new NextReportStageResponse();
        response.setProposalId(proposalId);

        for (String period : QUARTERLY_PERIODS) {
            if (!hasSubmitted(reports, TYPE_QUARTERLY, period)) {
                response.setReportType(TYPE_QUARTERLY);
                response.setPeriod(period);
                response.setRequiresWorkplan(true);
                response.setRequiresTerminalReport(false);
                response.setDraftReportId(findDraftId(reports, TYPE_QUARTERLY, period));
                return response;
            }
        }

        if (!hasSubmitted(reports, TYPE_TERMINAL, TERMINAL_PERIOD)) {
            response.setReportType(TYPE_TERMINAL);
            response.setPeriod(TERMINAL_PERIOD);
            response.setRequiresWorkplan(false);
            response.setRequiresTerminalReport(true);
            response.setDraftReportId(findDraftId(reports, TYPE_TERMINAL, TERMINAL_PERIOD));
            return response;
        }

        response.setAllStagesComplete(true);
        return response;
    }

    @Transactional
    public QuarterlyProgressReport create(QuarterlyProgressReportRequest request) {
        Proposal proposal = getEligibleProposal(request.getProposalId());
        NextReportStageResponse stage = computeNextStage(proposal.getId());
        if (stage.isAllStagesComplete()) {
            throw new IllegalStateException(
                    "All required reporting stages for this project have already been submitted.");
        }

        // Validate the requested stage against the required next stage (the
        // frontend is never trusted as the only enforcement point).
        String requestedType = normalizeType(request.getReportType());
        if (requestedType != null && !requestedType.equals(stage.getReportType())) {
            throw new IllegalArgumentException("Invalid report type. The next required report is "
                    + stage.getReportType() + " (" + stage.getPeriod() + ").");
        }
        if (request.getPeriod() != null && !request.getPeriod().isBlank()
                && !request.getPeriod().trim().equalsIgnoreCase(stage.getPeriod())) {
            throw new IllegalArgumentException(
                    "Invalid reporting stage. The next required stage is " + stage.getPeriod() + ".");
        }

        String type = stage.getReportType();
        String period = stage.getPeriod();

        List<QuarterlyProgressReport> reports = reportRepository.findByProposalIdOrderByCreatedAtAsc(proposal.getId());
        QuarterlyProgressReport report = findReport(reports, type, period);
        if (report != null) {
            // Duplicate/retry guard: never create a second record for the same
            // proposal + stage. An existing submitted report cannot be resubmitted.
            if ("SUBMITTED".equals(report.getStatus())) {
                throw new IllegalStateException(
                        "The " + stageLabel(type) + " for " + period + " has already been submitted.");
            }
            // Resume the existing draft instead of creating a duplicate row.
        } else {
            report = new QuarterlyProgressReport();
            report.setProposalId(proposal.getId());
            report.setCreatedBy(proponentId(proposal));
            // "Create a Copy" traceability: which report this one was started from. Only a
            // report of this same project is ever stored; anything else is ignored.
            report.setCopiedFromReportId(
                    resolveCopySource(proposal.getId(), request.getCopiedFromReportId()));
        }

        report.setReportType(type);
        report.setPeriod(period);
        applyRequest(report, request);
        // A new report (and a legacy draft missing them) takes the identification
        // fields from the saved proposal, never from the request body.
        applyProposalProjectInfo(report, proposal);

        if ("SUBMITTED".equalsIgnoreCase(request.getStatus())) {
            validateRequiredDocuments(report);
            return submit(report, proposal);
        }
        report.setStatus("DRAFT");
        return reportRepository.save(report);
    }

    @Transactional
    public QuarterlyProgressReport update(Long id, QuarterlyProgressReportRequest request) {
        QuarterlyProgressReport report = getReport(id);
        if ("SUBMITTED".equals(report.getStatus())) {
            throw new IllegalStateException("A submitted quarterly progress report can no longer be edited.");
        }
        // Only an unfinished draft can be reached here (a submitted report is rejected
        // above). A completed project resolves through the reportable rule so a legacy
        // draft on an already-completed project can still be saved or submitted.
        Proposal proposal = getReportableProposal(report.getProposalId());
        // The reporting stage of an existing report is immutable; only its content
        // and (for a submission) the required document are validated here.
        applyRequest(report, request);
        // Preserve the snapshot the report already recorded; only a legacy blank field
        // is filled from the project.
        applyProposalProjectInfo(report, proposal);
        if ("SUBMITTED".equalsIgnoreCase(request.getStatus())) {
            validateRequiredDocuments(report);
            return submit(report, proposal);
        }
        return reportRepository.save(report);
    }


    /**
     * Attaches the uploaded Project Workplan (stages 1-3) and/or Terminal Report
     * (final stage) to a draft report. Submitted reports are immutable.
     */
    @Transactional
    public QuarterlyProgressReport attachDocuments(Long id, MultipartFile workplan, MultipartFile terminalReport) {
        QuarterlyProgressReport report = getReport(id);
        if ("SUBMITTED".equals(report.getStatus())) {
            throw new IllegalStateException("A submitted report can no longer be modified.");
        }
        try {
            if (workplan != null && !workplan.isEmpty()) {
                SavedFile saved = storeFile(workplan, "workplan");
                report.setWorkplanFileName(saved.originalName());
                report.setWorkplanFilePath(saved.path());
            }
            if (terminalReport != null && !terminalReport.isEmpty()) {
                SavedFile saved = storeFile(terminalReport, "terminal-report");
                report.setTerminalReportFileName(saved.originalName());
                report.setTerminalReportFilePath(saved.path());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store the uploaded document: " + e.getMessage());
        }
        return reportRepository.save(report);
    }

    /**
     * Loads an attached document (workplan or terminal report) for authorized
     * download. Authorization is enforced by the security layer on the
     * {@code /api/quarterly-reports/**} GET rule.
     */
    public DocumentDownload loadDocument(Long id, String docType) {
        QuarterlyProgressReport report = getReport(id);
        String path;
        String fileName;
        if (DOC_WORKPLAN.equalsIgnoreCase(docType)) {
            path = report.getWorkplanFilePath();
            fileName = report.getWorkplanFileName();
        } else if (DOC_TERMINAL.equalsIgnoreCase(docType)) {
            path = report.getTerminalReportFilePath();
            fileName = report.getTerminalReportFileName();
        } else {
            throw new IllegalArgumentException("Unknown document type: " + docType);
        }
        if (isBlank(path)) {
            throw new IllegalArgumentException("No " + docType + " document is attached to this report.");
        }
        UploadStorage.StoredUpload stored = uploadStorage.open(path)
                .orElseThrow(() -> new IllegalArgumentException("The stored document could not be read."));
        String downloadName = !isBlank(fileName) ? fileName : stored.fileName();
        return new DocumentDownload(stored, downloadName);
    }

    private void validateRequiredDocuments(QuarterlyProgressReport report) {
        if (TYPE_TERMINAL.equals(typeOf(report))) {
            // The Terminal Report is completed in RPIS as a structured document. Reports
            // created before the in-system form existed may still hold a legacy uploaded
            // file; that stays a valid compatibility path so historical drafts can still
            // submit.
            boolean hasStructuredReport = hasTerminalReportContent(report.getTerminalReport());
            boolean hasLegacyTerminalFile = !isBlank(report.getTerminalReportFilePath());
            if (!hasStructuredReport && !hasLegacyTerminalFile) {
                throw new IllegalStateException(
                        "The Terminal Report content is required before this report can be submitted. "
                                + "Please complete the Terminal Report form (report title, abstract, "
                                + "introduction and results and discussion).");
            }
            if (hasStructuredReport) {
                validateTerminalReportContent(report.getTerminalReport());
            }
        } else {
            // Stages 1-3 require a Project Workplan, which is now an interactive
            // form saved with the report (workplan_json). Uploading a workplan
            // file is NOT required. Reports created before the interactive form
            // existed may still hold a legacy uploaded workplan file; that stays
            // a valid compatibility path so historical drafts can still submit.
            boolean hasStructuredWorkplan = hasWorkplanContent(report.getWorkplan());
            boolean hasLegacyWorkplanFile = !isBlank(report.getWorkplanFilePath());
            if (!hasStructuredWorkplan && !hasLegacyWorkplanFile) {
                throw new IllegalStateException(
                        "The Project Workplan is required before this report can be submitted. "
                                + "Please complete the Project Workplan form (at least one objective).");
            }
        }
    }

    /** True when the structured workplan has at least one described objective. */
    private boolean hasWorkplanContent(WorkplanData workplan) {
        if (workplan == null || workplan.getObjectives() == null) {
            return false;
        }
        return workplan.getObjectives().stream()
                .anyMatch(o -> o != null && o.getObjective() != null && !o.getObjective().isBlank());
    }

    /**
     * True when the structured Terminal Report carries the core content the official format
     * requires: the report title, the abstract, the introduction and the results and
     * discussion.
     *
     * <p>The remaining sections (review of related literature, methodology, conclusions and
     * recommendations, literature cited, appendix, problems, output, utilization, the
     * preliminary lists) are optional, exactly as in the printed format.</p>
     */
    public boolean hasTerminalReportContent(TerminalReportData report) {
        return report != null
                && !isBlank(report.getProjectTitle())
                && !isBlank(report.getAbstractText())
                && hasSectionText(report.getIntroduction())
                && hasSectionText(report.getResultsAndDiscussion());
    }

    /**
     * True when a body section holds text, either in the section itself or in one of its
     * subsections (the official format writes most of the body as numbered subsections).
     */
    private boolean hasSectionText(TerminalReportSection section) {
        return section != null && section.hasText();
    }

    /**
     * Content rules of the structured Terminal Report, enforced when it is submitted:
     * the core sections must be present and the abstract may not exceed the one-page
     * limit of the official format. Drafts are not blocked by these rules - a draft may
     * be saved while it is still incomplete.
     */
    private void validateTerminalReportContent(TerminalReportData report) {
        int words = wordCount(report.getAbstractText());
        if (words > MAX_ABSTRACT_WORDS) {
            throw new IllegalStateException("The abstract must not exceed " + MAX_ABSTRACT_WORDS
                    + " words (currently " + words + " words). Please shorten it before submitting.");
        }
    }

    /** Words in a narrative field: whitespace-separated tokens, ignoring extra spacing. */
    private static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /**
     * The single submission routine. Every path that submits a report - creating a
     * stage straight from the form, or saving an existing draft as submitted - goes
     * through here, because submission itself is the event that can finish reporting.
     *
     * <p>The report is persisted as SUBMITTED first and the completion transition runs
     * afterwards, so the completion check reads this report from the project's stored
     * stages. Both happen inside the caller's transaction: either the report and the
     * project status are committed together, or neither is.</p>
     */
    private QuarterlyProgressReport submit(QuarterlyProgressReport report, Proposal proposal) {
        report.setStatus("SUBMITTED");
        report.setSubmittedAt(LocalDateTime.now());
        report.setSubmittedBy(proponentId(proposal));
        QuarterlyProgressReport saved = reportRepository.save(report);
        completeProjectWhenReportingFinished(proposal, saved);
        // The submission response carries the proposal status that resulted from this
        // request, so the client can show a project that just became COMPLETED without
        // waiting for a separate read.
        saved.setProposalStatus(proposal.getStatus());
        return saved;
    }

    /**
     * The project completion transition. It runs only after a report was actually stored
     * as SUBMITTED and reads the project's own stored reporting stages: when Q1, Q2, Q3
     * and the Terminal Report are all submitted, an implementation-phase project
     * ({@link #IMPLEMENTATION_STATUSES}) becomes {@link #PROPOSAL_COMPLETED}.
     *
     * <p>A draft never completes the project, and a project still missing any required
     * stage keeps its current status - in particular APPROVED, which is displayed as
     * "Implementation Approved". A project in any other status is never overwritten.</p>
     */
    private void completeProjectWhenReportingFinished(Proposal proposal,
            QuarterlyProgressReport savedReport) {
        if (proposal == null || !"SUBMITTED".equals(savedReport.getStatus())) {
            return;
        }
        String status = proposal.getStatus() == null ? "" : proposal.getStatus().trim().toUpperCase();
        if (!IMPLEMENTATION_STATUSES.contains(status)) {
            return;
        }
        if (!computeNextStage(proposal.getId()).isAllStagesComplete()) {
            return;
        }
        recordCompletion(proposal, status);
        proposal.setStatus(PROPOSAL_COMPLETED);
        proposalRepository.save(proposal);
    }

    /**
     * Records the completion transition in the same transaction as the status change,
     * as every application status change is required to do. The actor is the Proponent
     * whose final report triggered it.
     */
    private void recordCompletion(Proposal proposal, String fromStatus) {
        ProposalStatusHistory entry = new ProposalStatusHistory();
        entry.setProposal(proposal);
        // Recorded values must satisfy the same proposal-status lookup as proposals.status.
        entry.setFromStatus(ProposalStatus.requireSupportedStatus(fromStatus));
        entry.setToStatus(ProposalStatus.requireSupportedStatus(PROPOSAL_COMPLETED));
        if (proposal.getProponent() != null) {
            entry.setActorUserId(proposal.getProponent().getId());
            entry.setActorRole(proposal.getProponent().getRole());
            entry.setActorName(proposal.getProponent().getName());
        }
        entry.setRemarks("All required reporting stages were submitted: Q1, Q2, Q3 and "
                + "the Terminal Report.");
        entry.setSource(ProposalStatusHistory.SOURCE_APPLICATION);
        entry.setChangedAt(LocalDateTime.now());
        proposal.getStatusHistory().add(entry);
    }

    private Long proponentId(Proposal proposal) {
        return proposal.getProponent() != null ? proposal.getProponent().getId() : null;
    }

    /**
     * The report a brand-new draft was copied from, for traceability. Only a report that
     * exists and belongs to the same project is stored - a missing id or another project's
     * report is ignored rather than failing the proponent's save, because the reference is
     * informational and never affects the copied content.
     */
    private Long resolveCopySource(Long proposalId, Long copiedFromReportId) {
        if (copiedFromReportId == null) {
            return null;
        }
        return reportRepository.findById(copiedFromReportId)
                .filter(source -> proposalId.equals(source.getProposalId()))
                .map(QuarterlyProgressReport::getId)
                .orElse(null);
    }

    private Proposal getEligibleProposal(Long proposalId) {
        return requireProposal(proposalId, false);
    }

    /** The same project, with COMPLETED also accepted so its reports stay accessible. */
    private Proposal getReportableProposal(Long proposalId) {
        return requireProposal(proposalId, true);
    }

    private Proposal requireProposal(Long proposalId, boolean allowCompleted) {
        if (proposalId == null) {
            throw new IllegalArgumentException("Proposal id is required.");
        }
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        boolean permitted = allowCompleted ? isReportable(proposal) : isEligible(proposal);
        if (!permitted) {
            throw new IllegalStateException(
                    "This project has not completed the full approval chain: it must be approved "
                            + "for implementation with a recorded Office of the Chancellor Special "
                            + "Order (SO) and, where budget evaluation was required, a completed "
                            + "OVCAF budget approval. Quarterly progress reports are only available "
                            + "to such projects.");
        }
        return proposal;
    }

    /**
     * The authoritative quarterly-reporting eligibility rule. Every part of the
     * approval chain must be evidenced by a persisted record:
     * <ol>
     *   <li>the project status is approved for implementation;</li>
     *   <li>the Office of the Chancellor recorded the issued SO - both the SO number
     *       and the time it was recorded, which are written together by the OC
     *       final-approval transaction (a value typed straight into the column is not
     *       an OC approval record);</li>
     *   <li>when the OC routed the proposal for budget evaluation
     *       ({@code needsBudget = true}), OVCAF recorded an approved, endorsed
     *       decision. The status "APPROVED" alone is never treated as that record.</li>
     * </ol>
     * A null status/flag is handled conservatively: null {@code needsBudget} means the
     * historic records that never used the routing flag, which are not retroactively
     * required to carry an OVCAF record.
     */
    public boolean isEligible(Proposal proposal) {
        return isInApprovalChain(proposal, IMPLEMENTATION_STATUSES);
    }

    /**
     * True when the project's reports may still be read and its reporting stage resolved:
     * the same approval-chain evidence as {@link #isEligible(Proposal)}, with a completed
     * project also accepted. This never grants the right to create a new report.
     */
    public boolean isReportable(Proposal proposal) {
        return isInApprovalChain(proposal, REPORTABLE_STATUSES);
    }

    private boolean isInApprovalChain(Proposal proposal, List<String> acceptedStatuses) {
        if (proposal == null) {
            return false;
        }
        // Normalise both sides: a status stored in a different case (or with
        // surrounding whitespace) still identifies the same workflow stage, and an
        // SO number of only spaces is not an issued SO.
        boolean hasSo = proposal.getSoNumber() != null && !proposal.getSoNumber().isBlank();
        boolean soRecorded = hasSo && proposal.getSoUploadedAt() != null;
        String status = proposal.getStatus() == null ? "" : proposal.getStatus().trim().toUpperCase();
        boolean approved = acceptedStatuses.contains(status);
        if (!approved || !soRecorded) {
            return false;
        }
        if (Boolean.TRUE.equals(proposal.getNeedsBudget())) {
            return proposal.getId() != null
                    && ovcafValidationRepository.existsByProposalIdAndDecisionAndBudgetEndorsedTrue(
                            proposal.getId(), "APPROVED_ENDORSED");
        }
        return true;
    }


    private void applyRequest(QuarterlyProgressReport report, QuarterlyProgressReportRequest request) {
        // year is NOT NULL; the terminal stage has no form fields, so default it
        // to the current year when it is not supplied and none is stored yet.
        if (request.getYear() != null) {
            report.setYear(request.getYear());
        } else if (report.getYear() == null) {
            report.setYear(LocalDate.now().getYear());
        }
        // The project identification fields (title, leader, gender, duration, start
        // date, end date) are NOT taken from the request: they are the saved proposal
        // snapshot applied by applyProposalProjectInfo. A client can therefore never
        // make a report describe a different project.
        report.setBaseStation(request.getBaseStation());
        report.setSitesOfImplementation(request.getSitesOfImplementation());
        report.setCatchUpPlan(request.getCatchUpPlan());
        report.setProblemsConcerns(request.getProblemsConcerns());
        report.setSuggestedSolutions(request.getSuggestedSolutions());
        report.setObjectives(request.getObjectives() != null ? request.getObjectives() : new ArrayList<>());
        report.setOutputs(request.getOutputs() != null ? request.getOutputs() : new ArrayList<>());
        // Only replace the stored workplan when the request actually carries one,
        // so unrelated updates (e.g. the terminal stage) never wipe it.
        if (request.getWorkplan() != null) {
            report.setWorkplan(request.getWorkplan());
        }
        // Same for the structured Terminal Report: an update that does not carry it
        // (for example a stage 1-3 save, or a terminal draft that only attaches a file)
        // never wipes the content the proponent already entered.
        if (request.getTerminalReport() != null) {
            report.setTerminalReport(request.getTerminalReport());
        }
        normalizePercentages(report);
    }

    /**
     * Copies the six identification fields from the saved proposal into a report that
     * does not have them yet. Existing values are never replaced, so reopening a filed
     * report keeps the snapshot it recorded while a legacy blank field is filled from
     * the project instead of staying empty.
     */
    private void applyProposalProjectInfo(QuarterlyProgressReport report, Proposal proposal) {
        if (proposal == null) {
            return;
        }
        if (isBlank(report.getProjectTitle())) {
            report.setProjectTitle(proposal.getProjectTitle());
        }
        if (isBlank(report.getProjectLeader())) {
            report.setProjectLeader(proposal.getProjectLeader());
        }
        if (isBlank(report.getLeaderGender())) {
            report.setLeaderGender(proposal.getProjectLeaderSex());
        }
        if (isBlank(report.getProjectDuration())) {
            report.setProjectDuration(proposal.getDuration());
        }
        if (report.getProjectStartDate() == null) {
            report.setProjectStartDate(proposal.getStartDate());
        }
        if (report.getProjectEndDate() == null) {
            report.setProjectEndDate(proposal.getEndDate());
        }
    }

    /**
     * Validates and normalises every percentage the report carries:
     * <ul>
     *   <li>(7) Major accomplishments - FOR THE PERIOD and CUMULATIVE (FROM START);</li>
     *   <li>Project Workplan - the twelve monthly cells of every row.</li>
     * </ul>
     * The workplan AVERAGE TOTAL is always recalculated from the stored month values,
     * so a total sent through the API can never disagree with them. An invalid value
     * rejects the whole request (the transaction rolls back) instead of being stored.
     */
    private void normalizePercentages(QuarterlyProgressReport report) {
        if (report.getObjectives() != null) {
            for (QuarterlyObjective row : report.getObjectives()) {
                if (row == null) {
                    continue;
                }
                row.setPercentagePeriod(PercentageValue.normalize(
                        row.getPercentagePeriod(), "Percentage for the period"));
                row.setPercentageCumulative(PercentageValue.normalize(
                        row.getPercentageCumulative(), "Cumulative percentage"));
            }
        }

        WorkplanData workplan = report.getWorkplan();
        if (workplan == null || workplan.getObjectives() == null) {
            return;
        }
        for (WorkplanObjective row : workplan.getObjectives()) {
            if (row == null) {
                continue;
            }
            List<String> months = new ArrayList<>();
            if (row.getMonths() != null) {
                for (String month : row.getMonths()) {
                    if (months.size() == WORKPLAN_MONTHS) {
                        break;
                    }
                    months.add(PercentageValue.normalize(month, "Monthly percentage"));
                }
            }
            while (months.size() < WORKPLAN_MONTHS) {
                months.add("");
            }
            row.setMonths(months);
            // Derived, never trusted from the request.
            row.setTotal(PercentageValue.average(months));
        }
    }

    /** Treats legacy rows (null report_type) as quarterly progress reports. */
    public static String typeOf(QuarterlyProgressReport report) {
        String type = report.getReportType();
        return (type == null || type.isBlank()) ? TYPE_QUARTERLY : type;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String normalized = type.trim().toUpperCase();
        if (!TYPE_QUARTERLY.equals(normalized) && !TYPE_TERMINAL.equals(normalized)) {
            throw new IllegalArgumentException("Unknown report type: " + type);
        }
        return normalized;
    }

    private boolean hasSubmitted(List<QuarterlyProgressReport> reports, String type, String period) {
        return reports.stream().anyMatch(r -> typeOf(r).equals(type)
                && "SUBMITTED".equals(r.getStatus())
                && r.getPeriod() != null && r.getPeriod().equalsIgnoreCase(period));
    }

    private QuarterlyProgressReport findReport(List<QuarterlyProgressReport> reports, String type, String period) {
        return reports.stream()
                .filter(r -> typeOf(r).equals(type)
                        && r.getPeriod() != null && r.getPeriod().equalsIgnoreCase(period))
                .findFirst().orElse(null);
    }

    private Long findDraftId(List<QuarterlyProgressReport> reports, String type, String period) {
        QuarterlyProgressReport report = findReport(reports, type, period);
        return (report != null && !"SUBMITTED".equals(report.getStatus())) ? report.getId() : null;
    }

    private String stageLabel(String type) {
        return TYPE_TERMINAL.equals(type) ? "Terminal Report" : "Quarterly Progress Report";
    }

    private SavedFile storeFile(MultipartFile file, String prefix) throws IOException {
        // The storage layer creates its directory on first write, generates the stored
        // name and returns the path relative to its configured root.
        String stored = uploadStorage.store(file.getBytes(), REPORTS_FOLDER, prefix + "-report");
        return new SavedFile(file.getOriginalFilename(), stored);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** A stored upload: original file name plus its path on disk. */
    private record SavedFile(String originalName, String path) {
    }

    /** A downloadable report document (bytes + suggested file name). */
    public record DocumentDownload(UploadStorage.StoredUpload stored, String fileName) {
    }
}

