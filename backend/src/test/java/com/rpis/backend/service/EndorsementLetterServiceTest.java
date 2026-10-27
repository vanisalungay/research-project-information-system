package com.rpis.backend.service;

import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.rpis.backend.model.EndorsementLetterSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the generated official endorsement letter.
 *
 * <p>No database and no Spring context are needed: the letter service is a pure
 * renderer. The tests assert what can be asserted about a PDF - that it is a real A4
 * PDF, that it carries the two official letterhead bands and the signature as images,
 * that every dynamic field is present in the extracted text, and that the
 * header/footer geometry cannot overlap the letter body.
 *
 * <p>The one thing these tests cannot do is look at the page, so the visual
 * comparison with the reference is reported separately as a limitation.
 */
class EndorsementLetterServiceTest {

    private static final String COLLEGE = "College of Fisheries and Marine Sciences (CFMS)";
    private static final String TITLE = "Seaweed-Based Biostimulant for Mangrove Rehabilitation";
    private static final String PROPONENT = "Dr. Maria Lourdes Bautista";
    private static final String DEAN = "Dr. Ramon Villanueva";
    private static final String CODE = "MSUN-ORPS-END-007-2026";

    private EndorsementLetterService service;
    private EndorsementLetterSettings settings;

    @BeforeEach
    void setUp() {
        // No configured font file: the service uses its built-in Helvetica fallback.
        service = new EndorsementLetterService("");
        settings = new EndorsementLetterSettings();
        settings.setDirectorName("Dr. Eduardo");
        settings.setDirectorSurname("Salazar");
        settings.setDirectorDesignation("Director");
        settings.setOfficeName("Office of Research and Publication Services (ORPS)");
        settings.setUniversityName("Mindanao State University at Naawan");
    }

    private EndorsementLetterService.LetterContext context(String title) {
        return new EndorsementLetterService.LetterContext(settings, CODE, COLLEGE,
                LocalDate.of(2026, 10, 27), title, PROPONENT, null, DEAN, "Dean",
                signatureImage());
    }

