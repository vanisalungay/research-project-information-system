package com.rpis.backend.util;

import java.util.Locale;

/**
 * The one way an e-mail address is normalized before it is stored, compared or looked up.
 *
 * <p>The sign-in form used to compare the typed value byte-for-byte with the stored column.
 * Auto-capitalization on the first letter, a trailing space or a non-breaking space pasted
 * from the account table therefore produced "the same address" that did not match - the
 * address that worked when pasted failed when typed. The institutional domain and every
 * generated local part are lower case, so the comparison is done in lower case after the
 * invisible formatting is removed.
 *
 * <p>Only insignificant input differences are removed:
 * <ul>
 *   <li>leading and trailing whitespace, including the Unicode spaces {@link String#strip()}
 *       handles;</li>
 *   <li>non-breaking spaces and zero-width characters, which are invisible in the account
 *       table but are not part of any valid address;</li>
 *   <li>letter case (e-mail domains are case-insensitive; the institution's addresses are
 *       issued and stored in lower case).</li>
 * </ul>
 * Nothing inside the address is rewritten - no character of a valid address is dropped or
 * substituted - so an invalid address stays invalid and is refused by the existing
 * validation rather than being silently turned into a different account.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
        // Constants and static helpers only.
    }

    /**
     * The normalized address, or {@code null} when there is none. A blank input is
     * returned as an empty string (not null) so callers can distinguish "missing" from
     * "present but empty" the way they already do.
     */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String cleaned = email
                // A non-breaking space is not Character.isWhitespace, so strip() alone would
                // leave it attached to the address.
                .replace(' ', ' ')
                .replace(' ', ' ')
                .replace(' ', ' ')
                // Zero-width characters are invisible in the UI but are still characters in
                // the value. They can never be part of an address this system issues.
                .replace("​", "")
                .replace("‌", "")
                .replace("‍", "")
                .replace("﻿", "")
                .replace("⁠", "");
        // strip() removes the Unicode whitespace that trim() (<= U+0020) does not.
        return cleaned.strip().toLowerCase(Locale.ROOT);
    }

    /** The canonical spelling of a role: trimmed and upper case; null when absent. */
    public static String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String cleaned = role.replace(' ', ' ').strip();
        return cleaned.isEmpty() ? null : cleaned.toUpperCase(Locale.ROOT);
    }
}
