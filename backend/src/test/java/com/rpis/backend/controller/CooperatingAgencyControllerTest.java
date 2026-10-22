package com.rpis.backend.controller;

import com.rpis.backend.model.CooperatingAgency;
import com.rpis.backend.service.CooperatingAgencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level test of the cooperating agency directory endpoints
 * ({@code GET/POST /api/cooperating-agencies}): the checklist data is returned as
 * JSON, a new agency answers 201, a duplicate answers 409 with a message the form
 * can show as-is, and a blank name answers 400. Authentication itself is enforced by
 * {@code SecurityConfig} (both routes require an authenticated caller).
 */
class CooperatingAgencyControllerTest {

    private CooperatingAgencyService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(CooperatingAgencyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CooperatingAgencyController(service)).build();
    }

    private static byte[] body(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void theDirectoryIsReturnedAsAJsonList() throws Exception {
        when(service.getAllAgencies()).thenReturn(List.of(
                new CooperatingAgency(1L, "Commission on Higher Education (CHED)", null),
                new CooperatingAgency(2L, "Department of Agriculture (DA)", null)));

        mockMvc.perform(get("/api/cooperating-agencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Commission on Higher Education (CHED)"))
                .andExpect(jsonPath("$[1].name").value("Department of Agriculture (DA)"));
    }

    @Test
    void aNewAgencyIsCreated() throws Exception {
        when(service.addAgency("Bureau of Fisheries and Aquatic Resources (BFAR)"))
                .thenReturn(new CooperatingAgency(9L, "Bureau of Fisheries and Aquatic Resources (BFAR)", null));

        mockMvc.perform(post("/api/cooperating-agencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"Bureau of Fisheries and Aquatic Resources (BFAR)\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.name").value("Bureau of Fisheries and Aquatic Resources (BFAR)"));
    }

    @Test
    void aDuplicateNameIsReportedAsAConflict() throws Exception {
        when(service.addAgency(any())).thenThrow(new IllegalStateException(
                "Department of Science and Technology (DOST) is already in the cooperating agencies list."));

        mockMvc.perform(post("/api/cooperating-agencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"DOST\"}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Department of Science and Technology (DOST) is already in the cooperating agencies list."));
    }

    @Test
    void aBlankNameIsReportedAsABadRequest() throws Exception {
        when(service.addAgency(any())).thenThrow(new IllegalArgumentException("Agency name is required."));

        mockMvc.perform(post("/api/cooperating-agencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"  \"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Agency name is required."));
    }
}
