package com.rpis.backend.util;

import java.util.ArrayList;
import java.util.List;

/**
 * The one password policy every user-chosen password in RPIS must satisfy.
 *
 * <p>The browser shows the same rules live as the user types (src/utils/passwordPolicy.js),
 * but this class is the authority: registration, the profile password change, the
 * administrator's reset and the first-login change all pass through it. Server-generated
 * temporary passwords are deliberately exempt - they are short-lived, derived from the
 * account holder's name, and the account is forced to replace one before it can do
 * anything else.
 *
 * <p>The policy is: at least eight characters, at least one upper-case letter, at least
 * one lower-case letter, at least one digit and at least one symbol. The lower-case rule
 * predates this class and is kept so the change does not weaken an existing control; the
 * example password shown in the interface satisfies all five.
 *
 * <p>Nothing here logs, echoes or otherwise exposes the submitted password. The thrown
 * message names only the requirement that is missing.
 */
public final class PasswordPolicy {

    /** The shortest accepted new password. */
    public static final int MINIMUM_LENGTH = 8;

    private PasswordPolicy() {
    }

    /**
     * The requirements this password does not yet meet, in the order the interface shows
     * them. An empty list means the password is acceptable.
     */
    public static List<String> violations(String password) {
        String value = password == null ? "" : password;
        List<String> missing = new ArrayList<>();

        if (value.length() < MINIMUM_LENGTH) {
            missing.add("at least 8 characters");
        }

        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean symbol = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                symbol = true;
            }
        }
        if (!upper) {
            missing.add("at least one uppercase letter (A-Z)");
        }
        if (!lower) {
            missing.add("at least one lowercase letter (a-z)");
        }
        if (!digit) {
            missing.add("at least one number (0-9)");
        }
        if (!symbol) {
            missing.add("at least one special character or symbol");
        }

        return List.copyOf(missing);
    }

    /** True when every requirement is met. */
    public static boolean isValid(String password) {
        return violations(password).isEmpty();
    }

    /**
     * Throws with a requirement-only message when the password is unacceptable. The
     * submitted value itself is never part of the message.
     *
     * @throws IllegalArgumentException when at least one requirement is missing
     */
    public static void requireValid(String password) {
        List<String> missing = violations(password);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "The password must include " + String.join(", ", missing) + ".");
        }
    }
}