    /** A small real PNG so the signature is embedded exactly like a stored one. */
    private byte[] signatureImage() {
        BufferedImage image = new BufferedImage(240, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 240, 80);
        graphics.setColor(Color.BLACK);
        graphics.drawLine(20, 60, 200, 25);
        graphics.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String textOf(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(new PdfTextExtractor(reader).getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * The page text with every run of whitespace collapsed. A phrase the renderer
     * wrapped onto the next visual line comes back from the extractor with a line
     * break inside it, so the assertions match against this normalized form.
     */
    private String normalizedTextOf(byte[] pdf) throws Exception {
        return textOf(pdf).replaceAll("\\s+", " ").trim();
    }

    /** How many image XObjects the given page carries. */
    private int embeddedImages(byte[] pdf, int page) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfDictionary resources = reader.getPageN(page).getAsDict(PdfName.RESOURCES);
            PdfDictionary xobjects = resources == null ? null
                    : resources.getAsDict(PdfName.XOBJECT);
            if (xobjects == null) {
                return 0;
            }
            int images = 0;
            for (PdfName key : xobjects.getKeys()) {
                // The entries are indirect references, so they must be resolved before
                // their subtype can be read.
                PdfObject resolved = PdfReader.getPdfObject(xobjects.get(key));
                if (resolved instanceof PdfDictionary object
                        && PdfName.IMAGE.equals(object.getAsName(PdfName.SUBTYPE))) {
                    images++;
                }
            }
            return images;
        } finally {
            reader.close();
        }
    }

    @Test
    void generatesARealA4Pdf() throws Exception {
        EndorsementLetterService.GeneratedLetter letter = service.generate(context(TITLE));
        byte[] pdf = letter.content();

        assertTrue(pdf.length > 4, "the generated document is empty");
        assertEquals("%PDF", new String(pdf, 0, 4), "the generated document is not a PDF");
        assertEquals(CODE, letter.documentCode());

        PdfReader reader = new PdfReader(pdf);
        try {
            assertEquals(1, reader.getNumberOfPages(), "the letter must fit on one page");
            assertEquals(595.28f, reader.getPageSize(1).getWidth(), 0.5f);
            assertEquals(841.89f, reader.getPageSize(1).getHeight(), 0.5f);
        } finally {
            reader.close();
        }
    }

    @Test
    void carriesTheOfficialLetterheadBandsAndTheSignatureAsImages() throws Exception {
        byte[] pdf = service.generate(context(TITLE)).content();

        // The heading band, the footer band and the Dean's signature image.
        assertEquals(3, embeddedImages(pdf, 1),
                "the letter must embed the official heading band, the footer band and the signature");
    }

    @Test
    void theHeaderAndFooterBandsCannotOverlapTheBody() {
        float headerBottom = EndorsementLetterService.HEADER_TOP
                + EndorsementLetterService.HEADER_HEIGHT;
        float footerTop = EndorsementLetterService.FOOTER_BOTTOM
                + EndorsementLetterService.FOOTER_HEIGHT;

        assertTrue(EndorsementLetterService.HEADER_HEIGHT > 0f,
                "the heading band must have a positive height");
        assertTrue(footerTop < headerBottom,
                "the footer band must sit below the heading band, never over it");
        // The body frame is inset from both bands, so no line of text can be printed
        // over the seal, the divider or the footer.
        assertTrue(footerTop + 12f <= headerBottom - 12f,
                "the body frame must leave room between the two bands");
    }

    @Test
    void everyDynamicFieldIsPrinted() throws Exception {
        String text = normalizedTextOf(service.generate(context(TITLE)).content());

        assertTrue(text.contains(COLLEGE), text);
        assertTrue(text.contains("Date: October 27, 2026"), text);
        assertTrue(text.contains("Dr. Eduardo Salazar"), text);
        assertTrue(text.contains("Office of Research and Publication Services (ORPS)"), text);
        assertTrue(text.contains("Mindanao State University at Naawan"), text);
        assertTrue(text.contains("Subject: Endorsement of Internally Funded Project Proposal"), text);
        assertTrue(text.contains("Dear Director Salazar:"), text);
        assertTrue(text.contains(TITLE), text);
        assertTrue(text.contains(PROPONENT), text);
        assertTrue(text.contains(COLLEGE + "."), text);
        assertTrue(text.contains("Respectfully yours,"), text);
        assertTrue(text.contains(DEAN), text);
        assertTrue(text.contains("Dean, " + COLLEGE), text);
    }

    @Test
    void theOfficialBodyWordingIsPreserved() throws Exception {
        String text = normalizedTextOf(service.generate(context(TITLE)).content());

        assertTrue(text.contains("Respectfully endorsed for your review and appropriate action is the"),
                text);
        assertTrue(text.contains("Internally Funded Project Proposal (IFP) entitled"), text);
        assertTrue(text.contains("submitted by"), text);
        assertTrue(text.contains("The proposal has been reviewed and is hereby endorsed for "
                + "consideration in accordance with the university"), text);
        assertTrue(text.contains("research policies, guidelines, and procedures."), text);
        assertTrue(text.contains("Thank you for your consideration and support of the College"), text);
        assertTrue(text.contains("research initiatives."), text);
    }

    @Test
    void theDocumentCodeBlockCarriesTheGeneratedReference() throws Exception {
        String text = normalizedTextOf(service.generate(context(TITLE)).content());

        assertTrue(text.contains("Document Code"), text);
        assertTrue(text.contains(CODE), text);
    }

    @Test
    void aVeryLongTitleWrapsInsteadOfBeingClipped() throws Exception {
        String longTitle = "Assessment of the Socio-Economic Impact of Community-Based Mangrove "
                + "Ecosystem Restoration and Sustainable Aquaculture Livelihood Programs in the "
                + "Coastal Barangays of Misamis Oriental, Northern Mindanao, Philippines";

        byte[] pdf = service.generate(context(longTitle)).content();
        String text = normalizedTextOf(pdf);

        // The whole title is printed, and the letter still fits on the official single
        // page with its closing and signature.
        assertTrue(text.contains("Assessment of the Socio-Economic Impact"), text);
        assertTrue(text.contains("Northern Mindanao, Philippines"), text);
        assertTrue(text.contains("Respectfully yours,"), text);
        assertTrue(text.contains(DEAN), text);

        PdfReader reader = new PdfReader(pdf);
        try {
            assertEquals(1, reader.getNumberOfPages(), "a long title must still fit one page");
        } finally {
            reader.close();
        }
    }

    @Test
    void aDepartmentIsPrintedWhenItIsGiven() throws Exception {
        EndorsementLetterService.LetterContext withDepartment =
                new EndorsementLetterService.LetterContext(settings, CODE, COLLEGE,
                        LocalDate.of(2026, 10, 27), TITLE, PROPONENT,
                        "Department of Marine Biology", DEAN, "Dean", signatureImage());

        String text = normalizedTextOf(service.generate(withDepartment).content());

        assertTrue(text.contains("of the Department of Marine Biology."), text);
    }

    @Test
    void theLetterheadAssetIsTheOfficialOneAndHasTheExpectedShape() throws Exception {
        BufferedImage letterhead = ImageIO.read(new ClassPathResource(
                EndorsementLetterService.LETTERHEAD_RESOURCE).getInputStream());

        assertNotNull(letterhead, "the official letterhead asset must be present");
        assertEquals(EndorsementLetterService.LETTERHEAD_PIXELS_WIDE, letterhead.getWidth());
        assertEquals(EndorsementLetterService.LETTERHEAD_PIXELS_TALL, letterhead.getHeight());

        // The two crops must stay inside the asset.
        assertTrue(EndorsementLetterService.HEADER_CROP_TOP
                + EndorsementLetterService.HEADER_CROP_HEIGHT <= letterhead.getHeight());
        assertTrue(EndorsementLetterService.FOOTER_CROP_TOP
                + EndorsementLetterService.FOOTER_CROP_HEIGHT <= letterhead.getHeight());
    }
}
