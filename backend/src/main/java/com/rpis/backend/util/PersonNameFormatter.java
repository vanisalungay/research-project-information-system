package com.rpis.backend.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The server-side half of the one person-name format RPIS uses.
 *
 * <p>The standard order is <em>Last Name, First Name, Middle Initial (optional),
 * Suffix (optional)</em>, for example {@code Dela Cruz, Juan, M., Jr.}. Every name
 * entry is converted to title case: an all-uppercase, all-lowercase or mixed-case
 * value is stored the same way. Commas, periods, spaces, hyphens and apostrophes are
 * preserved; only the letters are re-cased. Roman-numeral suffixes ({@code II},
 * {@code III}, {@code IV}) stay upper case, so {@code JR.} becomes {@code Jr.} while
 * {@code III} stays {@code III}.</p>
 *
 * <p>This mirrors {@code src/utils/personName.js}. The browser formats on blur and
 * before submitting, and the services use this class so a request that bypasses the
 * browser (an integration, a replayed payload) is stored in the same form. Formatting
 * is idempotent: formatting an already formatted name returns it unchanged.</p>
 */
public final class PersonNameFormatter {

    /** II, III, IV ... are suffixes and keep their capitals; "Jr" is title-cased. */
    private static final Pattern ROMAN_NUMERAL =
            Pattern.compile("(?i)^(i{1,3}|iv|v|vi{1,3}|ix|x)$");

    /** One run of letters, accents included. */
    private static final Pattern LETTER_RUN = Pattern.compile("\\p{L}+");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private PersonNameFormatter() {
    }

    /**
     * The standard form of one name, or {@code null} when the input is {@code null}.
     * A blank value stays blank.
     */
    public static String format(String raw) {
        if (raw == null) {
            return null;
        }
        String text = WHITESPACE.matcher(raw.trim()).replaceAll(" ");
        if (text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (String segment : text.split(",")) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(formatSegment(trimmed));
        }
        return result.toString();
    }

    /**
     * Formats a field that holds several names, one per line (the terminal report's
     * Project Team). Each line is formatted independently and the line structure,
     * including blank lines, is preserved.
     */
    public static String formatLines(String raw) {
        if (raw == null) {
            return null;
        }
        String[] lines = raw.split("\\R", -1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(format(lines[i]));
        }
        return result.toString();
    }

    private static String formatSegment(String segment) {
        String[] words = WHITESPACE.split(segment);
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(formatWord(word));
        }
        return result.toString();
    }

    /** Title-case every letter run and leave the punctuation exactly where it is. */
    private static String formatWord(String word) {
        Matcher matcher = LETTER_RUN.matcher(word);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            result.append(word, last, matcher.start());
            String run = matcher.group();
            result.append(ROMAN_NUMERAL.matcher(run).matches()
                    ? run.toUpperCase(Locale.ROOT)
                    : titleCase(run));
            last = matcher.end();
        }
        result.append(word.substring(last));
        return result.toString();
    }

    private static String titleCase(String run) {
        return run.substring(0, 1).toUpperCase(Locale.ROOT)
                + run.substring(1).toLowerCase(Locale.ROOT);
    }
}
