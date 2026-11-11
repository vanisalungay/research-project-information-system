package com.rpis.backend.dto;

/**
 * Request body for {@code POST /api/documents/pdf}.
 *
 * <p>The frontend builds the proposal document with the one shared template and
 * sends the finished HTML here; this endpoint only renders it. The document's
 * own {@code <title>} supplies the PDF metadata title and is never drawn on a
 * page - the document has its own printed header.
 */
public record DocumentPdfRequest(String html) {
}
