package com.rpis.backend.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.rpis.backend.model.EndorsementLetterSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders the official MSU-Naawan endorsement letter as a PDF.
 *
 * <h2>Why the letterhead is an image</h2>
 * The official template (<i>MSUN Letterhead for A4</i>) carries its heading - the
 * university seal, "Republic of the Philippines", the "MINDANAO STATE UNIVERSITY AT
 * NAAWAN" wordmark and the navy-blue / yellow-gold divider - and its footer as
 * artwork. The RPIS therefore ships that official A4 letterhead image and draws the
 * <em>same two bands the official template draws</em> (its header and footer are
 * crops of the one image, taken with the exact crop rectangle the template uses):
 * the heading band is rows 34-394 of the 1768x2500 px letterhead, the footer band is
 * rows 2299-2490. Both bands are drawn full page width at the template's own
 * position and size, so the seal, the divider and the footer are the official
 * artwork, at ~213 dpi, never redrawn, stretched or substituted.
 *
 * <h2>Page geometry</h2>
 * A4 with the template's 1 inch margins, the heading band at the top, the footer band
 * at the bottom. The letter body flows between them and never overlaps either: the
 * footer is placed before the body is laid out, and the body frame ends above it.
 *
 * <h2>Typography</h2>
 * The official body style is Calibri 12 pt. Calibri is not redistributable and is not
 * part of the project, so the letter is rendered with the built-in Helvetica 12 pt
 * unless a font file is configured through {@code rpis.endorsement.font-path}, in
 * which case that font is used instead. This is the one deliberate typographic
 * difference from the DOCX template and it is reported as such.
 */
@Slf4j
@Service
public class EndorsementLetterService {

    /** Official letterhead asset, extracted from the university's own template. */
    static final String LETTERHEAD_RESOURCE = "templates/msun-letterhead-a4.png";

    private static final float PAGE_WIDTH = PageSize.A4.getWidth();
    private static final float PAGE_HEIGHT = PageSize.A4.getHeight();

    /** 1440 twips = 1 inch, the template's margin on all four sides. */
    static final float MARGIN = 72f;

    /** Body text size of the official template's "Normal (Web)" style. */
    static final float BODY_FONT_SIZE = 12f;

    /** Single line spacing of the official template, in points. */
    static final float LINE_HEIGHT = 14f;

    // ---- Letterhead source image and the template's crop rectangles -------------
    static final int LETTERHEAD_PIXELS_WIDE = 1768;
    static final int LETTERHEAD_PIXELS_TALL = 2500;
    /** Header crop: srcRect t=1.381% b=84.235%. */
    static final int HEADER_CROP_TOP = 34;
    static final int HEADER_CROP_HEIGHT = 360;
    /** Footer crop: srcRect t=91.985% b=0.391%. */
    static final int FOOTER_CROP_TOP = 2299;
    static final int FOOTER_CROP_HEIGHT = 191;

    // ---- Placement of the two bands, in points, as the template anchors them ----
    /** Header: cx 7571105 EMU x cy 1541145 EMU at -904240 / -274320 EMU. */
    static final float HEADER_LEFT = 0.8f;
    static final float HEADER_TOP = 14.4f;
    static final float HEADER_WIDTH = 596.15f;
    static final float HEADER_HEIGHT = 121.35f;
    /** Footer: same width, 816610 EMU tall, anchored at -910590 / -379095 EMU. */
    static final float FOOTER_LEFT = 0.3f;
    static final float FOOTER_BOTTOM = 15.35f;
    static final float FOOTER_WIDTH = 596.15f;
    static final float FOOTER_HEIGHT = 64.3f;

    // ---- Document Code block (template: red Arial 6 pt in a white box) ----------
    static final float DOC_CODE_LEFT = 367.1f;
    static final float DOC_CODE_TOP = 135.35f;
    static final float DOC_CODE_WIDTH = 216f;
    static final float DOC_CODE_HEIGHT = 111.8f;
    private static final Color DOC_CODE_RED = new Color(0xC0, 0x00, 0x00);

    // ---- Signature block --------------------------------------------------------
    private static final float SIGNATURE_MAX_WIDTH = 160f;
    private static final float SIGNATURE_MAX_HEIGHT = 62f;
    /** Closing line + signature image + name + designation, kept together. */
    private static final float SIGNATURE_BLOCK_HEIGHT = 132f;

    private static final DateTimeFormatter LETTER_DATE =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private final String configuredFontPath;
    private final byte[] letterheadBytes;

    public EndorsementLetterService(
            @Value("${rpis.endorsement.font-path:}") String configuredFontPath) {
        this.configuredFontPath = configuredFontPath;
        this.letterheadBytes = readLetterhead();
    }

