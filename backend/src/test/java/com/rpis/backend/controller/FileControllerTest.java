package com.rpis.backend.controller;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that uploaded proposal attachments are only downloadable by their
 * owner (Proponent) or by authorized staff, closing the previous public file
 * route.
 */
class FileControllerTest {

    private static final String FILE_NAME = "rpis-attachment-test.txt";
    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath();

    private ProposalRepository proposalRepository;
    private FileController controller;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(UPLOAD_DIR);
        Files.writeString(UPLOAD_DIR.resolve(FILE_NAME), "test attachment");

        proposalRepository = mock(ProposalRepository.class);
        controller = new FileController(proposalRepository, new CurrentUser());
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(UPLOAD_DIR.resolve(FILE_NAME));
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Proposal proposalWithAttachment(Long ownerId, String fileName) {
        User owner = new User();
        owner.setId(ownerId);
        Proposal proposal = new Proposal();
        proposal.setId(1L);
        proposal.setProponent(owner);
        proposal.setReviewFileName(fileName);
        return proposal;
    }

    @Test
    void proponentCanDownloadTheirOwnAttachment() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findByProponentId(7L))
                .thenReturn(List.of(proposalWithAttachment(7L, FILE_NAME)));

        ResponseEntity<?> response = controller.download(FILE_NAME);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void proponentCannotDownloadAnAttachmentTheyDoNotOwn() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findByProponentId(7L))
                .thenReturn(List.of(proposalWithAttachment(7L, "someone-elses.pdf")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.download(FILE_NAME));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void staffCanDownloadAnyAttachment() throws Exception {
        authenticate(2L, "RPS_STAFF");

        ResponseEntity<?> response = controller.download(FILE_NAME);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
