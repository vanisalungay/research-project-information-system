package com.rpis.backend.controller;

import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The one HTTP entry point every proposal Download uses. It must be authenticated,
 * return a real PDF file and never involve the browser's print dialog - which is
 * what removes the browser's automatic date/title header and URL/page-number
 * footer from downloaded proposals.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentPdfApiTest {

    private static final String HTML = "<!doctype html><html><head><meta charset='utf-8'>"
            + "<title>Proposal Details</title><style>"
            + "@page { size: A4; margin: 9mm 13mm 9mm;"
            + " @bottom-right { content: \"Page \" counter(page) \" of \" counter(pages); font-size: 8pt; } }"
            + "body{font-family:Arial,sans-serif;font-size:10pt}"
            + "</style></head><body><h1>JUL REJECT BY REC</h1><p>Record content</p></body></html>";

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;

    private String token;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("PDF Downloader");
        user.setEmail("pdf.download." + System.nanoTime() + "@msunaawan.edu.ph");
        user.setPassword("secret");
        user.setRole("PROPONENT");
        user.setStatus("APPROVED");
        user = userRepository.saveAndFlush(user);
        token = tokenService.generateToken(user.getId(), "PROPONENT");
    }

    @Test
    void anAuthenticatedDownloadReturnsTheRenderedPdfFile() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/documents/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"html\":" + quote(HTML) + "}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertTrue(pdf.length > 500, "a rendered PDF must be returned");
        assertEquals("%PDF", new String(pdf, 0, 4));
    }

    @Test
    void anUnauthenticatedCallerCannotRenderDocuments() throws Exception {
        mockMvc.perform(post("/api/documents/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"html\":" + quote(HTML) + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aDocumentThatWouldLoadExternalResourcesIsRejectedWith400() throws Exception {
        String unsafe = "<html><body><img src='https://example.com/x.png'></body></html>";
        mockMvc.perform(post("/api/documents/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"html\":" + quote(unsafe) + "}"))
                .andExpect(status().isBadRequest());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
