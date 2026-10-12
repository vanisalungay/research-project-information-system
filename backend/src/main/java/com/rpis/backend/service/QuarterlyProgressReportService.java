package com.rpis.backend.service;

import com.rpis.backend.dto.NextReportStageResponse;
import com.rpis.backend.dto.QuarterlyProgressReportRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.model.WorkplanData;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.QuarterlyProgressReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuarterlyProgressReportService {

    private static final List<String> IMPLEMENTATION_STATUSES = List.of("APPROVED", "RELEASED");

    /** Reporting stage 1-3: Quarterly Progress Report + Project Workplan. */
    public static final String TYPE_QUARTERLY = "QUARTERLY_PROGRESS";
    /** Reporting stage 4: Terminal Report only. */
    public static final String TYPE_TERMINAL = "TERMINAL_REPORT";
    /** Ordered quarterly reporting stages (the first three required stages). */
    private static final List<String> QUARTERLY_PERIODS = List.of("Q1", "Q2", "Q3");
    /** Period marker used for the terminal reporting stage. */
    private static final String TERMINAL_PERIOD = "TERMINAL";

    /** Document type keys accepted by the download endpoint. */
    public static final String DOC_WORKPLAN = "workplan";
    public static final String DOC_TERMINAL = "terminal";

    /** Existing file storage convention for project reports. */
    private static final String UPLOAD_DIR = "uploads/reports/";

    private final QuarterlyProgressReportRepository reportRepository;
    private final ProposalRepository proposalRepository;

    public List<QuarterlyProgressReport> getReportsByProposal(Long proposalId) {
        return reportRepository.findByProposalIdOrderByCreatedAtDesc(proposalId);
    }

    public QuarterlyProgressReport getReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quarterly progress report not found: " + id));
    }

    public List<Proposal> getEligibleProjects() {
        return proposalRepository.findEligibleForQuarterlyReport(IMPLEMENTATION_STATUSES);
    }

    /**
     * Next required reporting stage for a proposal, derived from that proposal's
     * own submitted reports (never a global count and never affected by another
     * proposal's records). Only SUBMITTED reports count as completed stages, so
     * drafts, failed uploads and retries never advance the sequence.
     */
    public NextReportStageResponse getNextStage(Long proposalId) {
        Proposal proposal = getEligibleProposal(proposalId);
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
        }

        report.setReportType(type);
        report.setPeriod(period);
        applyRequest(report, request);

        if ("SUBMITTED".equalsIgnoreCase(request.getStatus())) {
            validateRequiredDocuments(report);
            submit(report, proposal);
        } else {
            report.setStatus("DRAFT");
        }
        return reportRepository.save(report);
    }

    @Transactional
    public QuarterlyProgressReport update(Long id, QuarterlyProgressReportRequest request) {
        QuarterlyProgressReport report = getReport(id);
        if ("SUBMITTED".equals(report.getStatus())) {
            throw new IllegalStateException("A submitted quarterly progress report can no longer be edited.");
        }
        Proposal proposal = getEligibleProposal(report.getProposalId());
        // The reporting stage of an existing report is immutable; only its content
        // and (for a submission) the required document are validated here.
        applyRequest(report, request);
        if ("SUBMITTED".equalsIgnoreCase(request.getStatus())) {
            validateRequiredDocuments(report);
            submit(report, proposal);
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
        Path resolved = Paths.get(path).toAbsolutePath().normalize();
        try {
            Resource resource = new UrlResource(resolved.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("The stored document could not be read.");
            }
            String downloadName = !isBlank(fileName) ? fileName : resolved.getFileName().toString();
            return new DocumentDownload(resource, downloadName);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The stored document path is invalid.");
        }
    }

    private void validateRequiredDocuments(QuarterlyProgressReport report) {
        if (TYPE_TERMINAL.equals(typeOf(report))) {
            if (isBlank(report.getTerminalReportFilePath())) {
                throw new IllegalStateException(
                        "The completed Terminal Report file is required before this report can be submitted.");
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

    private void submit(QuarterlyProgressReport report, Proposal proposal) {
        report.setStatus("SUBMITTED");
        report.setSubmittedAt(LocalDateTime.now());
        report.setSubmittedBy(proponentId(proposal));
    }

    private Long proponentId(Proposal proposal) {
        return proposal.getProponent() != null ? proposal.getProponent().getId() : null;
    }

    private Proposal getEligibleProposal(Long proposalId) {
        if (proposalId == null) {
            throw new IllegalArgumentException("Proposal id is required.");
        }
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        if (!isEligible(proposal)) {
            throw new IllegalStateException(
                    "This project is not approved for implementation or has no issued Special Order (SO). "
                            + "Quarterly progress reports are only available for approved, SO-issued projects.");
        }
        return proposal;
    }

    public boolean isEligible(Proposal proposal) {
        if (proposal == null) {
            return false;
        }
        boolean hasSo = proposal.getSoNumber() != null && !proposal.getSoNumber().isBlank();
        boolean approved = proposal.getStatus() != null && IMPLEMENTATION_STATUSES.contains(proposal.getStatus());
        return hasSo && approved;
    }


    private void applyRequest(QuarterlyProgressReport report, QuarterlyProgressReportRequest request) {
        // year is NOT NULL; the terminal stage has no form fields, so default it
        // to the current year when it is not supplied and none is stored yet.
        if (request.getYear() != null) {
            report.setYear(request.getYear());
        } else if (report.getYear() == null) {
            report.setYear(LocalDate.now().getYear());
        }
        report.setProjectTitle(request.getProjectTitle());
        report.setProjectLeader(request.getProjectLeader());
        report.setLeaderGender(request.getLeaderGender());
        report.setBaseStation(request.getBaseStation());
        report.setSitesOfImplementation(request.getSitesOfImplementation());
        report.setProjectDuration(request.getProjectDuration());
        report.setProjectStartDate(parseDate(request.getProjectStartDate()));
        report.setProjectEndDate(parseDate(request.getProjectEndDate()));
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
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String original = file.getOriginalFilename();
        String extension = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf("."))
                : "";
        String uniqueFilename = UUID.randomUUID() + "_" + prefix + extension;
        Path target = uploadPath.resolve(uniqueFilename);
        Files.write(target, file.getBytes());
        return new SavedFile(original, target.toString().replace('\\', '/'));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** A stored upload: original file name plus its path on disk. */
    private record SavedFile(String originalName, String path) {
    }

    /** A downloadable report document (bytes + suggested file name). */
    public record DocumentDownload(Resource resource, String fileName) {
    }
}

