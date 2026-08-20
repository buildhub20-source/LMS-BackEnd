package com.lms.security;

import com.lms.common.exception.InvalidTokenException;
import com.lms.common.util.TokenHasher;
import com.lms.config.JwtConfig;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID SESSION_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");

    private JwtService jwtService;
    private LmsUserDetails principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(config("unit-test-secret-key-that-is-long-enough-1234"));
        principal = new LmsUserDetails(USER_ID, "ada@lms.test", "Ada Lovelace", null, true, false,
                Set.of("INSTRUCTOR"), Set.of("COURSE_VIEW", "COURSE_CREATE"), null);
    }

    @Test
    void accessTokenCarriesIdentityRolesPermissionsAndSession() {
        Claims claims = jwtService.parseAccessToken(jwtService.generateAccessToken(principal, SESSION_ID));

        assertThat(claims.getSubject()).isEqualTo("ada@lms.test");
        assertThat(jwtService.userId(claims)).isEqualTo(USER_ID);
        assertThat(jwtService.sessionId(claims)).isEqualTo(SESSION_ID);
        assertThat(jwtService.roles(claims)).containsExactly("INSTRUCTOR");
        assertThat(jwtService.permissions(claims)).containsExactlyInAnyOrder("COURSE_VIEW", "COURSE_CREATE");
    }

    @Test
    void accessTokenNeverCarriesCredentialMaterial() {
        String token = jwtService.generateAccessToken(principal, SESSION_ID);
        Claims claims = jwtService.parseAccessToken(token);

        assertThat(claims).doesNotContainKeys("password", "passwordHash");
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRejected() {
        JwtService other = new JwtService(config("a-completely-different-secret-key-9876543210"));
        String foreignToken = other.generateAccessToken(principal, SESSION_ID);

        assertThatThrownBy(() -> jwtService.parseAccessToken(foreignToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("not valid");
    }

    @Test
    void anExpiredTokenIsRejected() {
        JwtConfig expiring = config("unit-test-secret-key-that-is-long-enough-1234");
        expiring.setAccessTokenTtl(Duration.ofSeconds(-120));
        expiring.setClockSkew(Duration.ZERO);

        String expired = new JwtService(expiring).generateAccessToken(principal, SESSION_ID);

        assertThatThrownBy(() -> jwtService.parseAccessToken(expired))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void garbageIsRejectedRatherThanParsed() {
        assertThatThrownBy(() -> jwtService.parseAccessToken("not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void aShortSecretIsRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtService(config("too-short")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void refreshTokensAreHashedNotStoredInPlaintext() {
        String raw = "a-refresh-token-value";
        String digest = TokenHasher.sha256(raw);

        assertThat(digest).isNotEqualTo(raw).hasSize(64);
        assertThat(TokenHasher.matches(raw, digest)).isTrue();
        assertThat(TokenHasher.matches("something-else", digest)).isFalse();
    }

    private JwtConfig config(String secret) {
        JwtConfig config = new JwtConfig();
        config.setSecret(secret);
        config.setIssuer("lms-backend");
        config.setAccessTokenTtl(Duration.ofMinutes(15));
        config.setRefreshTokenTtl(Duration.ofDays(7));
        config.setClockSkew(Duration.ofSeconds(30));
        return config;
    }
}
