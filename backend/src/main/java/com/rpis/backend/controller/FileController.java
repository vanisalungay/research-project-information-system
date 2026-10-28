package com.rpis.backend.controller;

import com.rpis.backend.model.EndorsementRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.EndorsementRequestRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.storage.UploadStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Serves uploaded documents (Special Orders, endorsement letters and proposal
 * attachments) to the accounts allowed to read them.
 *
 * <h2>Authorization is an allow-list, and it fails closed</h2>
 * The previous version of this controller only checked one role:
 *
 * <pre>{@code
 * if (!currentUser.isProponent()) {
 *     return;   // everyone else: unrestricted
 * }
 * }</pre>
 *
 * <p>That granted unrestricted access to every file to any authenticated account that
 * was not a Proponent - including a Dean ({@code DEAN}), which is deliberately not
 * one of the privileged review roles. Access is now an explicit allow-list: the
 * reviewing/administrative roles may read any document, a Proponent may read only the
 * documents their own proposals reference, a Dean may read the endorsement letters
 * assigned to them, and any other or unrecognized role is refused.
 *
 * <h2>Ownership is matched on the whole path</h2>
 * The previous check compared only the <em>file name</em>, so any file anywhere in the
 * upload tree whose name happened to match was released. The comparison now uses the
 * full normalised relative path, which is also why the two historic path shapes (with
 * and without a leading {@code uploads/}) are normalised rather than compared raw.
 *
 * <p>Path traversal is not handled here at all: {@link UploadStorage} resolves the
 * requested value and returns nothing when it escapes the storage root, so this
 * controller cannot be tricked into reading outside it.
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    /** Reviewing and administrative roles that may read any stored document. */
    private static final List<String> UNRESTRICTED_ROLES = List.of(
            UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF, UserRoles.OVCRIGE, UserRoles.OVCAF,
            UserRoles.REC, UserRoles.OC);

    private static final String ROLE_PROPONENT = UserRoles.PROPONENT;
    private static final String ROLE_DEAN = UserRoles.DEAN;

    private final UploadStorage uploadStorage;
    private final ProposalRepository proposalRepository;
    private final EndorsementRequestRepository endorsementRequestRepository;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<InputStreamResource> download(@RequestParam("path") String path) {
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Resolves to nothing when the value escapes the storage root, so an invalid or
        // hostile path is refused before authorization is even considered.
        UploadStorage.StoredUpload stored = uploadStorage.open(path).orElse(null);
        if (stored == null) {
            return ResponseEntity.notFound().build();
        }

        requireFileAccess(path);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + stored.fileName() + "\"")
                .contentLength(stored.size())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(stored.content()));
    }

    /**
     * Refuses the request unless the caller is allowed to read {@code requestedPath}.
     *
     * @throws ResponseStatusException 403 when the caller is not allowed
     */
    private void requireFileAccess(String requestedPath) {
        String role = currentUser.getRole();
        Long userId = currentUser.requireUserId();
        String wanted = normalize(requestedPath);

        if (role != null && UNRESTRICTED_ROLES.contains(role.toUpperCase(Locale.ROOT))) {
            return;
        }
        if (ROLE_PROPONENT.equalsIgnoreCase(role)) {
            requireOwnedByProposal(userId, wanted);
            return;
        }
        if (ROLE_DEAN.equalsIgnoreCase(role)) {
            requireOwnedByEndorsement(userId, wanted);
            return;
        }

        // Any other or unrecognized role: deny. Failing closed is deliberate - a role
        // added later must be granted document access explicitly.
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not authorized to access this file.");
    }

    /** A Proponent may read a file that one of their own proposals references. */
    private void requireOwnedByProposal(Long userId, String wanted) {
        for (Proposal proposal : proposalRepository.findByProponentId(userId)) {
            if (referencesProposalFile(proposal, wanted)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not authorized to access this file.");
    }

    /** A Dean may read the endorsement letters assigned to them. */
    private void requireOwnedByEndorsement(Long deanId, String wanted) {
        for (EndorsementRequest request
                : endorsementRequestRepository.findByAssignedDeanIdOrderByCreatedAtDesc(deanId)) {
            if (matches(request.getLetterFilePath(), wanted)
                    || matches(request.getSignatureFilePath(), wanted)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not authorized to access this file.");
    }

    private boolean referencesProposalFile(Proposal proposal, String wanted) {
        return matches(proposal.getReviewFileName(), wanted)
                || matches(proposal.getRoadmapFilePath(), wanted)
                || matches(proposal.getRoadmapFileName(), wanted)
                || matches(proposal.getBeneficiariesFilePath(), wanted)
                || matches(proposal.getBeneficiariesFileName(), wanted)
                || matches(proposal.getGadFileName(), wanted)
                || matches(proposal.getSoFilePath(), wanted)
                || matches(proposal.getSoFileName(), wanted);
    }

    /**
     * True when a stored value refers to the requested path.
     *
     * <p>Both sides are normalised (separators, and a leading {@code uploads/} removed)
     * and then compared in full. A stored value that names only a file name still
     * matches, because that is all some historic rows hold.
     */
    private boolean matches(String stored, String wanted) {
        if (stored == null || stored.isBlank() || wanted == null || wanted.isBlank()) {
            return false;
        }
        String normalised = normalize(stored);
        return normalised.equals(wanted)
                || normalised.endsWith("/" + wanted)
                || wanted.endsWith("/" + normalised);
    }

    /** Separators unified and the historic {@code uploads/} prefix removed. */
    private String normalize(String path) {
        String cleaned = path.replace('\\', '/').trim();
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("uploads/")) {
            cleaned = cleaned.substring("uploads/".length());
        }
        return cleaned;
    }
}
