package com.rpis.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.rpis.backend.model.Notification;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.TokenService;
import com.rpis.backend.service.UserSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The wire contract of the Request for Endorsement endpoint, exercised through the real
 * Spring MVC and Spring Security stack.
 *
 * <p>It pins Program Title and Project Title as two separate JSON fields: they are
 * accepted and returned independently, and the deprecated single-title field
 * ({@code proposalTitle}) is still read and written as the PROJECT title. This is the
 * contract the Request for Endorsement form posts, the Dean screens display and the
 * Create Proposal carry-over reads, so a rename or a swap on either side fails here
 * instead of silently showing the wrong title to the Proponent or the Dean.
 *
 * <p>Runs against the isolated {@code rpis_test} database only, and every request is
 * rolled back with the test transaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EndorsementRequestApiTest {

    private static final String COLLEGE = "College of Fisheries and Marine Sciences (CFMS)";
    private static final String PROGRAM_TITLE = "MSUN RIIDE - Blue Economy and Marine Resources";
    private static final String PROJECT_TITLE = "Coral Restoration Study";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProposalAnnouncementRepository announcementRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserSignatureService signatureService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String proponentToken;
    private String deanToken;
    private Long proponentId;
    private Long deanId;
    private Long callId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE endorsement_request_events RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE certifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE endorsement_requests RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE notifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        User proponent = new User();
        proponent.setName("Ana Proponent");
        proponent.setEmail("api.proponent@msunaawan.edu.ph");
        proponent.setPassword("secret");
        proponent.setRole("PROPONENT");
        proponent.setStatus("APPROVED");
        proponent = userRepository.saveAndFlush(proponent);
        proponentId = proponent.getId();
        proponentToken = tokenService.generateToken(proponent.getId(), proponent.getRole());

        // The Dean authorized for the college of the request below, with the electronic
        // signature the official letter is signed with.
        User dean = new User();
        dean.setName("Dean Maria Santos");
        dean.setEmail("api.dean@msunaawan.edu.ph");
        dean.setPassword("secret");
        dean.setRole(UserRoles.DEAN);
        dean.setStatus("APPROVED");
        dean.setAssignedCollege(COLLEGE);
        dean = userRepository.saveAndFlush(dean);
        deanId = dean.getId();
        deanToken = tokenService.generateToken(dean.getId(), dean.getRole());
        signatureService.saveSignature(deanId, signatureUpload());

        ProposalAnnouncement call = new ProposalAnnouncement();
        call.setName("Internally Funded Research Program 2026");
        call.setStartDate(LocalDate.now().minusDays(1));
        call.setEndDate(LocalDate.now().plusDays(7));
        call.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        call.setFundingProgram(ProposalAnnouncement.FUNDING_PROGRAM_IFP);
        callId = announcementRepository.saveAndFlush(call).getId();
    }

    /** A real PNG, so the signature service accepts it exactly like an upload. */
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

    /**
     * Removes the files an approval wrote. The database rows roll back with the test
     * transaction; the stored letter and signature snapshots do not, so the test cleans
     * them up itself.
     */
    private void removeStoredFiles(Long requestId, Long deanAccountId) {
        for (String stored : jdbcTemplate.queryForList(
                "SELECT letter_file_path FROM endorsement_requests WHERE id = ?",
                String.class, requestId)) {
            deleteStoredFile(stored);
        }
        for (String stored : jdbcTemplate.queryForList(
                "SELECT signature_file_path FROM endorsement_requests WHERE id = ?",
                String.class, requestId)) {
            deleteStoredFile(stored);
        }
        for (String stored : jdbcTemplate.queryForList(
                "SELECT signature_file_path FROM users WHERE id = ?",
                String.class, deanAccountId)) {
            deleteStoredFile(stored);
        }
    }

    private void deleteStoredFile(String stored) {
        if (stored == null) {
            return;
        }
        Path resolved = Paths.get("uploads").toAbsolutePath().normalize().resolve(stored);
        try {
            Files.deleteIfExists(resolved);
        } catch (Exception ignored) {
            // A leftover test file is not worth failing a test for.
        }
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken);
    }

    /** A JSON payload carrying exactly the fields the form sends. */
    private String payload(String programTitleField, String projectTitleField) {
        return "{"
                + programTitleField
                + projectTitleField
                + "\"proposalAnnouncementId\":" + callId + ","
                + "\"college\":\"" + COLLEGE + "\","
                + "\"department\":null,"
                + "\"researchSummary\":null,"
                + "\"notes\":null"
                + "}";
    }

    private String field(String name, String value) {
        return "\"" + name + "\":\"" + value + "\",";
    }

    private MvcResult create(String json) throws Exception {
        return mockMvc.perform(withToken(post("/api/endorsements"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andReturn();
    }

    @Test
    void theTwoTitlesAreAcceptedAndReturnedAsSeparateFields() throws Exception {
        MvcResult created = create(payload(field("programTitle", PROGRAM_TITLE),
                field("projectTitle", PROJECT_TITLE)));
        String body = created.getResponse().getContentAsString();

        assertEquals(PROGRAM_TITLE, JsonPath.read(body, "$.programTitle"));
        assertEquals(PROJECT_TITLE, JsonPath.read(body, "$.projectTitle"));
        // The deprecated alias is the project title - never the program title.
        assertEquals(PROJECT_TITLE, JsonPath.read(body, "$.proposalTitle"));

        // Both values survive the round trip to the database and back.
        Object id = JsonPath.read(body, "$.id");
        MvcResult reread = mockMvc.perform(withToken(get("/api/endorsements/" + id)))
                .andExpect(status().isOk())
                .andReturn();
        String stored = reread.getResponse().getContentAsString();
        assertEquals(PROGRAM_TITLE, JsonPath.read(stored, "$.programTitle"));
        assertEquals(PROJECT_TITLE, JsonPath.read(stored, "$.projectTitle"));
    }

    @Test
    void anEmptyProgramTitleIsAcceptedAndNeverBecomesTheProjectTitle() throws Exception {
        String body = create(payload(field("programTitle", ""), field("projectTitle", PROJECT_TITLE)))
                .getResponse().getContentAsString();

        // The optional Program Title stays empty: it is not filled from the project title.
        assertNull(JsonPath.read(body, "$.programTitle"));
        assertEquals(PROJECT_TITLE, JsonPath.read(body, "$.projectTitle"));
    }

    @Test
    void theLegacySingleTitleFieldIsStillReadAsTheProjectTitle() throws Exception {
        String body = create(payload("", field("proposalTitle", "Legacy Single Title")))
                .getResponse().getContentAsString();

        assertEquals("Legacy Single Title", JsonPath.read(body, "$.projectTitle"));
        assertNull(JsonPath.read(body, "$.programTitle"));
    }

    @Test
    void aRequestWithoutAProjectTitleIsRefusedWithAReadableMessage() throws Exception {
        MvcResult refused = mockMvc.perform(withToken(post("/api/endorsements"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(field("programTitle", PROGRAM_TITLE), "")))
                .andExpect(status().isBadRequest())
                .andReturn();

        String message = JsonPath.read(refused.getResponse().getContentAsString(), "$.error");
        // A Program Title can never stand in for the required Project Title.
        assertTrue(message.contains("project title"), message);
    }

    /**
     * The Dean's decision, exercised exactly as the Dean screen calls it: the same
     * endpoint, method, payload and bearer token. It pins the facts the screen depends on
     * afterwards - the response reports the issued letter, the letter is served as a PDF -
     * and the notification target that made clicking the Dean's notification open the
     * Proponent's announcement page (403 "not authorized to view this announcement")
     * instead of the endorsement request.
     */
    @Test
    void theDeanApprovesThroughTheApiAndTheLetterIsIssued() throws Exception {
        String created = create(payload(field("programTitle", PROGRAM_TITLE),
                field("projectTitle", PROJECT_TITLE))).getResponse().getContentAsString();
        Object id = JsonPath.read(created, "$.id");
        Long requestId = Long.valueOf(String.valueOf(id));

        // The Dean was notified of the request waiting for a decision, and the
        // notification carries that request - not only its Call for Proposals.
        List<Notification> deanNotifications = notificationRepository
                .findByUserOrderByCreatedAtDesc(userRepository.findById(deanId).orElseThrow());
        Notification waiting = deanNotifications.stream()
                .filter(notification -> notification.getMessage() != null
                        && notification.getMessage().contains("waiting for your endorsement"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the Dean must be notified"));
        assertEquals("ENDORSEMENT", waiting.getType());
        assertEquals(requestId, waiting.getProposalId());

        String approval = "{"
                + "\"endorserName\":\"Dean Maria Santos\","
                + "\"endorserDesignation\":\"Dean\","
                + "\"endorsementDate\":\"" + LocalDate.now() + "\"}";
        MvcResult approved = mockMvc.perform(put("/api/endorsements/" + requestId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approval))
                .andExpect(status().isOk())
                .andReturn();
        String body = approved.getResponse().getContentAsString();
        assertEquals("APPROVED", JsonPath.read(body, "$.status"));
        assertEquals(Boolean.TRUE, JsonPath.read(body, "$.hasLetter"));
        assertNotNull(JsonPath.read(body, "$.letterDocumentCode"));

        // The issued letter is served by the existing document endpoint, for its owner.
        MvcResult document = mockMvc.perform(get("/api/endorsements/" + requestId + "/document")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(MediaType.APPLICATION_PDF_VALUE,
                document.getResponse().getContentType());
        assertTrue(document.getResponse().getContentAsByteArray().length > 0,
                "the issued letter must be a real document");

        // The Proponent is told the outcome, and that notification opens the request too.
        Notification endorsed = notificationRepository
                .findByUserOrderByCreatedAtDesc(userRepository.findById(proponentId).orElseThrow())
                .stream()
                .filter(notification -> notification.getMessage() != null
                        && notification.getMessage().contains("was endorsed"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the Proponent must be notified"));
        assertEquals(requestId, endorsed.getProposalId());

        removeStoredFiles(requestId, deanId);
    }
}