    private static byte[] readLetterhead() {
        try (InputStream in = new ClassPathResource(LETTERHEAD_RESOURCE).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "The official MSUN letterhead (" + LETTERHEAD_RESOURCE + ") is missing from the "
                            + "application resources, so the endorsement letter cannot be rendered.",
                    e);
        }
    }

    /** Everything the letter is populated from. */
    public record LetterContext(
            EndorsementLetterSettings settings,
            String documentCode,
            String college,
            LocalDate endorsementDate,
            String proposalTitle,
            String proponentName,
            String department,
            String deanName,
            String deanDesignation,
            byte[] signatureImage) {
    }

    /**
     * Renders the letter. The returned bytes are the complete, printable PDF that is
     * stored and then served to the Proponent, so the previewed document and the
     * downloaded one are the same file.
     */
    public GeneratedLetter generate(LetterContext context) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // The whole page is laid out explicitly, so the document carries no margins of
        // its own: the template's margins are applied to the body frame.
        Document document = new Document(PageSize.A4, 0f, 0f, 0f, 0f);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.addTitle("Endorsement of Internally Funded Project Proposal");
            document.addSubject("Official endorsement letter issued through the RPIS");
            document.addKeywords("MSU-Naawan, endorsement letter, " + context.documentCode()
                    + ", electronic signature image (not a cryptographic digital signature)");
            document.addCreator("Research Project Information System - MSU Naawan");
            document.open();

            Font body = bodyFont(Font.NORMAL);
            Font bold = bodyFont(Font.BOLD);
            PdfContentByte canvas = writer.getDirectContent();

            drawLetterhead(canvas);
            drawDocumentCode(canvas, context.documentCode());

            float firstTop = bodyTop();
            float bottom = bodyBottom();

            ColumnText column = new ColumnText(canvas);
            for (Paragraph paragraph : bodyParagraphs(context, body, bold)) {
                column.addElement(paragraph);
            }
            float yAfterBody = flowBody(document, writer, column, firstTop, bottom);
            drawSignatureBlock(document, writer, context, yAfterBody, bottom, body, bold);

            document.close();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "The official endorsement letter could not be generated: " + e.getMessage(), e);
        }
        return new GeneratedLetter(out.toByteArray(), context.documentCode());
    }

    /** Top of the body frame: just below the heading band. */
    private static float bodyTop() {
        return PAGE_HEIGHT - (HEADER_TOP + HEADER_HEIGHT) - 12f;
    }

    /** Bottom of the body frame: just above the footer band. */
    private static float bodyBottom() {
        return FOOTER_BOTTOM + FOOTER_HEIGHT + 12f;
    }

    /**
     * Flows the letter body between the two bands, repeating the letterhead on a
     * continuation page if a very long title or college name ever needs one (the
     * text wraps instead of being clipped). Returns the y position where the body
     * ended, so the closing and the signature are placed directly under it.
     */
    private float flowBody(Document document, PdfWriter writer, ColumnText column,
            float firstTop, float bottom) throws Exception {
        float top = firstTop;
        int status = ColumnText.START_COLUMN;
        while (true) {
            column.setSimpleColumn(MARGIN, bottom, PAGE_WIDTH - MARGIN, top);
            status = column.go();
            if (!ColumnText.hasMoreText(status)) {
                break;
            }
            document.newPage();
            drawLetterhead(writer.getDirectContent());
            top = bodyTop();
        }
        return column.getYLine();
    }

    /** The rendered PDF. */
    public record GeneratedLetter(byte[] content, String documentCode) {
    }

    /**
     * Draws the two official letterhead bands: the heading at the top of the page and
     * the footer at the bottom, both full page width at the template's own position
     * and size. The seal and the divider are never stretched or substituted - the
     * bands keep the letterhead's own aspect ratio, because their crop rectangles
     * have exactly the aspect ratio of the boxes they are drawn into.
     */
    private void drawLetterhead(PdfContentByte canvas) throws Exception {
        Image header = toPdfImage(crop(letterhead(), HEADER_CROP_TOP, HEADER_CROP_HEIGHT));
        header.scaleAbsolute(HEADER_WIDTH, HEADER_HEIGHT);
        header.setAbsolutePosition(HEADER_LEFT, PAGE_HEIGHT - HEADER_TOP - HEADER_HEIGHT);
        canvas.addImage(header);

        Image footer = toPdfImage(crop(letterhead(), FOOTER_CROP_TOP, FOOTER_CROP_HEIGHT));
        footer.scaleAbsolute(FOOTER_WIDTH, FOOTER_HEIGHT);
        footer.setAbsolutePosition(FOOTER_LEFT, FOOTER_BOTTOM);
        canvas.addImage(footer);
    }

    /** The official letterhead, decoded once. */
    private BufferedImage letterhead() throws IOException {
        return ImageIO.read(new ByteArrayInputStream(letterheadBytes));
    }

    /** One horizontal band of the letterhead, full width. */
    private static BufferedImage crop(BufferedImage source, int top, int height) {
        int safeTop = Math.max(0, Math.min(top, source.getHeight() - 1));
        int safeHeight = Math.max(1, Math.min(height, source.getHeight() - safeTop));
        return source.getSubimage(0, safeTop, source.getWidth(), safeHeight);
    }

    /**
     * Turns a band into a PDF image. The bands are composited on white, which is what
     * the letterhead itself is, so a transparent pixel can never become a black one.
     */
    private static Image toPdfImage(BufferedImage band) throws Exception {
        return Image.getInstance(band, Color.WHITE);
    }

    /**
     * The "Document Code" block: a white box with the red label, at the exact
     * position, size and colour the official template uses (red Arial 6 pt, upper
     * right of the body header). The generated document code is printed under the
     * label so the issued letter carries its own reference. The box is drawn before
     * the body text, exactly as the template anchors it behind the text.
     */
    private void drawDocumentCode(PdfContentByte canvas, String documentCode) {
        float x = DOC_CODE_LEFT;
        float top = PAGE_HEIGHT - DOC_CODE_TOP;
        float bottom = top - DOC_CODE_HEIGHT;

        canvas.saveState();
        canvas.setColorFill(Color.WHITE);
        canvas.setColorStroke(new Color(0xBF, 0xBF, 0xBF));
        canvas.setLineWidth(0.75f);
        canvas.rectangle(x, bottom, DOC_CODE_WIDTH, DOC_CODE_HEIGHT);
        canvas.fillStroke();

        Font label = FontFactory.getFont(FontFactory.HELVETICA, 6f, Font.NORMAL, DOC_CODE_RED);
        float centerX = x + DOC_CODE_WIDTH / 2f;
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                new Paragraph("Document Code", label), centerX, top - 50f, 0);
        if (documentCode != null && !documentCode.isBlank()) {
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Paragraph(documentCode, label), centerX, top - 60f, 0);
        }
        canvas.restoreState();
    }

    /**
     * The letter body, in the order and with the wording of the official template.
     * Only the placeholders are populated, and they are populated from the saved
     * records: the proposal title and the faculty name from the endorsement request,
     * the college/department from the request, the date from the endorsement date,
     * and the Director, the office and the university from the administrative
     * settings.
     */
    private List<Paragraph> bodyParagraphs(LetterContext context, Font body, Font bold) {
        EndorsementLetterSettings settings = context.settings();
        List<Paragraph> paragraphs = new ArrayList<>();

        paragraphs.add(line(collegeLine(context.college()), body));
        paragraphs.add(spacer());
        paragraphs.add(line("Date: " + formatDate(context.endorsementDate()), body));
        paragraphs.add(spacer());

        // Addressee block: the ORPS Director, in bold, then the office and the
        // university, exactly as the template prints them.
        paragraphs.add(line(directorFullName(settings), bold));
        paragraphs.add(line(settings.getDirectorDesignation(), body));
        paragraphs.add(line(settings.getOfficeName(), body));
        paragraphs.add(line(settings.getUniversityName(), body));
        paragraphs.add(spacer());

        paragraphs.add(line("Subject: Endorsement of Internally Funded Project Proposal", bold));
        paragraphs.add(spacer());
        paragraphs.add(line("Dear Director " + settings.getDirectorSurname() + ":", body));
        paragraphs.add(spacer());

        paragraphs.add(submissionParagraph(context, body, bold));
        paragraphs.add(line("The proposal has been reviewed and is hereby endorsed for consideration "
                + "in accordance with the university\u2019s research policies, guidelines, and "
                + "procedures.", body));
        paragraphs.add(line("Thank you for your consideration and support of the College\u2019s "
                + "research initiatives.", body));
        return paragraphs;
    }

    /**
     * The opening paragraph, with the proposal title and the faculty name in bold, as
     * the official letter prints them.
     */
    private Paragraph submissionParagraph(LetterContext context, Font body, Font bold) {
        Paragraph paragraph = new Paragraph(14f);
        paragraph.add(new Chunk("Respectfully endorsed for your review and appropriate action is the "
                + "Internally Funded Project Proposal (IFP) entitled \u201c", body));
        paragraph.add(new Chunk(blankToEmpty(context.proposalTitle()), bold));
        paragraph.add(new Chunk("\u201d, submitted by ", body));
        paragraph.add(new Chunk(blankToEmpty(context.proponentName()), bold));
        paragraph.add(new Chunk(" of the " + unitOf(context) + ".", body));
        paragraph.setSpacingAfter(10f);
        return paragraph;
    }

    /** The unit the proposal belongs to: its department when given, else its college. */
    private static String unitOf(LetterContext context) {
        String department = context.department();
        if (department != null && !department.isBlank()) {
            return department.trim();
        }
        return blankToEmpty(context.college());
    }

    /**
     * The "College of ..." line of the official template.
     *
     * <p>The template prints "College of [College Name]", but the RPIS college values
     * already name the unit in full ("College of Fisheries and Marine Sciences (CFMS)",
     * "Integrated Development School (IDS)"). Prefixing unconditionally would print
     * "College of College of Fisheries...", so the prefix is only added when the value
     * does not already name a unit.
     */
    private static String collegeLine(String college) {
        String value = blankToEmpty(college);
        if (value.isEmpty()) {
            return "College of ";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String unit : UNIT_WORDS) {
            if (lower.startsWith(unit)) {
                return value;
            }
        }
        return "College of " + value;
    }

    /** Words that already name an organizational unit in the RPIS college values. */
    private static final List<String> UNIT_WORDS = List.of(
            "college", "school", "institute", "department", "integrated", "faculty", "campus");

    private static String directorFullName(EndorsementLetterSettings settings) {
        String name = settings.getDirectorName() == null ? "" : settings.getDirectorName().trim();
        String surname = settings.getDirectorSurname() == null
                ? ""
                : settings.getDirectorSurname().trim();
        String full = (name + " " + surname).trim();
        return full.isEmpty() ? "\u2014" : full;
    }

    /**
     * Draws the closing and the signature block. The closing line, the Dean's
     * authorized signature image, the Dean's name and the designation are kept
     * together: when the body ends too close to the footer band the whole block moves
     * to a continuation page instead of being split or pushed under the footer.
     */
    private void drawSignatureBlock(Document document, PdfWriter writer, LetterContext context,
            float yAfterBody, float bottom, Font body, Font bold) throws Exception {
        float y = yAfterBody;
        if (y - SIGNATURE_BLOCK_HEIGHT < bottom) {
            document.newPage();
            drawLetterhead(writer.getDirectContent());
            y = bodyTop();
        }

        PdfContentByte canvas = writer.getDirectContent();
        float closingY = y - 18f;
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                new Paragraph("Respectfully yours,", body), MARGIN, closingY, 0);

        float imageTop = closingY - 12f;
        float imageHeight = 0f;
        byte[] signature = context.signatureImage();
        if (signature != null && signature.length > 0) {
            Image image = Image.getInstance(signature);
            image.scaleToFit(SIGNATURE_MAX_WIDTH, SIGNATURE_MAX_HEIGHT);
            imageHeight = image.getScaledHeight();
            image.setAbsolutePosition(MARGIN, imageTop - imageHeight);
            canvas.addImage(image);
        }

        float nameY = imageTop - imageHeight - 14f;
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                new Paragraph(blankToEmpty(context.deanName()), bold), MARGIN, nameY, 0);
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                new Paragraph("Dean, " + blankToEmpty(context.college()), body), MARGIN, nameY - 14f, 0);
    }

    /**
     * The letter body font: the configured font file when one is available, otherwise
     * the built-in Helvetica (see the class comment).
     */
    private Font bodyFont(int style) {
        if (configuredFontPath != null && !configuredFontPath.isBlank()) {
            try {
                BaseFont base = BaseFont.createFont(configuredFontPath, BaseFont.WINANSI,
                        BaseFont.EMBEDDED);
                return new Font(base, BODY_FONT_SIZE, style);
            } catch (Exception e) {
                log.warn("The configured endorsement letter font '{}' could not be loaded ({}); "
                        + "falling back to the built-in Helvetica.", configuredFontPath,
                        e.getMessage());
            }
        }
        return FontFactory.getFont(
                style == Font.BOLD ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA,
                BODY_FONT_SIZE);
    }

    /** One line of the letter, at the template's single line spacing. */
    private static Paragraph line(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setLeading(LINE_HEIGHT);
        return paragraph;
    }

    /** The blank line the official template leaves between its blocks. */
    private static Paragraph spacer() {
        Paragraph paragraph = new Paragraph(" ",
                FontFactory.getFont(FontFactory.HELVETICA, BODY_FONT_SIZE));
        paragraph.setLeading(10f);
        return paragraph;
    }

    private static String formatDate(LocalDate date) {
        return LETTER_DATE.format(date == null ? LocalDate.now() : date);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
