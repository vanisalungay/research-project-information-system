package com.rpis.backend.concurrency;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.ProposalVersion;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalVersionRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.ProposalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real-PostgreSQL concurrency tests for proposal/revision identifier generation.
 * <p>
 * Runs against the isolated {@code rpis_test} database (see
 * {@code application-test.properties}); it never touches the live {@code rpis_db}.
 * These are true database concurrency tests — the service is invoked through its
 * Spring transaction proxy against a real PostgreSQL engine, not Mockito/AtomicLong
 * stand-ins.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProposalConcurrencyPostgresTest {

    private static final int NEW_PROPOSAL_THREADS = 24;
    private static final int REVISION_THREADS = 12;

    @Autowired
    private ProposalService proposalService;
    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProposalAnnouncementRepository announcementRepository;
    @Autowired
    private ProposalVersionRepository versionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataSource dataSource;

    private User proponentA;
    private User proponentB;

    @BeforeEach
    void setUp() throws Exception {
        // Safety guard: refuse to run against anything but the isolated test DB.
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("rpis_test"),
                    "Concurrency tests must run against the isolated rpis_test database, but connected to: " + url);
        }

        // Fresh, isolated state for every test.
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("DROP SEQUENCE IF EXISTS proposal_code_seq");
        jdbcTemplate.execute("CREATE SEQUENCE proposal_code_seq");

        proponentA = createProponent("Concurrency Proponent A", "conc.a@msunaawan.edu.ph");
        proponentB = createProponent("Concurrency Proponent B", "conc.b@msunaawan.edu.ph");

        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setName("Concurrency Test Window");
        announcement.setStartDate(LocalDate.now().minusDays(1));
        announcement.setEndDate(LocalDate.now().plusDays(1));
        announcement.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        announcementRepository.saveAndFlush(announcement);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ helpers

    private User createProponent(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole("PROPONENT");
        user.setStatus("APPROVED");
        return userRepository.saveAndFlush(user);
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_PROPONENT"))));
    }

    private ProposalRequest request(String title, String status) {
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle(title);
        request.setStatus(status);
        return request;
    }

    private Proposal createRevisionReadyProposal() {
        Proposal proposal = new Proposal();
        proposal.setProjectTitle("Revision Ready Proposal");
        proposal.setProposalCode("777");
        proposal.setDocumentId("MSUN-ORPS-DA-777-2026-REV00");
        proposal.setRevisionNumber(0);
        proposal.setStatus("RPS_RETURNED");
        proposal.setProponent(proponentA);
        return proposalRepository.saveAndFlush(proposal);
    }

    private Proposal manualProposal(String code, String documentId) {
        Proposal proposal = new Proposal();
        proposal.setProjectTitle("Manual " + code);
        proposal.setProposalCode(code);
        proposal.setDocumentId(documentId);
        proposal.setStatus("DRAFT");
        proposal.setProponent(proponentA);
        return proposal;
    }

    /**
     * Runs {@code n} tasks simultaneously (released by a single gate). Each result is
     * either the task's return value or the {@link Throwable} it raised, so callers can
     * assert on both successes and expected integrity-violation failures.
     */
    private List<Object> runConcurrently(int n, IntFunction<Object> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int index = i;
            futures.add(pool.submit(() -> {
                gate.await();
                try {
                    return task.apply(index);
                } catch (Throwable t) {
                    return t;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }
        gate.countDown();

        List<Object> results = new ArrayList<>();
        try {
            for (Future<Object> future : futures) {
                results.add(future.get(120, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        return results;
    }

    private boolean isIntegrityViolation(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof DataIntegrityViolationException) {
                return true;
            }
            if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
                return true;
            }
            if (cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ tests

    @Test
    void concurrentNewProposalsReceiveDistinctIds() throws Exception {
        List<User> submitters = List.of(proponentA, proponentB);

        List<Object> outcomes = runConcurrently(NEW_PROPOSAL_THREADS, i -> {
            authenticate(submitters.get(i % submitters.size()));
            return proposalService.saveProposal(request("Concurrent Proposal " + i, "DRAFT"), null)
                    .getProposalCode();
        });

        List<String> codes = new ArrayList<>();
        for (Object outcome : outcomes) {
            if (outcome instanceof Throwable t) {
                throw new AssertionError("A concurrent new-proposal submission failed: " + t, t);
            }
            codes.add((String) outcome);
        }

        assertEquals(NEW_PROPOSAL_THREADS, codes.size());
        assertEquals(NEW_PROPOSAL_THREADS, new HashSet<>(codes).size(), "proposal codes must all be distinct");
        assertTrue(codes.stream().allMatch(code -> code.matches("\\d{3,}")), "codes must be zero-padded numeric");

        List<Proposal> persisted = proposalRepository.findAll();
        assertEquals(NEW_PROPOSAL_THREADS, persisted.size());
        Set<String> documentIds = persisted.stream().map(Proposal::getDocumentId).collect(Collectors.toSet());
        assertEquals(NEW_PROPOSAL_THREADS, documentIds.size(), "document ids must all be distinct");
    }

    @Test
    void concurrentRevisionsNeverDuplicateVersionOrRevisionNumbers() throws Exception {
        Proposal proposal = createRevisionReadyProposal();
        Long proposalId = proposal.getId();

        List<Object> outcomes = runConcurrently(REVISION_THREADS, i -> {
            authenticate(proponentA);
            proposalService.saveProposal(request("Revision attempt " + i, "SUBMITTED"), proposalId);
            return "ok";
        });

        long successes = outcomes.stream().filter("ok"::equals).count();
        for (Object outcome : outcomes) {
            if (outcome instanceof Throwable t) {
                assertTrue(isIntegrityViolation(t),
                        "revision failures must be integrity violations, but got: " + t);
            }
        }
        assertTrue(successes >= 1, "at least one revision submission must succeed");

        Proposal reloaded = proposalRepository.findById(proposalId).orElseThrow();
        assertEquals(1, reloaded.getRevisionNumber(),
                "the revision number must advance exactly once despite concurrent submissions");

        List<ProposalVersion> versions = versionRepository.findByProposalIdOrderByVersionNumberAsc(proposalId);
        Set<Integer> distinctVersionNumbers = versions.stream()
                .map(ProposalVersion::getVersionNumber).collect(Collectors.toSet());
        assertEquals(versions.size(), distinctVersionNumbers.size(), "version numbers must be unique");

        Integer duplicates = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (SELECT version_number FROM proposal_versions "
                        + "WHERE proposal_id = ? GROUP BY version_number HAVING COUNT(*) > 1) t",
                Integer.class, proposalId);
        assertEquals(0, duplicates, "no duplicate (proposal_id, version_number) rows may exist");
    }

    @Test
    void databaseRejectsDuplicateProposalCode() {
        proposalRepository.saveAndFlush(manualProposal("900", "MSUN-ORPS-DA-900-2026-REV00"));
        Proposal duplicateCode = manualProposal("900", "MSUN-ORPS-DA-901-2026-REV00");

        assertThrows(DataIntegrityViolationException.class,
                () -> proposalRepository.saveAndFlush(duplicateCode));
    }

    @Test
    void databaseRejectsDuplicateRevisionVersionNumber() {
        Proposal proposal = proposalRepository.saveAndFlush(
                manualProposal("901", "MSUN-ORPS-DA-901-2026-REV00"));

        ProposalVersion first = new ProposalVersion();
        first.setProposal(proposal);
        first.setVersionNumber(1);
        versionRepository.saveAndFlush(first);

        ProposalVersion duplicate = new ProposalVersion();
        duplicate.setProposal(proposal);
        duplicate.setVersionNumber(1);

        assertThrows(DataIntegrityViolationException.class,
                () -> versionRepository.saveAndFlush(duplicate));
    }
}
