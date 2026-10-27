package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.Proposal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the mapping of the Create Proposal form's "Address *" and
 * "Contact *" sections ({@link ProposalService#applyAddressAndContact}), which
 * replace the old combined "Address / Contact" field.
 * <p>
 * Two behaviours matter: the structured parts and the contact type/value are
 * persisted as distinct columns, and the combined {@code address} text every
 * existing screen reads is kept in step with them - without a client that
 * predates the split (the revision form) being able to wipe a saved address.
 */
class ProposalAddressContactMappingTest {

    private static ProposalRequest structuredRequest() {
        ProposalRequest request = new ProposalRequest();
        request.setAddressCountry("Philippines");
        request.setAddressRegion("Region X \u2013 Northern Mindanao");
        request.setAddressProvince("Misamis Oriental");
        request.setAddressDistrict("");
        request.setAddressMunicipality("Naawan");
        request.setAddressBarangay("Balintad");
        request.setContactType("EMAIL_ADDRESS");
        request.setContactValue("juan.delaacruz@example.com");
        return request;
    }

    @Test
    void structuredPartsAndContactAreStoredAsDistinctColumns() {
        Proposal proposal = new Proposal();

        ProposalService.applyAddressAndContact(proposal, structuredRequest());

        assertEquals("Philippines", proposal.getAddressCountry());
        assertEquals("Region X \u2013 Northern Mindanao", proposal.getAddressRegion());
        assertEquals("Misamis Oriental", proposal.getAddressProvince());
        assertEquals("", proposal.getAddressDistrict());
        assertEquals("Naawan", proposal.getAddressMunicipality());
        assertEquals("Balintad", proposal.getAddressBarangay());
        assertEquals("EMAIL_ADDRESS", proposal.getContactType());
        assertEquals("juan.delaacruz@example.com", proposal.getContactValue());
    }

    @Test
    void aRequestWithoutACountryStoresThePhilippines() {
        // The form no longer asks for a country, so the parts arrive on their own and
        // the country is filled in - the structured columns and the combined
        // "Address / Contact" text stay complete.
        ProposalRequest request = structuredRequest();
        request.setAddressCountry(null);
        Proposal proposal = new Proposal();

        ProposalService.applyAddressAndContact(proposal, request);

        assertEquals("Philippines", proposal.getAddressCountry());
        assertEquals(
                "Balintad, Naawan, Misamis Oriental, Region X \u2013 Northern Mindanao, Philippines"
                        + " / juan.delaacruz@example.com",
                proposal.getAddress());
    }

    @Test
    void theCombinedAddressKeepsTheOldFieldOrderAndAppendsTheContact() {
        Proposal proposal = new Proposal();

        ProposalService.applyAddressAndContact(proposal, structuredRequest());

        assertEquals(
                "Balintad, Naawan, Misamis Oriental, Region X \u2013 Northern Mindanao, Philippines"
                        + " / juan.delaacruz@example.com",
                proposal.getAddress());
    }

    @Test
    void aContactNumberIsCombinedTheSameWay() {
        Proposal proposal = new Proposal();
        ProposalRequest request = structuredRequest();
        request.setContactType("CONTACT_NUMBER");
        request.setContactValue("0917 123 4567");

        ProposalService.applyAddressAndContact(proposal, request);

        assertEquals("CONTACT_NUMBER", proposal.getContactType());
        assertEquals("0917 123 4567", proposal.getContactValue());
        assertEquals(
                "Balintad, Naawan, Misamis Oriental, Region X \u2013 Northern Mindanao, Philippines / 0917 123 4567",
                proposal.getAddress());
    }

    @Test
    void aClientWithoutTheSplitFieldsKeepsTheSavedAddress() {
        Proposal proposal = new Proposal();
        ProposalService.applyAddressAndContact(proposal, structuredRequest());

        // The revision form (and any other client that predates the split) sends
        // only the combined text: the structured values must survive.
        ProposalRequest legacy = new ProposalRequest();
        legacy.setAddress("Somewhere else / 0917 000 0000");
        ProposalService.applyAddressAndContact(proposal, legacy);

        assertEquals("Balintad", proposal.getAddressBarangay());
        assertEquals("Philippines", proposal.getAddressCountry());
        assertEquals("EMAIL_ADDRESS", proposal.getContactType());
        assertEquals("Somewhere else / 0917 000 0000", proposal.getAddress());
    }

    @Test
    void aNewProposalWithoutTheSplitFieldsHasNoStructuredAddress() {
        Proposal proposal = new Proposal();
        ProposalRequest legacy = new ProposalRequest();
        legacy.setAddress("Barangay San Miguel, Naawan");

        ProposalService.applyAddressAndContact(proposal, legacy);

        assertNull(proposal.getAddressCountry());
        assertNull(proposal.getContactType());
        assertEquals("Barangay San Miguel, Naawan", proposal.getAddress());
    }

    @Test
    void emptyValuesClearThePartsAndTheCombinedAddress() {
        Proposal proposal = new Proposal();
        ProposalService.applyAddressAndContact(proposal, structuredRequest());

        ProposalRequest cleared = new ProposalRequest();
        cleared.setAddressCountry("");
        cleared.setAddressRegion("");
        cleared.setAddressProvince("");
        cleared.setAddressDistrict("");
        cleared.setAddressMunicipality("");
        cleared.setAddressBarangay("");
        cleared.setContactType("");
        cleared.setContactValue("");
        ProposalService.applyAddressAndContact(proposal, cleared);

        assertEquals("", proposal.getAddressRegion());
        assertEquals("", proposal.getAddressBarangay());
        assertEquals("", proposal.getAddress());
    }

    @Test
    void valuesAreTrimmedAndCappedToTheColumnLengths() {
        Proposal proposal = new Proposal();
        ProposalRequest request = new ProposalRequest();
        request.setAddressCountry("  Philippines  ");
        request.setAddressRegion("x".repeat(200));
        request.setContactType(" CONTACT_NUMBER ");
        request.setContactValue("  " + "9".repeat(300));

        ProposalService.applyAddressAndContact(proposal, request);

        assertEquals("Philippines", proposal.getAddressCountry());
        assertEquals(ProposalService.ADDRESS_PART_MAX_LENGTH, proposal.getAddressRegion().length());
        assertEquals("CONTACT_NUMBER", proposal.getContactType());
        assertEquals(ProposalService.CONTACT_VALUE_MAX_LENGTH, proposal.getContactValue().length());
    }
}
