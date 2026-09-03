package com.lms.security.jwt;

import com.lms.config.JwtConfig;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Exposes a JWKS-compatible discovery document so that trusted services
 * (e.g. lms-certificate-service) can verify tokens issued by this backend
 * without receiving the raw secret.
 *
 * <p><strong>Security note:</strong> This backend uses HS256 (HMAC-SHA256),
 * a symmetric algorithm. The raw secret is <em>never</em> returned here.
 * The document only advertises the key ID ({@code kid}) and algorithm so that
 * a peer service that already knows the shared secret can confirm it is using
 * the correct key. If you migrate to RS256 this controller should be updated
 * to return the RSA public key in JWK format.
 */
@RestController
@RequestMapping("/api/v1/.well-known")
@RequiredArgsConstructor
public class JwksController {

    /**
     * Static kid derived from the first 8 characters of the base64-encoded key.
     * Deterministic so that the cert service can cache it across restarts.
     */
    private static final String KID_PREFIX = "lms-hs256-";

    private final JwtConfig jwtConfig;

    /**
     * {@code GET /api/v1/.well-known/jwks.json}
     *
     * <p>Returns a minimal JWKS document containing the key metadata for the
     * HS256 signing key currently in use. The actual key bytes are not included.
     */
    @GetMapping("/jwks.json")
    public Map<String, Object> jwks() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        // Derive a stable kid from the first 8 chars of the base64 of the key material.
        String kid = KID_PREFIX + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(keyBytes)
                .substring(0, 8);

        Map<String, Object> key = Map.of(
                "kty", "oct",       // Octet sequence (symmetric)
                "alg", "HS256",
                "use", "sig",
                "kid", kid
                // NOTE: "k" (the raw key value) is intentionally omitted.
        );

        return Map.of("keys", List.of(key));
    }
}
