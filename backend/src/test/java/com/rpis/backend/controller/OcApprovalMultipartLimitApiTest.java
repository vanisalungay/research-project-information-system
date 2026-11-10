package com.rpis.backend.controller;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The HTTP 413 investigation, verified through a real servlet container.
 *
 * <p>The application limit is deliberately below the container ceilings, so:
 * <ul>
 *   <li>a file above the application limit but below the container ceilings reaches
 *       the controller and is answered with a readable 400 containing the reason;</li>
 *   <li>only a file above the container ceilings is refused by the multipart parser,
 *       and that 413 is translated into the same JSON error shape.</li>
 * </ul>
 * The limits are scaled down here (app 1 MB, container 2/3 MB) so the test exercises
 * the same layering without sending a 30 MB body. The production values are the same
 * relationship: app 25 MB, container 30/35 MB.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=update",
                "spring.servlet.multipart.max-file-size=2MB",
                "spring.servlet.multipart.max-request-size=3MB",
                // Tomcat must be able to swallow the whole rejected body, otherwise the
                // connection is reset mid-upload and the client never reads the 413.
                "server.tomcat.max-swallow-size=3MB",
                "rpis.special-order.max-size-mb=1"
        })
@ActiveProfiles("test")
class OcApprovalMultipartLimitApiTest {

    private static final byte[] PDF_HEADER = "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String ocToken;
    private Proposal proposal;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        User proponent = user("Limit Proponent", "proponent.limit@msunaawan.edu.ph", "PROPONENT");
        User oc = user("Limit Chancellor", "chancellor.limit@msunaawan.edu.ph", "OC");
        ocToken = tokenService.generateToken(oc.getId(), "OC");

        proposal = new Proposal();
        proposal.setDocumentId("TEST-LIMIT-" + System.nanoTime());
        proposal.setProjectTitle("Multipart Limit Proposal");
        proposal.setStatus("FOR_OC_APPROVAL");
        proposal.setProponent(proponent);
        proposal = proposalRepository.saveAndFlush(proposal);
    }

    private User user(String name, String email, String role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus("APPROVED");
        return userRepository.saveAndFlush(user);
    }

    private ResponseEntity<String> approveWith(byte[] content, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("soNumber", "SO-2026-LIMIT");
        body.add("needsBudget", "true");
        body.add("soFile", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(ocToken);

        return restTemplate.exchange("/api/proposals/{id}/approve-with-so", HttpMethod.PUT,
                new HttpEntity<>(body, headers), String.class, proposal.getId());
    }

    private byte[] pdfOfSize(int bytes) {
        byte[] content = new byte[bytes];
        System.arraycopy(PDF_HEADER, 0, content, 0, PDF_HEADER.length);
        return content;
    }

    @Test
    void aFileAboveTheApplicationLimitIsAnsweredWithAReadable400() {
        // 1.5 MB: above the 1 MB application limit, below the 2/3 MB container ceiling.
        ResponseEntity<String> response = approveWith(pdfOfSize(1536 * 1024), "SO.pdf");

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody() != null && response.getBody().contains("1 MB"),
                "the response must name the actual limit: " + response.getBody());
        assertEquals("FOR_OC_APPROVAL", jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId()));
    }

    @Test
    void aFileAboveTheContainerCeilingIsAnsweredWithAJson413() {
        // 2.5 MB: above the 2 MB per-file ceiling, so the multipart parser refuses it.
        ResponseEntity<String> response = approveWith(pdfOfSize(2560 * 1024), "SO.pdf");

        assertEquals(413, response.getStatusCode().value());
        assertTrue(response.getBody() != null && response.getBody().contains("\"error\""),
                "the 413 must carry the JSON error shape: " + response.getBody());
        assertEquals("FOR_OC_APPROVAL", jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId()));
    }

    @Test
    void aValidSmallPdfIsApprovedThroughTheRealServer() {
        ResponseEntity<String> response = approveWith(pdfOfSize(2048), "SO.pdf");

        assertEquals(200, response.getStatusCode().value(), response.getBody());
        assertEquals("FOR_OVCAF_APPROVAL", jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId()));
        assertTrue(jdbcTemplate.queryForObject(
                        "SELECT so_uploaded_at FROM proposals WHERE id = ?", Object.class, proposal.getId()) != null,
                "the OC approval record must be written");
    }
}
