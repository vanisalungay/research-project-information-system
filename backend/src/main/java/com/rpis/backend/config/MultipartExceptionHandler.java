package com.rpis.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

/**
 * Turns a multipart rejection into the JSON error shape the frontend already reads
 * ({@code {"error": "..."}}) instead of the container's opaque 413 page.
 *
 * <h2>Why this exists</h2>
 * The servlet multipart parser refuses a request before any controller method runs,
 * so an application-level validation message can never be shown for it. The
 * application limits are therefore set below the container ceilings in
 * {@code application.properties}, which means this handler only fires for a request
 * beyond the ceiling itself - and it still tells the caller the largest accepted
 * size rather than returning an empty 413.
 */
@Slf4j
@RestControllerAdvice
public class MultipartExceptionHandler {

    /**
     * The largest file the application accepts anywhere, in megabytes. Kept in step
     * with the Special Order limit because that is the largest upload the forms
     * offer; the message names the same number the form enforces.
     */
    @Value("${rpis.special-order.max-size-mb:25}")
    private long maxFileSizeMb = 25;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Rejected an oversized multipart upload: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "The uploaded file is too large. The maximum accepted size is "
                        + maxFileSizeMb + " MB. Please upload a smaller file."));
    }

    /**
     * A malformed multipart request (for example a missing boundary) is a client
     * error, not a server failure; it is answered with the same JSON shape so the
     * form can show a meaningful reason.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> handleMultipartError(MultipartException ex) {
        log.warn("Rejected a malformed multipart request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "The file upload could not be read. Please choose the file again."));
    }
}
