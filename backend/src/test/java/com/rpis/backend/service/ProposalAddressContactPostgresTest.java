package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PostgreSQL-backed test for the Create Proposal form's "Address *" and
 * "Contact *" sections: the structured parts and the selected contact must be
 * persisted in their own columns and read back unchanged, and a proposal saved
 * before the split must keep its combined address text.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProposalAddressContactPostgresTest {

    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User proponent;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        User user = new User();
        user.setName("Address Proponent");
        user.setEmail("address.contact@msunaawan.edu.ph");
        user.setPassword("secret");
        user.setRole("PROPONENT");
        user.setStatus("APPROVED");
        proponent = userRepository.saveAndFlush(user);
    }

    private static ProposalRequest structuredRequest(String contactType, String contactValue) {
        ProposalRequest request = new ProposalRequest();
        request.setAddressCountry("Philippines");
        request.setAddressRegion("Region X \u2013 Northern Mindanao");
        request.setAddressProvince("Misamis Oriental");
        request.setAddressDistrict("");
        request.setAddressMunicipality("Naawan");
        request.setAddressBarangay("Balintad");
        request.setContactType(contactType);
        request.setContactValue(contactValue);
        return request;
    }

    @Test
    void theStructuredAddressAndContactArePersistedInTheirOwnColumns() {
        Proposal proposal = new Proposal();
        proposal.setProponent(proponent);
        proposal.setProjectTitle("Structured address persistence");
        proposal.setStatus("DRAFT");
        ProposalService.applyAddressAndContact(proposal, structuredRequest("EMAIL_ADDRESS", "juan@example.com"));
        Proposal saved = proposalRepository.saveAndFlush(proposal);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT address, address_country, address_region, address_province, address_district, "
                        + "address_municipality, address_barangay, contact_type, contact_value "
                        + "FROM proposals WHERE id = ?",
                saved.getId());

        assertEquals("Philippines", row.get("address_country"));
        assertEquals("Region X \u2013 Northern Mindanao", row.get("address_region"));
        assertEquals("Misamis Oriental", row.get("address_province"));
        assertEquals("", row.get("address_district"));
        assertEquals("Naawan", row.get("address_municipality"));
        assertEquals("Balintad", row.get("address_barangay"));
        assertEquals("EMAIL_ADDRESS", row.get("contact_type"));
        assertEquals("juan@example.com", row.get("contact_value"));
        // The combined column stays usable for every existing screen.
        assertEquals(
                "Balintad, Naawan, Misamis Oriental, Region X \u2013 Northern Mindanao, Philippines / juan@example.com",
                row.get("address"));

        // And it reads back through the entity exactly as it was written.
        Proposal reloaded = proposalRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Balintad", reloaded.getAddressBarangay());
        assertEquals("Naawan", reloaded.getAddressMunicipality());
        assertEquals("EMAIL_ADDRESS", reloaded.getContactType());
        assertEquals("juan@example.com", reloaded.getContactValue());
    }

    @Test
    void aProposalSavedBeforeTheSplitKeepsItsCombinedAddress() {
        // A row written by the old single "Address / Contact" field.
        jdbcTemplate.update(
                "INSERT INTO proposals (proponent_id, project_title, status, address) VALUES (?, ?, ?, ?)",
                proponent.getId(), "Legacy address", "DRAFT", "San Miguel, Naawan / 0917 000 0000");

        Proposal legacy = proposalRepository.findAll().get(0);

        assertEquals("San Miguel, Naawan / 0917 000 0000", legacy.getAddress());
        assertNull(legacy.getAddressCountry());
        assertNull(legacy.getAddressBarangay());
        assertNull(legacy.getContactType());
    }
}
