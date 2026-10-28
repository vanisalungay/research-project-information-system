package com.rpis.backend.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.rpis.backend.dto.EndorsementDecisionRequest;
import com.rpis.backend.dto.EndorsementRequestCreateRequest;
import com.rpis.backend.dto.EndorsementRequestResponse;
import com.rpis.backend.model.Certification;
import com.rpis.backend.model.EndorsementRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.CertificationRepository;
import com.rpis.backend.repository.EndorsementLetterSettingsRepository;
import com.rpis.backend.repository.EndorsementRequestEventRepository;
import com.rpis.backend.repository.EndorsementRequestRepository;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests of the Dean endorsement workflow against real PostgreSQL.
 *
 * <p>Runs against the isolated {@code rpis_test} database only (see
 * {@code application-test.properties}); it never touches the live {@code rpis_db}.
 * The workflow is exercised through the Spring transaction proxy: a Proponent raises
 * the request, the Dean approves it (which renders and stores the official letter and
 * files it in Certifications) or rejects it, and the request is linked to the
 * proposal the Proponent continued to.
 */
@SpringBootTest
@ActiveProfiles("test")
class EndorsementWorkflowPostgresTest {

    private static final String COLLEGE = "College of Fisheries and Marine Sciences (CFMS)";
    private static final String OTHER_COLLEGE = "College of Business and Information Technology (CBIT)";

    @Autowired
    private EndorsementRequestService endorsementService;
    @Autowired
    private CertificationService certificationService;
    @Autowired
    private UserSignatureService signatureService;
    @Autowired
    private EndorsementRequestRepository requestRepository;
    @Autowired
    private EndorsementRequestEventRepository eventRepository;
    @Autowired
    private CertificationRepository certificationRepository;
    @Autowired
    private EndorsementLetterSettingsRepository settingsRepository;
    @Autowired
    private ProposalAnnouncementRepository announcementRepository;
    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User proponent;
    private User otherProponent;
    private User dean;
    private User otherDean;
    private User admin;
    private ProposalAnnouncement call;
    private ProposalAnnouncement externalCall;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE certifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE endorsement_request_events RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE endorsement_requests RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE notifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = account("Ana Proponent", "ana.proponent@msunaawan.edu.ph", "PROPONENT", null);
        otherProponent = account("Ben Proponent", "ben.proponent@msunaawan.edu.ph", "PROPONENT", null);
        dean = account("Dean Maria Santos", "dean.cfms@msunaawan.edu.ph", UserRoles.DEAN, COLLEGE);
        otherDean = account("Dean Pedro Reyes", "dean.cbit@msunaawan.edu.ph", UserRoles.DEAN, OTHER_COLLEGE);
        admin = account("RPS Administrator", "rps.admin@msunaawan.edu.ph", "RPS_ADMIN", null);

        call = announcement("Internally Funded Research Program 2026",
                ProposalAnnouncement.FUNDING_PROGRAM_IFP);
        externalCall = announcement("Externally Funded Research Program 2026", "EXTERNALLY_FUNDED");

        // The Dean has the electronic signature the official letter is signed with.
        signatureService.saveSignature(dean.getId(), signatureUpload());
        dean = userRepository.findById(dean.getId()).orElseThrow();

