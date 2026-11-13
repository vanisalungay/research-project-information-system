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
 * <pre>firstname + middlename + "." + lastname + "@msunawaan.edu.ph"</pre>
 *
 * with the given names run together and everything lower-cased and stripped of accents and
 * punctuation - {@code Xarzha Maurine Bagares} becomes
 * {@code xarzhamaurine.bagares@msunawaan.edu.ph}.
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

    /** The institution's domain. Every generated address ends with it. */
    public static final String INSTITUTIONAL_DOMAIN = "@msunawaan.edu.ph";

    /**
     * The misspelling this codebase used before the domain was corrected. It is still
     * accepted when an address is validated, so the accounts that already exist (and the
     * seeded demonstration accounts) keep working; nothing new is ever created with it.
     */
    public static final String LEGACY_DOMAIN = "@msunaawan.edu.ph";

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

    private final UserRepository userRepository;

    /** True when the address belongs to the institution (current or legacy spelling). */
    public static boolean isInstitutionalAddress(String email) {
        if (email == null) {
            return false;
        }
        String value = email.trim().toLowerCase(Locale.ROOT);
        return value.endsWith(INSTITUTIONAL_DOMAIN) || value.endsWith(LEGACY_DOMAIN);
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
     * The local part of the address: the given names run together, a dot, then the surname.
     * One name is used as it is; two or more treat the last word as the surname. Returns ""
     * when the name carries nothing usable.
     */
    public static String localPart(String fullName) {
        java.util.List<String> parts = normalizedParts(fullName);
        if (parts.isEmpty()) {
            return "";
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        String surname = parts.get(parts.size() - 1);
        StringBuilder given = new StringBuilder();
        for (int i = 0; i < parts.size() - 1; i++) {
            given.append(parts.get(i));
        }
        return given + "." + surname;
    }

    /** The words of a name, lower case and stripped of accents and punctuation. */
    private static java.util.List<String> normalizedParts(String fullName) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (fullName == null) {
            return parts;
        }
        for (String value : fullName.trim().split("\\s+")) {
            String normalized = normalizePart(value);
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
        java.util.List<String> parts = normalizedParts(fullName);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException(
                    "A full name is required to generate a temporary password.");
        }
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
            if (userRepository.findByEmail(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Every email address derived from this name is already taken.");
    }

}