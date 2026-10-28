package com.rpis.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private JdbcTemplate jdbcTemplate;

    private String proponentToken;
    private Long callId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE endorsement_request_events RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE certifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE endorsement_requests RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        User proponent = new User();
        proponent.setName("Ana Proponent");
        proponent.setEmail("api.proponent@msunaawan.edu.ph");
        proponent.setPassword("secret");
        proponent.setRole("PROPONENT");
        proponent.setStatus("APPROVED");
        proponent = userRepository.saveAndFlush(proponent);
        proponentToken = tokenService.generateToken(proponent.getId(), proponent.getRole());

        ProposalAnnouncement call = new ProposalAnnouncement();
        call.setName("Internally Funded Research Program 2026");
        call.setStartDate(LocalDate.now().minusDays(1));
        call.setEndDate(LocalDate.now().plusDays(7));
        call.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        call.setFundingProgram(ProposalAnnouncement.FUNDING_PROGRAM_IFP);
        callId = announcementRepository.saveAndFlush(call).getId();
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
}
