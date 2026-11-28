package com.rpis.backend.controller;

import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Self-registration issues the institutional address from the applicant's name, exactly as
 * the administrator's Create Account form does, and the sign-up preview endpoint must agree
 * with what the server will assign.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SelfRegistrationEmailApiTest {

    private static final String COLLEGE = "College of Fisheries and Marine Sciences (CFMS)";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
    }

    private Map<String, Object> registration(String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        // A hand-typed address must be ignored: the server owns the institutional address.
        body.put("email", "typed-by-applicant@example.com");
        body.put("password", "StrongPass1!");
        body.put("role", "PROPONENT");
        body.put("departmentOffice", COLLEGE);
        return body;
    }

    @Test
    void registrationGeneratesTheInstitutionalAddressAndKeepsTheCollege() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Juan Dela Cruz","email":"typed-by-applicant@example.com",
                                 "password":"StrongPass1!","role":"PROPONENT",
                                 "departmentOffice":"College of Fisheries and Marine Sciences (CFMS)"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("juan.delacruz@msunaawan.edu.ph"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.departmentOffice").value(COLLEGE));

        User stored = userRepository.findByNormalizedEmail("juan.delacruz@msunaawan.edu.ph")
                .orElseThrow();
        assertEquals("PENDING", stored.getStatus());
        assertEquals(COLLEGE, stored.getDepartmentOffice());
    }

    @Test
    void aSecondAccountWithTheSameNameGetsTheNextFreeAddress() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Juan Dela Cruz","password":"StrongPass1!","role":"PROPONENT",
                                 "departmentOffice":"College of Fisheries and Marine Sciences (CFMS)"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("juan.delacruz@msunaawan.edu.ph"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Juan Dela Cruz","password":"StrongPass1!","role":"PROPONENT",
                                 "departmentOffice":"College of Fisheries and Marine Sciences (CFMS)"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("juan.delacruz2@msunaawan.edu.ph"));
    }

    @Test
    void theSignUpPreviewIsPublicAndTracksTheDuplicateSuffix() throws Exception {
        mockMvc.perform(get("/api/users/registration-email").param("name", "Juan Dela Cruz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan.delacruz@msunaawan.edu.ph"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Juan Dela Cruz","password":"StrongPass1!","role":"PROPONENT",
                                 "departmentOffice":"College of Fisheries and Marine Sciences (CFMS)"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/registration-email").param("name", "Juan Dela Cruz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan.delacruz2@msunaawan.edu.ph"));
    }

    @Test
    void aBlankNameCannotProduceAnAddress() throws Exception {
        mockMvc.perform(get("/api/users/registration-email").param("name", " "))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","password":"StrongPass1!","role":"PROPONENT",
                                 "departmentOffice":"College of Fisheries and Marine Sciences (CFMS)"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
