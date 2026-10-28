package com.rpis.backend.controller;

import com.rpis.backend.model.EndorsementRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.EndorsementRequestRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.storage.LocalUploadStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Authorization tests for the uploaded-document route.
 *
 * <p>These pin down the allow-list that replaced the previous
 * {@code if (!isProponent()) return;} check. That check released <em>every</em> stored
 * document to any authenticated account that was not a Proponent - including a Dean,
 * which is deliberately not one of the privileged review roles - so the tests below
 * cover each role explicitly rather than only the Proponent case.
 *
 * <p>The storage root is a temporary directory, so the tests never touch a real upload
 * directory and cannot depend on the working directory.
 */
class FileControllerTest {

    private static final String FILE_NAME = "rpis-attachment-test.txt";
    private static final String SUBDIRECTORY = "special-orders";

    @TempDir
    Path storageRoot;

    private ProposalRepository proposalRepository;
    private EndorsementRequestRepository endorsementRequestRepository;
    private FileController controller;
    private String storedPath;

    @BeforeEach
    void setUp() {
        LocalUploadStorage storage = new LocalUploadStorage(storageRoot.toString());
        // Stored through the storage layer, so the test exercises the real convention
        // (a path relative to the root) instead of inventing one.
        storedPath = storage.store("test attachment".getBytes(), SUBDIRECTORY, FILE_NAME);

        proposalRepository = mock(ProposalRepository.class);
        endorsementRequestRepository = mock(EndorsementRequestRepository.class);
        // Default: this caller owns nothing and is assigned no endorsement.
        when(proposalRepository.findByProponentId(anyLong())).thenReturn(List.of());
        when(endorsementRequestRepository.findByAssignedDeanIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of());

        controller = new FileController(storage, proposalRepository, endorsementRequestRepository,
                new CurrentUser());
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Proposal proposalWithAttachment(Long ownerId, String filePath) {
        User owner = new User();
        owner.setId(ownerId);
        Proposal proposal = new Proposal();
        proposal.setId(1L);
        proposal.setProponent(owner);
        proposal.setSoFilePath(filePath);
        return proposal;
    }

    private EndorsementRequest endorsementWithLetter(Long deanId, String filePath) {
        User dean = new User();
        dean.setId(deanId);
        EndorsementRequest request = new EndorsementRequest();
        request.setId(1L);
        request.setAssignedDean(dean);
        request.setLetterFilePath(filePath);
        return request;
    }

    // ------------------------------------------------------------ the owner

    @Test
    void proponentCanDownloadTheirOwnAttachment() {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findByProponentId(7L))
                .thenReturn(List.of(proposalWithAttachment(7L, storedPath)));

        assertEquals(HttpStatus.OK, controller.download(storedPath).getStatusCode());
    }

    @Test
    void proponentCannotDownloadAnAttachmentTheyDoNotOwn() {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findByProponentId(7L))
                .thenReturn(List.of(proposalWithAttachment(7L, SUBDIRECTORY + "/someone-elses.pdf")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.download(storedPath));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * A stored value that carries the historic {@code uploads/} prefix must still match,
     * because that is the shape rows written before the storage abstraction hold.
     */
    @Test
    void aHistoricStoredPathWithTheUploadsPrefixStillMatches() {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findByProponentId(7L))
                .thenReturn(List.of(proposalWithAttachment(7L, "uploads/" + storedPath)));

        assertEquals(HttpStatus.OK, controller.download(storedPath).getStatusCode());
    }

    // ------------------------------------------------------------ the roles

    @Test
    void staffCanDownloadAnyAttachment() {
        authenticate(2L, "RPS_STAFF");
        assertEquals(HttpStatus.OK, controller.download(storedPath).getStatusCode());
    }

    @Test
    void everyReviewingRoleCanDownloadAnyAttachment() {
        for (String role : List.of("RPS_ADMIN", "OVCRIGE", "OVCAF", "REC", "OC")) {
            authenticate(2L, role);
            assertEquals(HttpStatus.OK, controller.download(storedPath).getStatusCode(),
                    role + " must keep full document access");
        }
    }

    /**
     * The regression this change fixes: a Dean used to fall through the Proponent check
     * and receive unrestricted access to every document.
     */
    @Test
    void aDeanCanOnlyDownloadTheLettersAssignedToThem() {
        authenticate(9L, UserRoles.DEAN);

        // Not their letter: refused, where the previous code allowed it.
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.download(storedPath));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        // Their own letter: allowed.
        when(endorsementRequestRepository.findByAssignedDeanIdOrderByCreatedAtDesc(9L))
                .thenReturn(List.of(endorsementWithLetter(9L, storedPath)));
        assertEquals(HttpStatus.OK, controller.download(storedPath).getStatusCode());
    }

    /** An unrecognized or future role must be refused rather than fall through. */
    @Test
    void anUnrecognizedRoleIsRefused() {
        authenticate(3L, "SOME_FUTURE_ROLE");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.download(storedPath));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ------------------------------------------------------------ the storage

    @Test
    void aMissingFileIsNotFound() {
        authenticate(2L, "RPS_STAFF");
        assertEquals(HttpStatus.NOT_FOUND,
                controller.download("special-orders/does-not-exist.pdf").getStatusCode());
    }

    /** A traversal attempt must not reach outside the storage root. */
    @Test
    void aTraversalAttemptIsNotFound() {
        authenticate(2L, "RPS_STAFF");
        assertEquals(HttpStatus.NOT_FOUND,
                controller.download("../../etc/passwd").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                controller.download("..\\..\\windows\\win.ini").getStatusCode());
    }

    @Test
    void aBlankPathIsRejected() {
        authenticate(2L, "RPS_STAFF");
        assertEquals(HttpStatus.BAD_REQUEST, controller.download("").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.download("   ").getStatusCode());
    }
}
