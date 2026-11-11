package com.rpis.backend.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Turns the shared proposal document (the exact HTML/CSS the frontend builds) into
 * a PDF file.
 *
 * <p>The browser print dialog cannot be used for this: Chrome always draws its own
 * automatic header (date + document title) and footer (URL + page number) when the
 * user's "Headers and footers" option is on, regardless of the {@code @page} margin,
 * so the downloaded proposal could contain "11/11/26, 4:21 PM &lt;title&gt;". A
 * server-side render has no browser chrome at all. It also keeps the page numbering
 * the document itself declares (the {@code @bottom-right} margin box), so every
 * proposal - any title, status, proponent or role - is produced by this one renderer
 * from the one shared template.
 */
@Service
public class DocumentPdfService {

    private static final Logger log = LoggerFactory.getLogger(DocumentPdfService.class);

    /** The document is generated from stored record data; 8 MB is far above any real proposal. */
    static final int MAX_HTML_BYTES = 8 * 1024 * 1024;

    /**
     * A resource attribute or CSS url() that points at the network. The document only
     * uses inline data URIs (the seal and signatures), so an external reference is
     * either a mistake or an attempt to make the server fetch an arbitrary address.
     * Plain text URLs in the proposal's own content are not affected - only markup
     * that would actually load something.
     */
    private static final Pattern REMOTE_RESOURCE = Pattern.compile(
            "(?i)(?:\\b(?:src|href)\\s*=\\s*[\"']?\\s*(?:https?:)?//|url\\(\\s*[\"']?\\s*(?:https?:)?//)");

    private static final Pattern ACTIVE_MARKUP = Pattern.compile(
            "(?i)<\\s*(?:script|iframe|object|embed|base)\\b");

    /** Font files registered under the family names the shared stylesheet asks for. */
    record FontFace(String path, String family, boolean bold, boolean italic) {
    }

    private static final List<FontFace> FONT_CANDIDATES = List.of(
            new FontFace("C:/Windows/Fonts/arial.ttf", "Arial", false, false),
            new FontFace("C:/Windows/Fonts/arialbd.ttf", "Arial", true, false),
            new FontFace("C:/Windows/Fonts/ariali.ttf", "Arial", false, true),
            new FontFace("C:/Windows/Fonts/arialbi.ttf", "Arial", true, true),
            new FontFace("C:/Windows/Fonts/calibri.ttf", "Calibri", false, false),
            new FontFace("C:/Windows/Fonts/calibrib.ttf", "Calibri", true, false),
            new FontFace("C:/Windows/Fonts/calibrii.ttf", "Calibri", false, true),
            new FontFace("C:/Windows/Fonts/calibriz.ttf", "Calibri", true, true),
            new FontFace("/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf", "Arial", false, false),
            new FontFace("/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf", "Arial", true, false),
            new FontFace("/usr/share/fonts/truetype/liberation/LiberationSans-Italic.ttf", "Arial", false, true),
            new FontFace("/usr/share/fonts/truetype/liberation/LiberationSans-BoldItalic.ttf", "Arial", true, true),
            new FontFace("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "Arial", false, false),
            new FontFace("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "Arial", true, false),
            new FontFace("/System/Library/Fonts/Supplemental/Arial.ttf", "Arial", false, false));

    /**
     * Fonts used only when the primary family has no glyph for a character. The
     * official form's tick box uses U+2713 (&#10003;), which Arial/Calibri do not
     * contain; without a fallback that check mark would simply disappear from the
     * downloaded form. Segoe UI Symbol (Windows) and DejaVu Sans (Linux) both
     * have it.
     */
    static final List<String> FALLBACK_FONTS = List.of(
            "C:/Windows/Fonts/seguisym.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf");

    /**
     * Render a standalone HTML document to PDF bytes.
     *
     * @throws IllegalArgumentException when the input is empty, too large or asks the
     *                                  renderer to load something from the network
     */
    public byte[] render(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("The document has no content.");
        }
        if (html.getBytes(StandardCharsets.UTF_8).length > MAX_HTML_BYTES) {
            throw new IllegalArgumentException("The document is too large to generate.");
        }
        if (REMOTE_RESOURCE.matcher(html).find()) {
            throw new IllegalArgumentException("The document may not load external resources.");
        }
        if (ACTIVE_MARKUP.matcher(html).find()) {
            throw new IllegalArgumentException("The document contains unsupported markup.");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFonts(builder);
            // The document is HTML5; jsoup normalises it (entities, void elements)
            // into the W3C DOM openhtmltopdf renders.
            builder.withW3cDocument(W3CDom.convert(Jsoup.parse(html)), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception error) {
            log.error("Proposal PDF generation failed", error);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR,
                    "The document could not be generated. Please try again.");
        }
    }

    private void registerFonts(PdfRendererBuilder builder) {
        int registered = 0;
        for (FontFace face : FONT_CANDIDATES) {
            File file = new File(face.path());
            if (!file.isFile()) {
                continue;
            }
            try {
                builder.useFont(file, face.family(), face.bold() ? 700 : 400,
                        face.italic() ? PdfRendererBuilder.FontStyle.ITALIC
                                : PdfRendererBuilder.FontStyle.NORMAL,
                        true);
                registered++;
            } catch (Exception ignored) {
                // A single unreadable font must not stop the document; the next
                // candidate for the same family is still tried.
            }
        }
        if (registered == 0) {
            log.warn("No system font was found for the proposal PDF; the renderer will use its default.");
        }
        for (String path : FALLBACK_FONTS) {
            File file = new File(path);
            if (!file.isFile()) {
                continue;
            }
            try {
                builder.useFont(file, "RPIS Fallback", 400,
                        PdfRendererBuilder.FontStyle.NORMAL, true,
                        java.util.Set.of(
                                com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FSFontUseCase.FALLBACK_FINAL));
            } catch (Exception ignored) {
                // A missing fallback only affects glyphs the primary font lacks.
            }
        }
    }
}
