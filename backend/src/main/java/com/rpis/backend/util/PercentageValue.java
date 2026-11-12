package com.rpis.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The server-side rule for every percentage stored in a Quarterly Progress Report.
 *
 * <p>A percentage is persisted as its numeric value only ("75", "75.5"); the "%" sign
 * is a presentation detail. Values must be numbers from 0 to 100. A value written by
 * the old form ("80%") is still accepted and normalised, so existing reports keep
 * loading; anything else is rejected with a readable message instead of being stored.</p>
 *
 * <p>The Project Workplan's AVERAGE TOTAL is derived here as well: the mean of the
 * monthly percentages that have a value. Empty months are excluded from both the sum
 * and the divisor. The client cannot send a total of its own - the service always
 * replaces it with this calculation.</p>
 *
 * <p>Mirrors {@code src/utils/percentageInput.js}.</p>
 */
public final class PercentageValue {

    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = new BigDecimal("100");

    private PercentageValue() {
    }

    /**
     * Normalises one percentage. A blank value stays blank (an empty month is not 0).
     *
     * @param raw   stored/request value, optionally still carrying a trailing "%"
     * @param label field name used in the validation message
     * @return canonical numeric text, or "" when the value is blank
     * @throws IllegalArgumentException when the value is not a number from 0 to 100
     */
    public static String normalize(String raw, String label) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.endsWith("%")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        if (value.isEmpty()) {
            return "";
        }
        BigDecimal number = parse(value, label);
        if (number.compareTo(MIN) < 0 || number.compareTo(MAX) > 0) {
            throw invalid(label);
        }
        return canonical(number);
    }

    /**
     * True when the value is blank or a valid percentage. Used where a boolean is
     * clearer than catching the exception.
     */
    public static boolean isValid(String raw) {
        try {
            normalize(raw, "Percentage");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * The AVERAGE TOTAL of one Project Workplan row. Only months with a value count;
     * an entirely empty row has no average ("" rather than 0 or NaN). The average is
     * rounded to at most two decimal places without unnecessary trailing zeros.
     */
    public static String average(List<String> months) {
        if (months == null || months.isEmpty()) {
            return "";
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (String month : months) {
            String normalized = normalize(month, "Monthly percentage");
            if (normalized.isEmpty()) {
                continue;
            }
            sum = sum.add(new BigDecimal(normalized));
            count++;
        }
        if (count == 0) {
            return "";
        }
        BigDecimal average = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        return canonical(average);
    }

    private static BigDecimal parse(String value, String label) {
        String number = value;
        if (number.startsWith(".")) {
            number = "0" + number;
        }
        if (number.endsWith(".")) {
            number = number.substring(0, number.length() - 1);
        }
        if (!number.matches("\\d+(?:\\.\\d+)?")) {
            throw invalid(label);
        }
        try {
            return new BigDecimal(number);
        } catch (NumberFormatException e) {
            throw invalid(label);
        }
    }

    /** "75.50" -> "75.5", "60.00" -> "60", "0" -> "0". */
    private static String canonical(BigDecimal number) {
        BigDecimal stripped = number.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString();
    }

    private static IllegalArgumentException invalid(String label) {
        return new IllegalArgumentException(
                (label == null || label.isBlank() ? "Percentage" : label)
                        + " must be a number from 0 to 100.");
    }
}
