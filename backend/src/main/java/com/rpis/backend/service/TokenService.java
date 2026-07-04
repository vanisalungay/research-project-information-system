package com.rpis.backend.service;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class TokenService {

    private static final String SECRET_KEY = "RPIS_SUPER_SECURE_HMAC_SIGNING_KEY_2026_CHANGE_THIS_IN_PROD";
    private static final long EXPIRATION_TIME_MS = 24 * 60 * 60 * 1000L; // 24 Hours

    public String generateToken(Long userId, String role) {
        long timestamp = System.currentTimeMillis();
        String data = userId + ":" + role + ":" + timestamp;
        String signature = hmacSha256(data, SECRET_KEY);
        String rawToken = data + ":" + signature;
        return Base64.getEncoder().encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String tokenStr) {
        if (tokenStr == null || tokenStr.isEmpty()) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(tokenStr), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 4) {
                return false;
            }

            long timestamp = Long.parseLong(parts[2]);
            String signature = parts[3];

            // Check expiration
            if (System.currentTimeMillis() - timestamp > EXPIRATION_TIME_MS) {
                return false;
            }

            // Re-verify signature
            String data = parts[0] + ":" + parts[1] + ":" + parts[2];
            String expectedSignature = hmacSha256(data, SECRET_KEY);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String tokenStr) {
        try {
            String decoded = new String(Base64.getDecoder().decode(tokenStr), StandardCharsets.UTF_8);
            return Long.parseLong(decoded.split(":")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    public String getRoleFromToken(String tokenStr) {
        try {
            String decoded = new String(Base64.getDecoder().decode(tokenStr), StandardCharsets.UTF_8);
            return decoded.split(":")[1];
        } catch (Exception e) {
            return null;
        }
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error computing HMAC signature", e);
        }
    }
}
