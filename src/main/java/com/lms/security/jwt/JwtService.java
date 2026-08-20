package com.lms.security.jwt;

import com.lms.common.constants.SecurityConstants;
import com.lms.common.exception.InvalidTokenException;
import com.lms.config.JwtConfig;
import com.lms.security.authentication.LmsUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issues and validates the access JWT.
 *
 * <p>The refresh token is deliberately not a JWT: it is opaque random material
 * whose digest is stored in {@code user_session}, which is what makes
 * server-side revocation and rotation possible.
 */
@Slf4j
@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "lms.security.jwt.secret must be at least 32 bytes for HS256 signing");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Mints an access token for a principal bound to the given session.
     *
     * <p>The token carries identity and authorities only. It never carries the
     * password hash or any other credential material.
     */
    public String generateAccessToken(LmsUserDetails principal, UUID sessionId) {
        Instant now = Instant.now();

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(SecurityConstants.CLAIM_USER_ID, principal.getUserId().toString());
        claims.put(SecurityConstants.CLAIM_SESSION_ID, sessionId.toString());
        claims.put(SecurityConstants.CLAIM_ROLES, List.copyOf(principal.getRoles()));
        claims.put(SecurityConstants.CLAIM_PERMISSIONS, List.copyOf(principal.getPermissions()));
        claims.put(SecurityConstants.CLAIM_TOKEN_TYPE, SecurityConstants.TOKEN_TYPE_ACCESS);

        return Jwts.builder()
                .subject(principal.getUsername())
                .issuer(jwtConfig.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtConfig.getAccessTokenTtl())))
                .id(UUID.randomUUID().toString())
                .claims(claims)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        String type = claims.get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class);
        if (!SecurityConstants.TOKEN_TYPE_ACCESS.equals(type)) {
            throw new InvalidTokenException("Expected an access token");
        }
        return claims;
    }

    public UUID userId(Claims claims) {
        return uuidClaim(claims, SecurityConstants.CLAIM_USER_ID);
    }

    public UUID sessionId(Claims claims) {
        return uuidClaim(claims, SecurityConstants.CLAIM_SESSION_ID);
    }

    public Set<String> roles(Claims claims) {
        return claimAsSet(claims, SecurityConstants.CLAIM_ROLES);
    }

    public Set<String> permissions(Claims claims) {
        return claimAsSet(claims, SecurityConstants.CLAIM_PERMISSIONS);
    }

    public long accessTokenTtlSeconds() {
        return jwtConfig.getAccessTokenTtl().toSeconds();
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(jwtConfig.getIssuer())
                    .clockSkewSeconds(jwtConfig.getClockSkew().toSeconds())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("Token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Token is not valid");
        }
    }

    private UUID uuidClaim(Claims claims, String name) {
        String value = claims.get(name, String.class);
        if (value == null) {
            throw new InvalidTokenException("Token is missing the " + name + " claim");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("Token claim " + name + " is not a valid identifier");
        }
    }

    private Set<String> claimAsSet(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }
}
