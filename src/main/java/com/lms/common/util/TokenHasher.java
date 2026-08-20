package com.lms.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes the opaque bearer tokens (refresh, invitation, password reset) that
 * are persisted as a digest rather than in plaintext.
 *
 * <p>SHA-256 is the right tool here and BCrypt is not: these tokens are 256
 * bits of {@link java.security.SecureRandom} output, so they are not subject to
 * dictionary attack, and lookup happens by digest on every refresh. A
 * deliberately slow hash would have to be recomputed per candidate row.
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    public static String sha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }

    /** Constant-time comparison for digests that are compared rather than looked up. */
    public static boolean matches(String rawToken, String expectedHash) {
        if (rawToken == null || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                sha256(rawToken).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
