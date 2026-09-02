package com.lms.platform.security;

import com.lms.common.constants.SecurityConstants;
import com.lms.common.exception.InvalidTokenException;
import com.lms.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Separate token type prevents a tenant-admin token being used in the control plane. */
@Service
public class PlatformJwtService {
    private final JwtConfig config;
    private final SecretKey key;

    public PlatformJwtService(JwtConfig config) {
        this.config = config;
        this.key = Keys.hmacShaKeyFor(config.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(PlatformAdminPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder().subject(principal.email()).issuer(config.getIssuer())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(config.getAccessTokenTtl())))
                .claim(SecurityConstants.CLAIM_PLATFORM_ADMIN_ID, principal.id().toString())
                .claim(SecurityConstants.CLAIM_TOKEN_TYPE, SecurityConstants.TOKEN_TYPE_PLATFORM_ACCESS)
                .signWith(key).compact();
    }

    public PlatformAdminPrincipal parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(config.getIssuer())
                    .clockSkewSeconds(config.getClockSkew().toSeconds()).build().parseSignedClaims(token).getPayload();
            if (!SecurityConstants.TOKEN_TYPE_PLATFORM_ACCESS.equals(
                    claims.get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class))) {
                throw new InvalidTokenException("Expected a platform access token");
            }
            return new PlatformAdminPrincipal(UUID.fromString(
                    claims.get(SecurityConstants.CLAIM_PLATFORM_ADMIN_ID, String.class)), claims.getSubject(), true);
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Platform token is not valid");
        }
    }
}
