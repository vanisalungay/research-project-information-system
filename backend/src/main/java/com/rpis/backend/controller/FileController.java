package com.rpis.backend.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves uploaded documents (Special Orders, and later proposal attachments)
 * from the local "uploads" directory as downloadable files.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Path UPLOAD_ROOT = Paths.get("uploads").toAbsolutePath().normalize();

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

        String filename = resolved.getFileName() != null ? resolved.getFileName().toString() : "download";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
