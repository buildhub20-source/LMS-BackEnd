package com.lms.common.util;

import java.security.SecureRandom;
import java.util.Base64;

/** Generates cryptographically strong, URL-safe opaque tokens. */
public final class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private TokenGenerator() {
    }

    public static String urlSafeToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    public static String urlSafeToken() {
        return urlSafeToken(32);
    }
}
