package com.rpis.backend.controller;

import com.rpis.backend.dto.DocumentPdfRequest;
import com.rpis.backend.service.DocumentPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * The single PDF rendering endpoint used by every proposal Download action.
 *
 * <p>Rendering on the server is what removes the browser's automatic print
 * header/footer (date, time, document title, URL, page number) from downloaded
 * proposals: no print dialog is involved, so none of that metadata can appear.
 * The document's own {@code @bottom-right} page-number margin box is rendered by
 * the PDF engine instead.
 *
 * <p>Authentication is required by the security chain's catch-all rule. The
 * endpoint renders a document the caller's browser already has; it reads no
 * record itself and cannot expose another user's data beyond the HTML supplied.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentPdfController {

    private final DocumentPdfService documentPdfService;

    @PostMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@RequestBody DocumentPdfRequest request) {
        byte[] pdf = documentPdfService.render(request == null ? null : request.html());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                // The browser saves the file using the download attribute the frontend
                // sets; this header keeps the endpoint's own default name sane.
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"proposal.pdf\"")
                .body(pdf);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidDocument(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", error.getMessage() == null ? "The document is invalid." : error.getMessage()));
    }
}
