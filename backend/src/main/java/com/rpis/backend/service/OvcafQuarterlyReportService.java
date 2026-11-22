package com.rpis.backend.service;

import com.rpis.backend.dto.OvcafQuarterlyReportFilterOptions;
import com.rpis.backend.dto.OvcafQuarterlyReportPageResponse;
import com.rpis.backend.dto.OvcafQuarterlyReportRow;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.QuarterlyProgressReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only quarterly reporting for OVCAF.
 *
 * <p>The OVCAF remit is the projects whose budgets it processed. The eligible set is
 * therefore the quarterly-reporting projects (implementation stage, issued Special
 * Order, and an OVCAF budget endorsement when a budget was requested) further limited
 * to {@code needsBudget = true}. Only {@code SUBMITTED} reports are exposed, and there
 * is no write path here: OVCAF views reports and never edits, approves or returns them.
 */
@Service
@RequiredArgsConstructor
public class OvcafQuarterlyReportService {

    private static final String STATUS_SUBMITTED = "SUBMITTED";

    /** Reporting-stage order, used to offer the period filter in workflow order. */
    private static final List<String> PERIOD_ORDER =
            List.of("Q1", "Q2", "Q3", "Q4", "TERMINAL");

    private final QuarterlyProgressReportService reportService;
    private final QuarterlyProgressReportRepository reportRepository;
    private final ProposalRepository proposalRepository;