        // Nothing else is configured. In particular there is no ORPS Director and no letter
        // settings row: every test below therefore proves the endorsement workflow works
        // without that obsolete configuration.
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String stored : jdbcTemplate.queryForList(
                "SELECT letter_file_path FROM endorsement_requests WHERE letter_file_path IS NOT NULL",
                String.class)) {
            deleteStoredFile(stored);
        }
        for (String stored : jdbcTemplate.queryForList(
                "SELECT signature_file_path FROM endorsement_requests WHERE signature_file_path IS NOT NULL",
                String.class)) {
            deleteStoredFile(stored);
        }
        for (String stored : jdbcTemplate.queryForList(
                "SELECT signature_file_path FROM users WHERE signature_file_path IS NOT NULL",
                String.class)) {
            deleteStoredFile(stored);
        }
        settingsRepository.deleteAll();
    }

    private void deleteStoredFile(String stored) {
        try {
            Files.deleteIfExists(Paths.get("uploads").toAbsolutePath().normalize().resolve(stored));
        } catch (Exception ignored) {
            // A leftover test file is not worth failing a test for.
        }
    }

    // ==================== FIXTURES ====================

    private User account(String name, String email, String role, String college) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus("APPROVED");
        user.setAssignedCollege(college);
        user.setDesignation(UserRoles.DEAN.equals(role) ? "Dean" : null);
        return userRepository.saveAndFlush(user);
    }

    private ProposalAnnouncement announcement(String name, String fundingProgram) {
        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setName(name);
        announcement.setStartDate(LocalDate.now().minusDays(1));
        announcement.setEndDate(LocalDate.now().plusDays(7));
        announcement.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        announcement.setFundingProgram(fundingProgram);
        return announcementRepository.saveAndFlush(announcement);
    }

    private MockMultipartFile signatureUpload() {
        BufferedImage image = new BufferedImage(200, 70, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 200, 70);
        graphics.setColor(Color.BLACK);
        graphics.drawLine(10, 55, 180, 20);
        graphics.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return new MockMultipartFile("signature", "dean-signature.png",
                    MediaType.IMAGE_PNG_VALUE, out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getId(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
    }

    private EndorsementRequestCreateRequest newRequest(String title) {
        EndorsementRequestCreateRequest request = new EndorsementRequestCreateRequest();
        request.setProjectTitle(title);
        request.setProposalAnnouncementId(call.getId());
        request.setCollege(COLLEGE);
        request.setResearchSummary("A brief summary for the Dean.");
        request.setNotes("Please review before the deadline.");
        return request;
    }

    /**
     * A request whose two titles differ, so the Program Title can never be mistaken for
     * the Project Title (or the other way round) in any assertion below.
     */
    private EndorsementRequestCreateRequest newRequest(String projectTitle, String programTitle) {
        EndorsementRequestCreateRequest request = newRequest(projectTitle);
        request.setProgramTitle(programTitle);
        return request;
    }

    private EndorsementDecisionRequest approval() {
        EndorsementDecisionRequest request = new EndorsementDecisionRequest();
        request.setEndorserName("Dean Maria Santos");
        request.setEndorserDesignation("Dean");
        request.setEndorsementDate(LocalDate.of(2026, 10, 27));
        return request;
    }

    /** Creates a request as the Proponent and returns it. */
    private EndorsementRequestResponse submittedRequest(String title) {
        authenticateAs(proponent);
        return endorsementService.create(newRequest(title));
    }

    // ==================== THE PROPONENT'S REQUEST ====================

    @Test
    void aRequestStartsPendingAndIsRoutedToTheDeanOfItsCollege() {
        EndorsementRequestResponse request = submittedRequest("Mangrove Rehabilitation Study");

        assertEquals("PENDING", request.getStatus());
        assertEquals(proponent.getId(), request.getProponentId());
        assertEquals(dean.getId(), request.getAssignedDeanId());
        assertEquals(COLLEGE, request.getCollege());
        assertEquals(call.getName(), request.getProposalAnnouncementName());
        assertEquals("IFP", request.getFundingProgram());
        assertTrue(request.isLetterTemplateAvailable());
        assertTrue(request.isCanApprove());
        assertTrue(request.isCanReject());
        assertFalse(request.isHasLetter());
        assertNotNull(request.getCreatedAt());
        assertTrue(request.getReferenceNumber().startsWith("MSUN-ORPS-END-"),
                request.getReferenceNumber());

        // The creation and the routing are on the audit trail.
        assertTrue(eventRepository.findByEndorsementRequestIdOrderByCreatedAtAsc(request.getId())
                .stream()
                .anyMatch(event -> "CREATED".equals(event.getEventType())));
        assertTrue(eventRepository.findByEndorsementRequestIdOrderByCreatedAtAsc(request.getId())
                .stream()
                .anyMatch(event -> "ASSIGNED".equals(event.getEventType())));
    }

    @Test
    void theProponentSeesOnlyTheirOwnRequests() {
        EndorsementRequestResponse mine = submittedRequest("Mine");
        authenticateAs(otherProponent);
        endorsementService.create(newRequest("Theirs"));

        authenticateAs(proponent);
        List<EndorsementRequestResponse> mineOnly = endorsementService.listMine();

        assertEquals(1, mineOnly.size());
        assertEquals(mine.getId(), mineOnly.get(0).getId());
    }

    @Test
    void anotherProponentCannotReadTheRequest() {
        EndorsementRequestResponse request = submittedRequest("Private");

        authenticateAs(otherProponent);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> endorsementService.getForCurrentUser(request.getId()));
        assertEquals(403, error.getStatusCode().value());
    }

    @Test
    void anIneligibleCallForProposalIsRefused() {
        EndorsementRequestCreateRequest request = newRequest("Wrong call");
        request.setProposalAnnouncementId(999999L);

        authenticateAs(proponent);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> endorsementService.create(request));
        assertTrue(error.getMessage().contains("not open for submissions"), error.getMessage());
    }

    @Test
    void aNonProponentCannotRaiseARequest() {
        authenticateAs(dean);
        ResponseStatusException forbidden = assertThrows(ResponseStatusException.class,
                () -> endorsementService.create(newRequest("Not allowed")));
        assertEquals(403, forbidden.getStatusCode().value());
    }

    // ==================== THE DEAN'S DECISION ====================

    @Test
    void approvingIssuesTheOfficialLetterAndFilesItInCertifications() throws Exception {
        EndorsementRequestResponse request = submittedRequest("Seaweed Biostimulant Study");

        authenticateAs(dean);
        EndorsementRequestResponse approved = endorsementService.approve(request.getId(), approval());

        assertEquals("APPROVED", approved.getStatus());
        assertEquals("Dean Maria Santos", approved.getDecidedByName());
        assertEquals(dean.getId(), approved.getDecidedByUserId());
        assertEquals(LocalDate.of(2026, 10, 27), approved.getEndorsementDate());
        assertEquals(request.getReferenceNumber(), approved.getLetterDocumentCode());
        assertEquals(1, approved.getLetterVersion());
        assertTrue(approved.isHasLetter());
        assertTrue(approved.isHasSignature());
        assertNotNull(approved.getCertificationId());

        // The stored document is a real PDF and the signature was snapshotted beside it.
        EndorsementRequest stored = requestRepository.findById(request.getId()).orElseThrow();
        Path letter = Paths.get("uploads").toAbsolutePath().normalize()
                .resolve(stored.getLetterFilePath());
        assertTrue(Files.exists(letter), "the issued letter must be stored: " + letter);
        byte[] bytes = Files.readAllBytes(letter);
        assertEquals("%PDF", new String(bytes, 0, 4));
        assertTrue(Files.exists(Paths.get("uploads").toAbsolutePath().normalize()
                .resolve(stored.getSignatureFilePath())));

        // The certification entry belongs to the Proponent and points at the same file.
        assertEquals(1, certificationRepository.findAll().size());
        var certification = certificationRepository.findByEndorsementRequestId(request.getId())
                .orElseThrow();
        assertEquals(proponent.getId(), certification.getProponent().getId());
        assertEquals(stored.getLetterFilePath(), certification.getFilePath());
        assertEquals("Endorsement Letter", certificationService.toResponse(certification)
                .getDocumentTypeLabel());
        assertTrue(certificationService.toResponse(certification).isDocumentAvailable());

        // The Proponent was notified and the whole decision is on the audit trail.
        assertTrue(notificationRepository.findByUserOrderByCreatedAtDesc(proponent).stream()
                        .anyMatch(notification -> notification.getMessage() != null
                                && notification.getMessage().contains("was endorsed")),
                "the Proponent must be notified");
        var events = eventRepository.findByEndorsementRequestIdOrderByCreatedAtAsc(request.getId());
        assertTrue(events.stream().anyMatch(e -> "APPROVED".equals(e.getEventType())));
        assertTrue(events.stream().anyMatch(e -> "LETTER_GENERATED".equals(e.getEventType())));
        assertTrue(events.stream().anyMatch(e -> "CERTIFICATION_CREATED".equals(e.getEventType())));
        assertTrue(events.stream()
                .filter(e -> "APPROVED".equals(e.getEventType()))
                .allMatch(e -> dean.getId().equals(e.getActorId())));
    }

    @Test
    void theIssuedLetterIsOnlyReadableByItsOwnerAndTheReviewingRoles() {
        EndorsementRequestResponse request = submittedRequest("Access Controlled");
        authenticateAs(dean);
        endorsementService.approve(request.getId(), approval());

        // The Proponent who owns it can read it.
        authenticateAs(proponent);
        assertNotNull(endorsementService.loadLetter(request.getId()));

        // Another Proponent cannot.
        authenticateAs(otherProponent);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> endorsementService.loadLetter(request.getId()));
        assertEquals(403, error.getStatusCode().value());

        // A Dean outside the scope of the request cannot either.
        authenticateAs(otherDean);
        ResponseStatusException deanError = assertThrows(ResponseStatusException.class,
                () -> endorsementService.loadLetter(request.getId()));
        assertEquals(403, deanError.getStatusCode().value());
    }

    @Test
    void anUnrelatedDeanCannotDecideTheRequest() {
        EndorsementRequestResponse request = submittedRequest("Out of scope");

        authenticateAs(otherDean);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> endorsementService.approve(request.getId(), approval()));
        assertEquals(403, error.getStatusCode().value());

        // The request is untouched.
        assertEquals("PENDING", requestRepository.findById(request.getId()).orElseThrow().getStatus());
        assertEquals(0, certificationRepository.findAll().size());
    }

    @Test
    void rejectingStoresTheReasonAndIssuesNothing() {
        EndorsementRequestResponse request = submittedRequest("Rejected Study");

        EndorsementDecisionRequest rejection = new EndorsementDecisionRequest();
        rejection.setRejectionReason("The budget breakdown is incomplete.");
        authenticateAs(dean);
        EndorsementRequestResponse rejected = endorsementService.reject(request.getId(), rejection);

        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("The budget breakdown is incomplete.", rejected.getRejectionReason());
        assertEquals(dean.getId(), rejected.getDecidedByUserId());
        assertNotNull(rejected.getDecidedAt());
        assertFalse(rejected.isHasLetter());
        assertFalse(rejected.isCanApprove());
        assertFalse(rejected.isCanReject());

        // No letter and no certification entry exist, so the request can never be
        // treated as endorsed.
        EndorsementRequest stored = requestRepository.findById(request.getId()).orElseThrow();
        assertFalse(stored.hasIssuedLetter());
        assertNull(stored.getLetterFilePath());
        assertNull(stored.getLetterDocumentCode());
        assertEquals(0, certificationRepository.findAll().size());

        // The Proponent sees the reason.
        authenticateAs(proponent);
        assertEquals("The budget breakdown is incomplete.",
                endorsementService.getForCurrentUser(request.getId()).getRejectionReason());
    }

    @Test
    void rejectingWithoutAReasonIsRefused() {
        EndorsementRequestResponse request = submittedRequest("No reason");

        authenticateAs(dean);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> endorsementService.reject(request.getId(), new EndorsementDecisionRequest()));
        assertTrue(error.getMessage().contains("reason"), error.getMessage());
        assertEquals("PENDING", requestRepository.findById(request.getId()).orElseThrow().getStatus());
    }

    @Test
    void aSecondApprovalIsRefusedAndIssuesNoSecondLetter() {
        EndorsementRequestResponse request = submittedRequest("Double approval");

        authenticateAs(dean);
        endorsementService.approve(request.getId(), approval());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> endorsementService.approve(request.getId(), approval()));
        assertTrue(error.getMessage().contains("already"), error.getMessage());

        assertEquals(1, certificationRepository.findAll().size());
        assertEquals(1, requestRepository.findById(request.getId()).orElseThrow().getLetterVersion());
    }

    @Test
    void concurrentApprovalsIssueExactlyOneLetterAndOneCertification() throws Exception {
        EndorsementRequestResponse request = submittedRequest("Concurrent approval");

        int threads = 2;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                authenticateAs(dean);
                ready.countDown();
                go.await(15, TimeUnit.SECONDS);
                try {
                    endorsementService.approve(request.getId(), approval());
                    return Boolean.TRUE;
                } catch (RuntimeException e) {
                    return Boolean.FALSE;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        List<Future<Boolean>> results = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            results.add(pool.submit(task));
        }
        assertTrue(ready.await(15, TimeUnit.SECONDS), "the threads must be ready");
        go.countDown();

        int successes = 0;
        for (Future<Boolean> result : results) {
            if (Boolean.TRUE.equals(result.get(60, TimeUnit.SECONDS))) {
                successes++;
            }
        }
        pool.shutdown();

        assertEquals(1, successes, "exactly one concurrent approval may succeed");
        assertEquals(1, certificationRepository.findAll().size(),
                "a concurrent approval must never create a second certification");
        EndorsementRequest stored = requestRepository.findById(request.getId()).orElseThrow();
        assertEquals("APPROVED", stored.getStatus());
        assertEquals(1, stored.getLetterVersion());
        assertTrue(Files.exists(Paths.get("uploads").toAbsolutePath().normalize()
                .resolve(stored.getLetterFilePath())));
    }

    // ==================== BUSINESS GUARDS ====================

    @Test
    void aCallWithoutAnOfficialLetterCannotBeEndorsed() {
        EndorsementRequestCreateRequest request = newRequest("Externally funded study");
        request.setProposalAnnouncementId(externalCall.getId());

        authenticateAs(proponent);
        EndorsementRequestResponse created = endorsementService.create(request);
        assertFalse(created.isLetterTemplateAvailable());
        assertFalse(created.isCanApprove(), "the IFP letter must not be offered for another program");

        authenticateAs(dean);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> endorsementService.approve(created.getId(), approval()));
        assertTrue(error.getMessage().contains("No official endorsement letter"), error.getMessage());

        // Nothing was issued and the request is still pending.
        assertEquals("PENDING", requestRepository.findById(created.getId()).orElseThrow().getStatus());
        assertEquals(0, certificationRepository.findAll().size());
    }

    @Test
    void approvingWithoutASavedSignatureIsRefused() {
        EndorsementRequestResponse request = submittedRequest("No signature");

        signatureService.deleteSignature(dean.getId());
        authenticateAs(dean);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> endorsementService.approve(request.getId(), approval()));
        assertTrue(error.getMessage().contains("electronic signature"), error.getMessage());
        assertEquals("PENDING", requestRepository.findById(request.getId()).orElseThrow().getStatus());
    }

    /**
     * The regression this change fixes: an unconfigured ORPS Director used to make approval
     * impossible (409 CONFLICT) even though nothing else was wrong. The settings are now
     * wording only, so an empty settings table must not stop the Dean - the letter is
     * addressed to the office instead of to a named person.
     */
    @Test
    void anApprovalNeedsNoLetterSettingsConfiguration() {
        EndorsementRequestResponse request = submittedRequest("No letter settings configured");

        // setUp configures nothing; clearing again makes the empty state explicit so the
        // test cannot pass by accident on a database that still holds a settings row.
        settingsRepository.deleteAll();

        authenticateAs(dean);
        EndorsementRequestResponse approved = endorsementService.approve(request.getId(), approval());

        assertEquals("APPROVED", approved.getStatus());
        assertTrue(approved.isHasLetter(), "the official letter must still be issued");
        assertEquals(1, certificationRepository.findAll().size(),
                "the issued letter must still be filed in Certifications");
    }

    @Test
    void aProponentCannotReissueOrDecideTheirOwnRequest() {
        EndorsementRequestResponse request = submittedRequest("Not mine to decide");

        authenticateAs(proponent);
        assertThrows(ResponseStatusException.class,
                () -> endorsementService.approve(request.getId(), approval()));
        assertThrows(ResponseStatusException.class,
                () -> endorsementService.reject(request.getId(), new EndorsementDecisionRequest()));
        assertThrows(ResponseStatusException.class,
                () -> endorsementService.reissue(request.getId(), approval()));
    }

    @Test
    void anAdministratorCanReissueAnIssuedLetterAsANewVersion() throws Exception {
        EndorsementRequestResponse request = submittedRequest("Reissue me");
        authenticateAs(dean);
        endorsementService.approve(request.getId(), approval());
        String firstPath = requestRepository.findById(request.getId()).orElseThrow()
                .getLetterFilePath();

        authenticateAs(admin);
        EndorsementRequestResponse reissued = endorsementService.reissue(request.getId(), approval());

        assertEquals(2, reissued.getLetterVersion());
        EndorsementRequest stored = requestRepository.findById(request.getId()).orElseThrow();
        assertNotEquals(firstPath, stored.getLetterFilePath(),
                "a reissue must write a new file instead of overwriting the issued one");
        assertTrue(Files.exists(Paths.get("uploads").toAbsolutePath().normalize()
                .resolve(firstPath)), "the superseded letter must be preserved");
        assertTrue(Files.exists(Paths.get("uploads").toAbsolutePath().normalize()
                .resolve(stored.getLetterFilePath())));

        // The Proponent keeps exactly one certification, now pointing at the new version.
        assertEquals(1, certificationRepository.findAll().size());
        assertEquals(stored.getLetterFilePath(),
                certificationRepository.findByEndorsementRequestId(request.getId())
                        .orElseThrow().getFilePath());
        assertTrue(eventRepository.findByEndorsementRequestIdOrderByCreatedAtAsc(request.getId())
                .stream().anyMatch(e -> "LETTER_REISSUED".equals(e.getEventType())));
    }

    // ==================== CONTINUE TO PROPOSAL SUBMISSION ====================

    @Test
    void theEndorsementIsLinkedToTheProposalItWasUsedFor() {
        EndorsementRequestResponse request = submittedRequest("Linkable Study");
        authenticateAs(dean);
        endorsementService.approve(request.getId(), approval());

        Proposal proposal = new Proposal();
        proposal.setProponent(proponent);
        proposal.setProjectTitle("Linkable Study");
        proposal.setCollege(COLLEGE);
        proposal.setStatus("SUBMITTED");
        proposal = proposalRepository.saveAndFlush(proposal);

        authenticateAs(proponent);
        EndorsementRequestResponse linked = endorsementService.linkProposal(request.getId(),
                proposal.getId());

        assertEquals(proposal.getId(), linked.getProposalId());
        assertEquals(request.getId(),
                proposalRepository.findById(proposal.getId()).orElseThrow().getEndorsementRequestId());
        assertEquals(proposal.getId(), certificationRepository
                .findByEndorsementRequestId(request.getId()).orElseThrow().getProposalId());
        assertTrue(eventRepository.findByEndorsementRequestIdOrderByCreatedAtAsc(request.getId())
                .stream().anyMatch(e -> "PROPOSAL_LINKED".equals(e.getEventType())));

        // Linking again to the same proposal is idempotent; another proposal is refused.
        endorsementService.linkProposal(request.getId(), proposal.getId());
        Proposal other = new Proposal();
        other.setProponent(proponent);
        other.setProjectTitle("Another Study");
        other.setStatus("DRAFT");
        Proposal otherSaved = proposalRepository.saveAndFlush(other);
        assertThrows(IllegalStateException.class,
                () -> endorsementService.linkProposal(request.getId(), otherSaved.getId()));
    }

    @Test
    void onlyAnApprovedEndorsementCanBeLinked() {
        EndorsementRequestResponse request = submittedRequest("Still pending");

        Proposal proposal = new Proposal();
        proposal.setProponent(proponent);
        proposal.setProjectTitle("Still pending");
        proposal.setStatus("DRAFT");
        Proposal saved = proposalRepository.saveAndFlush(proposal);

        authenticateAs(proponent);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> endorsementService.linkProposal(request.getId(), saved.getId()));
        assertTrue(error.getMessage().contains("approved"), error.getMessage());
    }
    // ==================== PROGRAM TITLE vs PROJECT TITLE ====================

    /**
     * The two titles are distinct values: neither is ever derived from the other, and the
     * deprecated single-title field keeps carrying the PROJECT title so a client built
     * against the previous API version is unaffected.
     */
    @Test
    void theProgramTitleAndTheProjectTitleAreStoredAndReturnedIndependently() {
        authenticateAs(proponent);
        EndorsementRequestResponse request = endorsementService.create(
                newRequest("Mangrove Rehabilitation Study", "MSUN RIIDE - Blue Economy"));

        assertEquals("Mangrove Rehabilitation Study", request.getProjectTitle());
        assertEquals("MSUN RIIDE - Blue Economy", request.getProgramTitle());
        // The deprecated alias is the project title - never the program title.
        assertEquals("Mangrove Rehabilitation Study", request.getProposalTitle());

        EndorsementRequest stored = requestRepository.findById(request.getId()).orElseThrow();
        assertEquals("Mangrove Rehabilitation Study", stored.getProjectTitle());
        assertEquals("MSUN RIIDE - Blue Economy", stored.getProgramTitle());

        // Both values are recorded on the creation entry of the audit trail.
        String created = eventRepository
                .findByEndorsementRequestIdOrderByCreatedAtAsc(request.getId()).stream()
                .filter(event -> "CREATED".equals(event.getEventType()))
                .map(event -> event.getDetail())
                .findFirst().orElseThrow();
        assertTrue(created.contains("Mangrove Rehabilitation Study"), created);
        assertTrue(created.contains("MSUN RIIDE - Blue Economy"), created);
    }

    /** The Project Title is required; the Program Title is optional. */
    @Test
    void theProjectTitleIsRequiredAndTheProgramTitleIsOptional() {
        authenticateAs(proponent);

        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> endorsementService.create(newRequest(null, "MSUN RIIDE - Blue Economy")));
        assertTrue(missing.getMessage().contains("project title"), missing.getMessage());

        EndorsementRequestCreateRequest blankProgram = newRequest("A Study Without A Program");
        blankProgram.setProgramTitle("   ");
        EndorsementRequestResponse saved = endorsementService.create(blankProgram);
        assertNull(saved.getProgramTitle());
        assertEquals("A Study Without A Program", saved.getProjectTitle());
    }

    /**
     * A client that still sends only the previous single-title field keeps working: the
     * value is taken as the project title and no program title is invented for it.
     */
    @Test
    void aLegacySingleTitlePayloadStillCreatesTheRequest() {
        authenticateAs(proponent);
        EndorsementRequestCreateRequest legacy = newRequest(null);
        legacy.setProposalTitle("Legacy Single Title");

        EndorsementRequestResponse created = endorsementService.create(legacy);

        assertEquals("Legacy Single Title", created.getProjectTitle());
        assertEquals("Legacy Single Title", created.getProposalTitle());
        assertNull(created.getProgramTitle());
    }

    /**
     * The Dean's approval keeps both titles, and the official letter prints the PROJECT
     * title: the letter endorses the project, not the programme it belongs to. The
     * Certifications entry is named after the same project title.
     */
    @Test
    void theIssuedLetterAndCertificationNameTheProjectTitleNotTheProgramTitle() throws Exception {
        authenticateAs(proponent);
        EndorsementRequestResponse request = endorsementService.create(
                newRequest("Coral Restoration Study", "MSUN RIIDE - Coastal Program"));

        authenticateAs(dean);
        EndorsementRequestResponse approved = endorsementService.approve(request.getId(), approval());

        assertEquals("Coral Restoration Study", approved.getProjectTitle());
        assertEquals("MSUN RIIDE - Coastal Program", approved.getProgramTitle());

        Certification certification = certificationRepository
                .findByEndorsementRequestId(request.getId()).orElseThrow();
        assertEquals("Endorsement Letter - Coral Restoration Study", certification.getTitle());

        // The stored PDF is the authority: it names the project title and it does not
        // print the program title anywhere.
        EndorsementRequest stored = requestRepository.findById(request.getId()).orElseThrow();
        String letter = normalizedTextOf(Files.readAllBytes(
                Paths.get("uploads").toAbsolutePath().normalize().resolve(stored.getLetterFilePath())));
        assertTrue(letter.contains("Coral Restoration Study"), letter);
        assertFalse(letter.contains("MSUN RIIDE - Coastal Program"), letter);
    }

    /**
     * The page text with every run of whitespace collapsed. A phrase the renderer wrapped
     * onto the next visual line comes back from the extractor with a line break inside it,
     * so the assertions match against this normalized form (same helper as
     * {@link EndorsementLetterServiceTest}).
     */
    private String normalizedTextOf(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(new PdfTextExtractor(reader).getTextFromPage(page)).append('\n');
            }
            return text.toString().replaceAll("\\s+", " ").trim();
        } finally {
            reader.close();
        }
    }
}
