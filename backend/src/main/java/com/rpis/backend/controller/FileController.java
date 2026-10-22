package com.rpis.backend.controller;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Serves uploaded documents (Special Orders and proposal attachments) from the
 * local "uploads" directory as downloadable files.
 * <p>
 * Requires authentication. A Faculty Proponent may only download files that are
 * referenced by one of their own proposals; staff/reviewing roles keep full
 * access. This closes the previous public (permitAll) file route.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final Path UPLOAD_ROOT = Paths.get("uploads").toAbsolutePath().normalize();

    private final ProposalRepository proposalRepository;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<Resource> download(@RequestParam("path") String path) throws MalformedURLException {
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Path resolved = UPLOAD_ROOT.resolve(path).normalize();
        // Prevent path traversal outside the uploads directory.
        if (!resolved.startsWith(UPLOAD_ROOT)) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new UrlResource(resolved.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        requireFileAccess(resolved);

        String filename = resolved.getFileName() != null ? resolved.getFileName().toString() : "download";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * A Proponent may only download a file that is referenced by one of their own
     * proposals. Staff and reviewing roles are unaffected.
     */
    private void requireFileAccess(Path resolved) {
        if (!currentUser.isProponent()) {
            return;
        }
        String requestedName = resolved.getFileName() != null ? resolved.getFileName().toString() : "";
        Long userId = currentUser.requireUserId();

        List<Proposal> myProposals = proposalRepository.findByProponentId(userId);
        boolean owned = myProposals.stream().anyMatch(p -> referencesFile(p, requestedName));
        if (!owned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to access this file.");
        }
    }

    private boolean referencesFile(Proposal proposal, String requestedName) {
        return fileNameMatches(proposal.getReviewFileName(), requestedName)
                || fileNameMatches(proposal.getRoadmapFileName(), requestedName)
                || fileNameMatches(proposal.getRoadmapFilePath(), requestedName)
                || fileNameMatches(proposal.getBeneficiariesFileName(), requestedName)
                || fileNameMatches(proposal.getBeneficiariesFilePath(), requestedName)
                || fileNameMatches(proposal.getGadFileName(), requestedName)
                || fileNameMatches(proposal.getSoFileName(), requestedName)
                || fileNameMatches(proposal.getSoFilePath(), requestedName);
    }

    private boolean fileNameMatches(String stored, String requestedName) {
        if (stored == null || stored.isBlank()) {
            return false;
        }
        String normalized = stored.replace('\\', '/');
        String storedName = normalized.substring(normalized.lastIndexOf('/') + 1);
        return storedName.equals(requestedName);
    }
}
