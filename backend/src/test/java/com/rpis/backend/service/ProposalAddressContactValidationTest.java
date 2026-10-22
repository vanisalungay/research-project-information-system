package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Server-side validation of the Create Proposal form's "Address *" and
 * "Contact *" sections ({@link ProposalService#validateAddressAndContact}).
 * <p>
 * The form checks the same rules before it submits, but the backend must not
 * trust that: an incomplete address, a contact value without its type, an
 * unsupported type or a value that does not match the selected type must all be
 * rejected - while a client that predates the split (which sends neither
 * section) keeps working unchanged.
 */
class ProposalAddressContactValidationTest {

    private static ProposalRequest request() {
        ProposalRequest request = new ProposalRequest();
        request.setAddressCountry("Philippines");
        request.setAddressRegion("Northern Mindanao");
        request.setAddressProvince("Misamis Oriental");
        request.setAddressDistrict("");
        request.setAddressMunicipality("Naawan");
        request.setAddressBarangay("Balintad");
        request.setContactType("CONTACT_NUMBER");
        request.setContactValue("0917 123 4567");
        return request;
    }

    @Test
    void aCompleteAddressAndContactIsAccepted() {
        assertDoesNotThrow(() -> ProposalService.validateAddressAndContact(request()));
    }

    @Test
    void aRequestWithoutTheSplitSectionsIsNotAffected() {
        ProposalRequest legacy = new ProposalRequest();
        legacy.setAddress("San Miguel, Naawan / 0917 000 0000");

        assertDoesNotThrow(() -> ProposalService.validateAddressAndContact(legacy));
    }

    @Test
    void everyRequiredAddressPartIsEnforced() {
        for (String part : new String[] { "country", "region", "municipality", "barangay" }) {
            ProposalRequest request = request();
            switch (part) {
                case "country" -> request.setAddressCountry("");
                case "region" -> request.setAddressRegion("");
                case "municipality" -> request.setAddressMunicipality("");
                default -> request.setAddressBarangay("");
            }
            assertThrows(IllegalArgumentException.class,
                    () -> ProposalService.validateAddressAndContact(request),
                    "an address without its " + part + " must be rejected");
        }
    }

    @Test
    void anAddressNeedsAProvinceOrADistrict() {
        ProposalRequest missingBoth = request();
        missingBoth.setAddressProvince("");
        missingBoth.setAddressDistrict("");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(missingBoth));

        // A province-less region (NCR) is identified by its district instead.
        ProposalRequest ncr = request();
        ncr.setAddressRegion("NCR");
        ncr.setAddressProvince("");
        ncr.setAddressDistrict("First District");
        ncr.setAddressMunicipality("City of Manila");
        assertDoesNotThrow(() -> ProposalService.validateAddressAndContact(ncr));
    }

    @Test
    void theContactValueMustMatchTheSelectedType() {
        ProposalRequest numberAsEmail = request();
        numberAsEmail.setContactType("EMAIL_ADDRESS");
        numberAsEmail.setContactValue("0917 123 4567");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(numberAsEmail));

        ProposalRequest badEmail = request();
        badEmail.setContactType("EMAIL_ADDRESS");
        badEmail.setContactValue("not-an-email");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(badEmail));

        ProposalRequest goodEmail = request();
        goodEmail.setContactType("EMAIL_ADDRESS");
        goodEmail.setContactValue("juan.delaacruz@msunaawan.edu.ph");
        assertDoesNotThrow(() -> ProposalService.validateAddressAndContact(goodEmail));
    }

    @Test
    void theContactTypeAndValueMustArriveTogether() {
        ProposalRequest valueWithoutType = request();
        valueWithoutType.setContactType(null);
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(valueWithoutType));

        ProposalRequest typeWithoutValue = request();
        typeWithoutValue.setContactValue("");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(typeWithoutValue));

        ProposalRequest unsupportedType = request();
        unsupportedType.setContactType("PHONE");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(unsupportedType));
    }

    @Test
    void philippineAndInternationalNumbersAreAccepted() {
        assertTrue(ProposalService.isValidContactNumber("0917 123 4567"));
        assertTrue(ProposalService.isValidContactNumber("+63 917 123 4567"));
        assertTrue(ProposalService.isValidContactNumber("(088) 555-1234"));
        assertTrue(ProposalService.isValidContactNumber("088-555-12-34"));

        assertFalse(ProposalService.isValidContactNumber("0917ABC4567"));
        assertFalse(ProposalService.isValidContactNumber("12345"));
        assertFalse(ProposalService.isValidContactNumber("12345678901234567"));
        assertFalse(ProposalService.isValidContactNumber("63+9123456789"));
        assertFalse(ProposalService.isValidContactNumber(""));
    }

    @Test
    void anInvalidContactNumberIsRejected() {
        ProposalRequest request = request();
        request.setContactValue("call me maybe");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(request));
    }

    @Test
    void anAddressPartThatDoesNotBelongToItsParentIsRejected() {
        // Leyte is a province of Region VIII, not of Region X (Northern Mindanao).
        ProposalRequest wrongProvince = request();
        wrongProvince.setAddressProvince("Leyte");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(wrongProvince));

        // Naawan is a municipality of Misamis Oriental, not of Camiguin.
        ProposalRequest wrongProvinceForCity = request();
        wrongProvinceForCity.setAddressProvince("Camiguin");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(wrongProvinceForCity));

        // Neither does a city of another region belong to this one.
        ProposalRequest cityOfAnotherRegion = request();
        cityOfAnotherRegion.setAddressMunicipality("City of Manila");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(cityOfAnotherRegion));

        ProposalRequest unknownRegion = request();
        unknownRegion.setAddressRegion("Northern Mindano");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(unknownRegion));
    }

    @Test
    void anUnknownCountryIsRejected() {
        ProposalRequest request = request();
        request.setAddressCountry("Atlantis");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(request));
    }

    @Test
    void aForeignAddressIsTypedInAndKeepsTheProvinceOrDistrictRule() {
        ProposalRequest foreign = request();
        foreign.setAddressCountry("Japan");
        foreign.setAddressRegion("Kanto");
        foreign.setAddressProvince("Tokyo");
        foreign.setAddressMunicipality("Shinjuku");
        foreign.setAddressBarangay("Kabukicho");
        assertDoesNotThrow(() -> ProposalService.validateAddressAndContact(foreign));

        // It still has to identify its area.
        ProposalRequest withoutArea = request();
        withoutArea.setAddressCountry("Japan");
        withoutArea.setAddressRegion("Kanto");
        withoutArea.setAddressProvince("");
        withoutArea.setAddressDistrict("");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(withoutArea));
    }

    @Test
    void anIndependentCityIsCompleteWithoutAProvince() {
        // City of Isabela (Region IX) belongs to no province and no district.
        ProposalRequest isabela = request();
        isabela.setAddressRegion("Zamboanga Peninsula");
        isabela.setAddressProvince("");
        isabela.setAddressDistrict("");
        isabela.setAddressMunicipality("City of Isabela");
        isabela.setAddressBarangay("Balintad");
        assertDoesNotThrow(() -> ProposalService.validateAddressAndContact(isabela));

        // A municipality of one of the region's provinces is not.
        ProposalRequest dipolog = request();
        dipolog.setAddressRegion("Zamboanga Peninsula");
        dipolog.setAddressProvince("");
        dipolog.setAddressDistrict("");
        dipolog.setAddressMunicipality("City of Dipolog");
        assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateAddressAndContact(dipolog));
    }
}
