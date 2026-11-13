package com.rpis.backend.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rules for the percentage values stored by a Quarterly Progress Report and for the
 * automatically derived Project Workplan AVERAGE TOTAL.
 */
class PercentageValueTest {

    @Test
    void aValueIsStoredWithoutThePercentageSign() {
        assertEquals("75", PercentageValue.normalize("75", "Percentage"));
        assertEquals("75.5", PercentageValue.normalize("75.50", "Percentage"));
        assertEquals("80", PercentageValue.normalize("80%", "Percentage"),
                "a legacy value is normalised, never duplicated");
        assertEquals("0", PercentageValue.normalize("0", "Percentage"));
        assertEquals("100", PercentageValue.normalize("100", "Percentage"));
    }

    @Test
    void anEmptyValueStaysEmptyAndIsNotZero() {
        assertEquals("", PercentageValue.normalize("", "Percentage"));
        assertEquals("", PercentageValue.normalize("   ", "Percentage"));
        assertEquals("", PercentageValue.normalize(null, "Percentage"));
    }

    @Test
    void alphabeticAndOutOfRangeValuesAreRejected() {
        for (String value : List.of("abc", "50abc", "NaN", "Infinity", "-1", "100.01", "101")) {
            assertFalse(PercentageValue.isValid(value), value);
            assertThrows(IllegalArgumentException.class,
                    () -> PercentageValue.normalize(value, "Percentage"), value);
        }
    }

    @Test
    void averagesOnlyTheMonthsThatWereEntered() {
        assertEquals("60", PercentageValue.average(Arrays.asList("50", "70", "", "", "", "", "", "", "", "", "", "")));
        assertEquals("80", PercentageValue.average(Arrays.asList("80", "60", "100")));
        assertEquals("50", PercentageValue.average(Arrays.asList("40", "", "60")));
        assertEquals("100", PercentageValue.average(Arrays.asList("100", "100", "100")));
        assertEquals("50.33", PercentageValue.average(Arrays.asList("50", "50", "51")));
        assertEquals("0", PercentageValue.average(Arrays.asList("0", "")),
                "a real zero is a value, unlike an empty month");
    }

    @Test
    void anEmptyWorkplanRowHasNoAverage() {
        assertEquals("", PercentageValue.average(Collections.emptyList()));
        assertEquals("", PercentageValue.average(null));
        assertEquals("", PercentageValue.average(Arrays.asList("", "", "")));
    }
}
