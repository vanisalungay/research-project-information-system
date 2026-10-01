package com.rpis.backend.service;

import com.rpis.backend.dto.QuarterlyProgressReportRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.QuarterlyProgressReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuarterlyProgressReportService {

    private static final List<String> IMPLEMENTATION_STATUSES = List.of("APPROVED", "RELEASED");

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

    @Transactional
    public QuarterlyProgressReport create(QuarterlyProgressReportRequest request) {
        Proposal proposal = getEligibleProposal(request.getProposalId());
        QuarterlyProgressReport report = new QuarterlyProgressReport();
        report.setProposalId(proposal.getId());
        report.setCreatedBy(proponentId(proposal));
        applyRequest(report, request);
        if ("SUBMITTED".equalsIgnoreCase(request.getStatus())) {
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
        applyRequest(report, request);
        if ("SUBMITTED".equalsIgnoreCase(request.getStatus())) {
            submit(report, proposal);
        }
        return reportRepository.save(report);
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
        report.setPeriod(request.getPeriod());
        report.setYear(request.getYear());
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
}