    @Transactional(readOnly = true)
    public OvcafQuarterlyReportPageResponse list(String search, String period, String college,
            String program, Long proposalId, String sortBy, String sortDir, int page, int size) {
        Map<Long, Proposal> eligible = eligibleProjects();
        List<QuarterlyProgressReport> reports = eligible.isEmpty()
                ? List.of()
                : reportRepository.findByProposalIdInAndStatusOrderBySubmittedAtDescIdDesc(
                        eligible.keySet(), STATUS_SUBMITTED);

        List<OvcafQuarterlyReportRow> rows = reports.stream()
                .map(report -> toRow(report, eligible.get(report.getProposalId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        OvcafQuarterlyReportFilterOptions options = filterOptions(rows);

        String keyword = trimToNull(search) == null ? null : search.trim().toLowerCase(Locale.ROOT);
        String periodFilter = trimToNull(period);
        String collegeFilter = trimToNull(college);
        String programFilter = trimToNull(program);

        List<OvcafQuarterlyReportRow> filtered = rows.stream()
                .filter(row -> periodFilter == null || periodFilter.equalsIgnoreCase(row.getPeriod()))
                .filter(row -> collegeFilter == null || matches(row.getCollege(), collegeFilter))
                .filter(row -> programFilter == null || matches(row.getProgramTitle(), programFilter))
                .filter(row -> proposalId == null || proposalId.equals(row.getProposalId()))
                .filter(row -> keyword == null || matchesSearch(row, keyword))
                .collect(Collectors.toCollection(ArrayList::new));

        boolean descending = !"asc".equalsIgnoreCase(trimToNull(sortDir) == null ? "desc" : sortDir.trim());
        filtered.sort(comparatorFor(sortBy, descending)
                .thenComparing(OvcafQuarterlyReportRow::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        int safeSize = Math.min(Math.max(size, 1), 100);
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) safeSize));
        int safePage = Math.min(Math.max(page, 1), totalPages);
        int from = Math.min((safePage - 1) * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);

        OvcafQuarterlyReportPageResponse response = new OvcafQuarterlyReportPageResponse();
        response.setItems(new ArrayList<>(filtered.subList(from, to)));
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotalItems(totalItems);
        response.setTotalPages(totalPages);
        response.setFilterOptions(options);
        return response;
    }

    /**
     * Reads an attached workplan or terminal-report document, but only for a submitted
     * report of a project OVCAF is authorized to view.
     */
    @Transactional(readOnly = true)
    public QuarterlyProgressReportService.DocumentDownload loadDocument(Long reportId, String docType) {
        QuarterlyProgressReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Quarterly report not found: " + reportId));
        if (!STATUS_SUBMITTED.equalsIgnoreCase(report.getStatus())) {
            throw new IllegalStateException("Only submitted reports are available to OVCAF.");
        }
        Proposal proposal = proposalRepository.findById(report.getProposalId())
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found for this report."));
        if (!Boolean.TRUE.equals(proposal.getNeedsBudget()) || !reportService.isReportable(proposal)) {
            throw new IllegalStateException("This report is not available to OVCAF.");
        }
        return reportService.loadDocument(reportId, docType);
    }

    /** Reporting-eligible projects whose budget OVCAF actually processed. */
    private Map<Long, Proposal> eligibleProjects() {
        Map<Long, Proposal> eligible = new LinkedHashMap<>();
        for (Proposal proposal : reportService.getEligibleProjects()) {
            if (proposal.getId() != null && Boolean.TRUE.equals(proposal.getNeedsBudget())) {
                eligible.putIfAbsent(proposal.getId(), proposal);
            }
        }
        return eligible;
    }

    private static OvcafQuarterlyReportRow toRow(QuarterlyProgressReport report, Proposal proposal) {
        if (proposal == null) {
            return null;
        }
        OvcafQuarterlyReportRow row = new OvcafQuarterlyReportRow();
        row.setId(report.getId());
        row.setProposalId(report.getProposalId());
        row.setDocumentId(proposal.getDocumentId() != null
                ? proposal.getDocumentId() : proposal.getProposalCode());
        row.setProjectTitle(proposal.getProjectTitle());
        row.setProjectLeader(proposal.getProjectLeader() != null
                ? proposal.getProjectLeader()
                : proposal.getProponent() != null ? proposal.getProponent().getName() : null);
        row.setCollege(proposal.getCollege());
        row.setProgramTitle(proposal.getProgramTitle());
        row.setProposalStatus(proposal.getStatus());
        row.setSubmittedAt(report.getSubmittedAt());
        row.setPeriod(report.getPeriod());
        row.setYear(report.getYear());
        row.setStatus(report.getStatus());
        row.setReportType(report.getReportType());
        row.setReport(report);
        return row;
    }

    private static OvcafQuarterlyReportFilterOptions filterOptions(List<OvcafQuarterlyReportRow> rows) {
        Set<String> periods = new LinkedHashSet<>();
        for (String period : PERIOD_ORDER) {
            if (rows.stream().anyMatch(row -> period.equalsIgnoreCase(row.getPeriod()))) {
                periods.add(period);
            }
        }
        // Any stored period outside the current stage order is still offered.
        rows.stream().map(OvcafQuarterlyReportRow::getPeriod).filter(Objects::nonNull)
                .filter(period -> !periods.contains(period)).distinct().sorted()
                .forEach(periods::add);

        List<String> colleges = distinctSorted(rows, OvcafQuarterlyReportRow::getCollege);
        List<String> programs = distinctSorted(rows, OvcafQuarterlyReportRow::getProgramTitle);

        List<OvcafQuarterlyReportFilterOptions.ProjectOption> projects = rows.stream()
                .collect(Collectors.toMap(
                        OvcafQuarterlyReportRow::getProposalId,
                        row -> new OvcafQuarterlyReportFilterOptions.ProjectOption(
                                row.getProposalId(), row.getDocumentId(), row.getProjectTitle()),
                        (first, second) -> first,
                        LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(
                        option -> option.getProjectTitle() == null
                                ? "" : option.getProjectTitle().toLowerCase(Locale.ROOT)))
                .toList();

        return new OvcafQuarterlyReportFilterOptions(
                new ArrayList<>(periods), colleges, programs, projects);
    }

    private static List<String> distinctSorted(List<OvcafQuarterlyReportRow> rows,
            Function<OvcafQuarterlyReportRow, String> value) {
        return rows.stream()
                .map(value)
                .map(OvcafQuarterlyReportService::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static Comparator<OvcafQuarterlyReportRow> comparatorFor(String sortBy, boolean descending) {
        String key = trimToNull(sortBy) == null ? "submittedAt" : sortBy.trim();
        return switch (key) {
            case "projectTitle" -> textComparator(OvcafQuarterlyReportRow::getProjectTitle, descending);
            case "college" -> textComparator(OvcafQuarterlyReportRow::getCollege, descending);
            case "period" -> textComparator(OvcafQuarterlyReportRow::getPeriod, descending);
            case "year" -> {
                Comparator<Integer> order =
                        descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
                yield Comparator.comparing(OvcafQuarterlyReportRow::getYear,
                        Comparator.nullsLast(order));
            }
            case "status" -> textComparator(OvcafQuarterlyReportRow::getStatus, descending);
            default -> {
                Comparator<LocalDateTime> order =
                        descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
                yield Comparator.comparing(OvcafQuarterlyReportRow::getSubmittedAt,
                        Comparator.nullsLast(order));
            }
        };
    }

    private static Comparator<OvcafQuarterlyReportRow> textComparator(
            Function<OvcafQuarterlyReportRow, String> value, boolean descending) {
        Comparator<String> order = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return Comparator.comparing(
                row -> {
                    String text = value.apply(row);
                    return text == null ? null : text.toLowerCase(Locale.ROOT);
                },
                Comparator.nullsLast(order));
    }

    private static boolean matchesSearch(OvcafQuarterlyReportRow row, String keyword) {
        return contains(row.getDocumentId(), keyword)
                || contains(row.getProjectTitle(), keyword)
                || contains(row.getProjectLeader(), keyword)
                || contains(row.getCollege(), keyword)
                || contains(row.getProgramTitle(), keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static boolean matches(String value, String expected) {
        return value != null && value.trim().equalsIgnoreCase(expected.trim());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
