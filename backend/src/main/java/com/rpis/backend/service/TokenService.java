package com.rpis.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Stateless bearer-token service.
 *
 * <p>A token is {@code base64(userId:role:issuedAtMillis:tokenVersion:hmacSha256)}.
 * Nothing is stored in memory, so the same token is accepted by every instance
 * behind the load balancer and a restart or a redeploy does not log anybody out. The
 * only shared requirements are the signing key, which is read from configuration
 * ({@code rpis.security.token-secret}) so all instances can be given the same value,
 * and the account's {@code tokenVersion}, which is stored in the shared database and
 * compared on every request. Signing out increments that column
 * ({@link com.rpis.backend.service.UserService#revokeTokens(Long)}), so every token
 * issued before the sign-out stops authenticating - on every instance - without
 * keeping the tokens themselves anywhere.
 *
 * <p>The signing key and the lifetime used to be compile-time constants. They are
 * now injected, with the previous values as defaults, so an existing deployment
 * keeps validating the tokens it has already handed out. A token issued before the
 * version field existed is still accepted as version 0, which is the value existing
 * accounts are migrated to; such a token is invalidated by the account's first
 * sign-out.
 */
@Service
public class TokenService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String TOKEN_SEPARATOR = ":";
    private static final int TOKEN_PARTS = 5;
    /** Tokens issued before the version field existed: userId:role:issuedAt:signature. */
    private static final int LEGACY_TOKEN_PARTS = 4;

    private final byte[] signingKey;
    private final long expirationTimeMs;

    public TokenService(
            @Value("${rpis.security.token-secret:RPIS_SUPER_SECURE_HMAC_SIGNING_KEY_2026_CHANGE_THIS_IN_PROD}") String secret,
            @Value("${rpis.security.token-ttl-ms:86400000}") long expirationTimeMs) {
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationTimeMs = expirationTimeMs;
    }

    /**
     * The decoded contents of a token. Parsing once per request replaces the
     * previous three separate base64 decodes (validate + user id + role), which
     * ran on every single authenticated request.
     *
     * @param tokenVersion the account's token generation this token was issued for
     * @param versioned    whether the token actually carried a version; a legacy
     *                     token without one is treated as version 0
     */
    public record ParsedToken(Long userId, String role, long issuedAtMillis,
                              int tokenVersion, boolean versioned, String signature) {
    }

    /**
     * Issues a version-0 token. Kept for callers that do not track a version (tests);
     * sign-in uses {@link #generateToken(Long, String, int)} with the account's
     * current version, so signing out invalidates the token.
     */
    public String generateToken(Long userId, String role) {
        return generateToken(userId, role, 0);
    }

    /**
     * Issues a token for an explicit token version. Sign-in passes the version read
     * from the account; every other caller (tests included) may keep using the
     * two-argument overload, which means version 0.
     */
    public String generateToken(Long userId, String role, int tokenVersion) {
        long timestamp = System.currentTimeMillis();
        String data = userId + TOKEN_SEPARATOR + role + TOKEN_SEPARATOR + timestamp
                + TOKEN_SEPARATOR + tokenVersion;
        String signature = hmacSha256(data);
        String rawToken = data + TOKEN_SEPARATOR + signature;
        return Base64.getEncoder().encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a token and returns its parts, or {@code null} when the token is not
     * even well formed. This does not verify the signature or the expiry; use
     * {@link #validateToken(String)} or {@link #parseValid(String)} for that.
     */
    public ParsedToken parse(String tokenStr) {
        if (tokenStr == null || tokenStr.isEmpty()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(tokenStr), StandardCharsets.UTF_8);
            String[] parts = decoded.split(TOKEN_SEPARATOR);
            if (parts.length == LEGACY_TOKEN_PARTS) {
                // A token issued before token versioning: it belongs to generation 0.
                return new ParsedToken(
                        Long.parseLong(parts[0]),
                        parts[1],
                        Long.parseLong(parts[2]),
                        0,
                        false,
                        parts[3]);
            }
            if (parts.length != TOKEN_PARTS) {
                return null;
            }
            return new ParsedToken(
                    Long.parseLong(parts[0]),
                    parts[1],
                    Long.parseLong(parts[2]),
                    Integer.parseInt(parts[3]),
                    true,
                    parts[4]);
        } catch (Exception e) {
            // A malformed token is an expected condition (expired client storage,
            // hand-edited header), not an error worth a stack trace.
            return null;
        }
    }

    /**
     * Decodes a token once and returns it only when the signature matches and it
     * has not expired; {@code null} otherwise. This is the single call the
     * authentication filter needs, and it halves the work per request compared to
     * validating and then parsing again.
     */
    public ParsedToken parseValid(String tokenStr) {
        ParsedToken parsed = parse(tokenStr);
        if (parsed == null) {
            return null;
        }
        if (System.currentTimeMillis() - parsed.issuedAtMillis() > expirationTimeMs) {
            return null;
        }
        // The signed payload of a legacy token has no version segment; rebuilding it
        // with one would reject every token issued before this change.
        String data = parsed.userId() + TOKEN_SEPARATOR + parsed.role() + TOKEN_SEPARATOR
                + parsed.issuedAtMillis()
                + (parsed.versioned() ? TOKEN_SEPARATOR + parsed.tokenVersion() : "");
        String expected = hmacSha256(data);
        // Constant-time comparison: a byte-by-byte early exit leaks how much of a
        // forged signature was correct.
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                parsed.signature().getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        return parsed;
    }

    public boolean validateToken(String tokenStr) {
        return parseValid(tokenStr) != null;
    }

    public Long getUserIdFromToken(String tokenStr) {
        ParsedToken parsed = parse(tokenStr);
        return parsed != null ? parsed.userId() : null;
    }

    public String getRoleFromToken(String tokenStr) {
        ParsedToken parsed = parse(tokenStr);
        return parsed != null ? parsed.role() : null;
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            // Mac is not thread-safe, so one instance is created per call rather
            // than shared. The cost is negligible next to the database work each
            // request performs, and it keeps the service free of shared mutable
            // state.
            mac.init(new SecretKeySpec(signingKey, ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Error computing HMAC signature", e);
        }
    }
}

