package com.rpis.backend.service;

import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

/**
 * The institutional credentials an administrator-created account is issued.
 *
 * <p>The administrator types only the person's name: the address and the temporary password
 * are derived here, on the server, so the browser can never choose them. The naming rule is
 *
 * <pre>firstname + middlename + "." + lastname + "@msunaawan.edu.ph"</pre>
 *
 * with the given names run together and everything lower-cased and stripped of accents and
 * punctuation - {@code Xarzha Maurine Bagares} becomes
 * {@code xarzhamaurine.bagares@msunaawan.edu.ph}.
 *
 * <p>The name may be sent in the standard RPIS order ({@code Dela Cruz, Juan, M., Jr.}) or
 * as the Dean form's plain first-name-last-name order ({@code Juan Dela Cruz}). Both are
 * understood and resolve to the same address; an existing account's address is never
 * re-derived.</p>
 *
 * <p>The temporary password follows the format {@code [first initial][surname]123}:
 * {@code Xarzha Maurine Bagares} becomes {@code xbagares123}. It is returned to the
 * administrator once, in the creation response, and is stored only as a BCrypt hash.
 *
 * <p>Deriving it from the name trades secrecy for readability - the administrator can read it
 * out to the person who will own the account - so every account it is issued to is created
 * with {@code mustChangePassword} set: the value stops working the moment its owner signs in
 * and chooses a password of their own.
 */
@Service
@RequiredArgsConstructor
public class InstitutionalCredentialsService {

    /**
     * The institution's domain. Every generated address ends with it, and only this domain
     * is accepted for an account. The previous spelling contained an extra letter; the
     * migration {@code migration-fix-institutional-email-domain.sql} corrects the accounts
     * that were created with it.
     */
    public static final String INSTITUTIONAL_DOMAIN = "@msunaawan.edu.ph";

    /** How many numbered variants the uniqueness loop will try before giving up. */
    private static final int MAX_SUFFIX = 9999;

    /** Every generated temporary password ends with these digits. */
    private static final String TEMPORARY_PASSWORD_SUFFIX = "123";

    /**
     * Words that belong to the surname rather than to the given names, so {@code Dela Cruz} is
     * one surname while {@code Clara} is a given name. Compared lower case, without punctuation.
     */
    private static final java.util.Set<String> SURNAME_PARTICLES = java.util.Set.of(
            "de", "del", "dela", "delas", "dele", "la", "las", "los", "van", "von",
            "der", "den", "san", "santa", "sta", "mac");

    /** Jr, Sr, II, III ... are suffixes, not middle names, when a name is split on commas. */
    private static final java.util.Set<String> SUFFIXES = java.util.Set.of(
            "jr", "sr", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x");

    /** A name split into the given names and the surname. */
    private record NameParts(java.util.List<String> given, java.util.List<String> surname) {
    }

    private final UserRepository userRepository;

    /** True when the address belongs to the institution's one correct domain. */
    public static boolean isInstitutionalAddress(String email) {
        if (email == null) {
            return false;
        }
        String value = email.trim().toLowerCase(Locale.ROOT);
        return value.endsWith(INSTITUTIONAL_DOMAIN);
    }

    /** Lower case, no diacritics, nothing but a-z and 0-9 left. */
    static String normalizePart(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036f]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    /**
     * Splits a name into given names and surname, accepting both the standard
     * {@code Dela Cruz, Juan, M., Jr.} order and the legacy {@code Juan Dela Cruz} order
     * still stored on existing accounts.
     *
     * <p>With commas the first component is the surname, the second the given names and
     * the third the middle initial - or the suffix when the middle initial was left out.
     * Anything after the middle initial is a suffix and never part of the address. Without
     * a comma the name is read as "First Name Last Name", with the surname being the last
     * word plus any particles directly before it - the same rule the temporary password
     * uses, so {@code Juan Dela Cruz} and {@code Dela Cruz, Juan} derive the same address.</p>
     */
    static NameParts nameParts(String fullName) {
        String text = fullName == null ? "" : fullName.replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            return new NameParts(java.util.List.of(), java.util.List.of());
        }

        if (text.contains(",")) {
            String[] segments = text.split(",");
            java.util.List<String> surname = normalizedWords(segments[0]);
            java.util.List<String> given = segments.length > 1
                    ? normalizedWords(segments[1])
                    : new java.util.ArrayList<>();
            if (segments.length > 2) {
                String middle = segments[2].trim();
                if (!middle.isEmpty() && !SUFFIXES.contains(normalizePart(middle))) {
                    given.addAll(normalizedWords(middle));
                }
            }
            return new NameParts(given, surname);
        }

        java.util.List<String> parts = normalizedWords(text);
        if (parts.size() <= 1) {
            return new NameParts(java.util.List.of(), parts);
        }
        // "First Name Last Name": the surname is the last word plus the particles directly
        // before it, so "Dela Cruz" stays one surname. This mirrors temporaryPasswordFor and
        // keeps the address identical to the comma order for the same person.
        int surnameFrom = parts.size() - 1;
        while (surnameFrom > 0 && SURNAME_PARTICLES.contains(parts.get(surnameFrom - 1))) {
            surnameFrom--;
        }
        return new NameParts(parts.subList(0, surnameFrom),
                parts.subList(surnameFrom, parts.size()));
    }

