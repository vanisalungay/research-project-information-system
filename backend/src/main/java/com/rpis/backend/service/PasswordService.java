package com.rpis.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * The single place that hashes and verifies account passwords.
 *
 * <p>RPIS used to store and compare passwords as plain text
 * ({@code user.getPassword().equals(password)}), in the database column, in the
 * seed data and in the login controller. This service replaces all of that with
 * BCrypt (via the {@link PasswordEncoder} bean in {@code SecurityConfig}) and keeps
 * one verification path, so no caller has to know how a password is stored.
 *
 * <h2>Legacy accounts are never locked out</h2>
 * Rows written before hashing existed hold plain text. {@link #verify} therefore
 * understands both shapes: a stored value that is not a BCrypt hash is compared
 * directly, and the result reports that it was {@link Verification#legacy() legacy}
 * so the caller can replace it with a hash immediately. Combined with
 * {@link PasswordMigrationService} (which converts every row in one pass) there are
 * two independent ways for an existing account to keep working, so a user can never
 * be locked out by the migration.
 *
 * <h2>Nothing here logs a password</h2>
 * No method in this class writes a raw password, a hash or a comparison result to
 * any log. Callers must keep that property: log the account id, never the value.
 */
@Slf4j
@Service
public class PasswordService {

    /**
     * A BCrypt hash: {@code $2a$} / {@code $2b$} / {@code $2y$}, a two-digit cost,
     * then the 53 characters of salt and digest. Anything that does not match is
     * treated as a legacy plain-text value.
     */
    private static final Pattern BCRYPT_HASH =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final PasswordEncoder passwordEncoder;

    public PasswordService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /** True when the stored value is already a BCrypt hash rather than plain text. */
    public boolean isHashed(String stored) {
        return stored != null && BCRYPT_HASH.matcher(stored).matches();
    }

    /**
     * Hashes a raw password for storage.
     *
     * @throws IllegalArgumentException when the password is null or blank, so a
     *                                 caller can never accidentally store an empty
     *                                 credential
     */
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("A password is required.");
        }
        return passwordEncoder.encode(rawPassword);
    }

    /** Hashes a raw password only when it is non-blank; otherwise returns null. */
    public String hashIfPresent(String rawPassword) {
        return rawPassword == null || rawPassword.isBlank() ? null : passwordEncoder.encode(rawPassword);
    }

    /**
     * Verifies a presented password against the stored value.
     *
     * <p>Constant in behaviour for both shapes: a BCrypt hash is checked with the
     * encoder, and a legacy plain-text value is compared directly. The returned
     * {@link Verification#legacy()} flag tells the caller that the stored value still
     * needs hashing.
     */
    public Verification verify(String stored, String presented) {
        if (stored == null || presented == null) {
            return new Verification(false, false);
        }
        if (isHashed(stored)) {
            return new Verification(passwordEncoder.matches(presented, stored), false);
        }
        // Legacy plain-text row. Compared without the encoder; the caller upgrades it.
        return new Verification(stored.equals(presented), true);
    }

    /**
     * The outcome of a verification.
     *
     * @param matches whether the presented password is correct
     * @param legacy  whether the stored value was plain text and should now be
     *                replaced by a hash
     */
    public record Verification(boolean matches, boolean legacy) {
    }
}
