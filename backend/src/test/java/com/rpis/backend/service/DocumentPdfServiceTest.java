package com.rpis.backend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The proposal PDF is rendered on the server precisely so no browser print
 * header/footer (date, timestamp, browser title, URL) can appear in it. These
 * tests read the produced file back: the page count and the document's own
 * bottom-right page numbers must be part of the PDF.
 */
class DocumentPdfServiceTest {

    private final DocumentPdfService service = new DocumentPdfService();

    private static String html(int blocks) {
        StringBuilder body = new StringBuilder();
        for (int i = 1; i <= blocks; i++) {
            body.append("<p>Block ").append(i).append(" content</p>");
        }
        return "<!doctype html><html><head><meta charset='utf-8'>"
                + "<title>JUL REJECT BY REC</title><style>"
                + "@page { size: A4; margin: 9mm 13mm 9mm;"
                + " @bottom-right { content: \"Page \" counter(page) \" of \" counter(pages);"
                + " font-family: Arial, sans-serif; font-size: 8pt; } }"
                + "html,body{margin:0;padding:0;font-family:Arial,sans-serif;font-size:10pt}"
                + "body{padding:8.8mm 0 22mm} p{height:80mm;margin:0}"
                + "</style></head><body>" + body + "</body></html>";
    }

    /** Captures the first drawn page number and where it sits on the page. */
    private static final class PageNumberStripper extends PDFTextStripper {
        float x = -1;
        float y = -1;
        String text = "";

        PageNumberStripper() throws IOException {
            super();
        }

        /** The margin box's spaces are non-breaking; compare with ordinary spaces. */
        static String normalise(String value) {
            return value == null ? "" : value.replaceAll("[\\s\\u00A0\\u202F]+", " ").trim();
        }

        @Override
        protected void writeString(String string, List<TextPosition> positions) throws IOException {
            String normalised = normalise(string);
            if (x < 0 && normalised.contains("Page 1 of")) {
                TextPosition first = positions.get(0);
                x = first.getXDirAdj();
                y = first.getYDirAdj();
                text = normalised;
            }
            super.writeString(string, positions);
        }
    }

    @Test
    void theRenderedPdfCarriesTheDocumentsOwnPageNumbersAtTheBottomRight() throws Exception {
        byte[] pdf = service.render(html(4));
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);

        try (PDDocument document = PDDocument.load(pdf)) {
            int pages = document.getNumberOfPages();
            assertTrue(pages >= 2, "the test document must span several pages, was " + pages);

            PageNumberStripper stripper = new PageNumberStripper();
            stripper.getText(document);

            assertTrue(stripper.text.contains("Page 1 of " + pages),
                    "the document must number itself, got: " + stripper.text);
            PDPage page = document.getPage(0);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();
            assertTrue(stripper.x > width * 0.5f,
                    "the page number must be on the right half, x=" + stripper.x + " width=" + width);
            assertTrue(stripper.y > height * 0.7f,
                    "the page number must be at the bottom, y=" + stripper.y + " height=" + height);
        }
    }

    @Test
    void theDocumentCarriesNoBrowserPrintMetadata() throws Exception {
        byte[] pdf = service.render(html(2));
        try (PDDocument document = PDDocument.load(pdf)) {
            String text = PageNumberStripper.normalise(new PDFTextStripper().getText(document));
            assertFalse(text.matches("(?s).*\\d{1,2}/\\d{1,2}/\\d{2}, \\d{1,2}:\\d{2} (AM|PM).*"),
                    "no browser date/timestamp may be printed");
            assertFalse(text.contains("localhost"), "no localhost URL may be printed");
            assertFalse(text.contains("http://"), "no application URL may be printed");
            assertTrue(text.contains("Block 1 content"), "the record content itself is printed");
        }
    }

    @Test
    void aGlyphMissingFromThePrimaryFontStillRenders() throws Exception {
        assumeTrue(DocumentPdfService.FALLBACK_FONTS.stream().anyMatch(path -> new java.io.File(path).isFile()),
                "no fallback font is installed on this machine");
        // U+2713 is not in Arial; the official form's tick box uses it.
        String html = "<!doctype html><html><head><meta charset='utf-8'><title>Tick</title>"
                + "<style>body{font-family:Arial,sans-serif;font-size:12pt}</style></head>"
                + "<body><p>Tick: &#10003;</p></body></html>";
        byte[] pdf = service.render(html);
        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("✓"),
                    "the check mark must be drawn, not silently dropped, got: " + text);
        }
    }

    @Test
    void aDocumentThatWouldLoadExternalResourcesIsRejected() {
        String html = "<html><body><img src='https://example.com/x.png'></body></html>";
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.render(html));
        assertTrue(error.getMessage().contains("external"));
    }

    @Test
    void anOversizedDocumentIsRejected() {
        String html = "<html><body>" + "a".repeat(DocumentPdfService.MAX_HTML_BYTES + 1) + "</body></html>";
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.render(html));
        assertTrue(error.getMessage().contains("too large"));
    }

    @Test
    void anEmptyDocumentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.render("   "));
        assertThrows(IllegalArgumentException.class, () -> service.render(null));
    }
}