    /**
     * The local part of the address: the given names run together, a dot, then the whole
     * surname. The standard "Last, First Middle, Suffix" order and the Dean form's
     * "First Name Last Name" order both resolve to the same given names and surname.
     * Returns "" when the name carries nothing usable.
     */
    public static String localPart(String fullName) {
        NameParts parts = nameParts(fullName);
        String given = String.join("", parts.given());
        String surname = String.join("", parts.surname());
        if (given.isEmpty() && surname.isEmpty()) {
            return "";
        }
        if (given.isEmpty()) {
            return surname;
        }
        if (surname.isEmpty()) {
            return given;
        }
        return given + "." + surname;
    }

    /** The words of a name, lower case and stripped of accents and punctuation. */
    private static java.util.List<String> normalizedParts(String fullName) {
        return normalizedWords(fullName);
    }

    /** The words of one name component, normalized, in order. */
    private static java.util.List<String> normalizedWords(String value) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (value == null) {
            return parts;
        }
        for (String word : value.trim().split("\\s+")) {
            String normalized = normalizePart(word);
            if (!normalized.isEmpty()) {
                parts.add(normalized);
            }
        }
        return parts;
    }

    /**
     * The temporary password for a name: the first letter of the first given name, the whole
     * surname, then {@code 123} - {@code Xarzha Maurine Bagares} becomes {@code xbagares123}.
     *
     * <p>The surname is the last word plus any particle directly in front of it, so
     * {@code Dela Cruz} counts as one surname and {@code Maria Clara} as given names. A
     * one-word name is its own initial and surname ({@code Bagares} becomes {@code bbagares123}).
     * The result is lower case, without spaces, accents or punctuation, and is only ever stored
     * as a BCrypt hash.
     */
    public static String temporaryPasswordFor(String fullName) {
        String text = fullName == null ? "" : fullName.replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(
                    "A full name is required to generate a temporary password.");
        }

        // Standard "Dela Cruz, Juan, M., Jr." order: the surname is the first component.
        if (text.contains(",")) {
            NameParts parts = nameParts(text);
            if (parts.surname().isEmpty()) {
                throw new IllegalArgumentException(
                        "A full name is required to generate a temporary password.");
            }
            String firstGiven = parts.given().isEmpty()
                    ? parts.surname().get(0)
                    : parts.given().get(0);
            StringBuilder password = new StringBuilder();
            password.append(firstGiven.charAt(0));
            for (String word : parts.surname()) {
                password.append(word);
            }
            password.append(TEMPORARY_PASSWORD_SUFFIX);
            return password.toString();
        }

        // Legacy "Juan Dela Cruz" order: the surname is the last word plus its particles.
        java.util.List<String> parts = normalizedParts(text);
        int surnameFrom = parts.size() - 1;
        while (surnameFrom > 0 && SURNAME_PARTICLES.contains(parts.get(surnameFrom - 1))) {
            surnameFrom--;
        }
        StringBuilder password = new StringBuilder();
        password.append(parts.get(0).charAt(0));
        for (int i = surnameFrom; i < parts.size(); i++) {
            password.append(parts.get(i));
        }
        password.append(TEMPORARY_PASSWORD_SUFFIX);
        return password.toString();
    }

    /** The plain address for a name, with no uniqueness check. Empty when the name is unusable. */
    public static String emailFor(String fullName) {
        String local = localPart(fullName);
        return local.isEmpty() ? "" : local + INSTITUTIONAL_DOMAIN;
    }

    /**
     * The first free address for this name: the plain one, or the same local part with 2, 3,
     * ... before the domain while those are already taken. The database is the authority, so
     * an administrator never has to type or check an address.
     */
    @Transactional(readOnly = true)
    public String availableEmailFor(String fullName) {
        String local = localPart(fullName);
        if (local.isEmpty()) {
            throw new IllegalArgumentException(
                    "A full name is required to generate an institutional email address.");
        }
        for (int suffix = 1; suffix <= MAX_SUFFIX; suffix++) {
            String candidate = (suffix == 1 ? local : local + suffix) + INSTITUTIONAL_DOMAIN;
            // Normalized lookup: an account stored with an older capitalization still owns
            // its address, so a new account can never be issued a case-only variant of it.
            if (userRepository.findByNormalizedEmail(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Every email address derived from this name is already taken.");
    }

}