package com.rpis.backend.controller;

import com.rpis.backend.dto.CertificationResponse;
import com.rpis.backend.model.Certification;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The Proponent's Certifications section.
 *
 * <p>An endorsement letter appears here automatically once the Dean issues it - the
 * Proponent never uploads it. Both endpoints are scoped to the authenticated caller:
 * the list is always the caller's own documents, and a single document is only
 * served to its owner, the Dean who issued it, or RPS staff / administrator.
 */
@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;
    private final CurrentUser currentUser;

    /** The authenticated Proponent's certification documents, newest first. */
    @GetMapping
    public ResponseEntity<List<CertificationResponse>> list() {
        return ResponseEntity.ok(
                certificationService.listForProponent(currentUser.requireUserId()));
    }

    /**
     * One certification document. The same stored file is served for the in-app
     * preview and for the download, so what is displayed is what is downloaded.
     */
    @GetMapping("/{id}/document")
    public ResponseEntity<Resource> document(
            @PathVariable Long id,
            @RequestParam(name = "download", defaultValue = "false") boolean download) {
        Certification certification = certificationService.requireReadable(id,
                currentUser.requireUserId(), currentUser.getRole());
        CertificationService.StoredDocument document =
                certificationService.loadDocument(certification);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, (download ? "attachment" : "inline")
                        + "; filename=\"" + document.fileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new FileSystemResource(document.path()));
    }
}
