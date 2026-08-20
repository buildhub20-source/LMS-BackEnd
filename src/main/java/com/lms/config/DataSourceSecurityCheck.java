package com.lms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * Refuses to start against a cloud database over an unencrypted connection.
 *
 * <p>The database is a managed instance reached over the public internet in
 * every environment, so credentials and row data cross a network we do not
 * control. PgJDBC defaults to {@code sslmode=prefer}, which silently downgrades
 * to plaintext if the negotiation fails; requiring it explicitly turns that
 * into a hard failure.
 *
 * <p>Localhost and in-memory databases are exempt.
 */
@Slf4j
@Component
public class DataSourceSecurityCheck implements InitializingBean {

    private static final String[] ACCEPTED_SSL_MODES = {
            "sslmode=require", "sslmode=verify-ca", "sslmode=verify-full"
    };

    private final String jdbcUrl;
    private final Environment environment;

    public DataSourceSecurityCheck(@Value("${spring.datasource.url}") String jdbcUrl,
                                   Environment environment) {
        this.jdbcUrl = jdbcUrl;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String url = jdbcUrl.toLowerCase(Locale.ROOT);

        if (!url.startsWith("jdbc:postgresql:") || isLocal(url) || hasSsl(url)) {
            return;
        }

        String message = "Refusing to connect to a remote database without TLS. "
                + "Append ?sslmode=require to DB_URL.";

        if (environment.matchesProfiles("prod")) {
            throw new IllegalStateException(message);
        }

        // Non-prod still fails: a shared cloud database holds real credentials
        // regardless of which profile is talking to it.
        throw new IllegalStateException(message + " (active profiles: "
                + String.join(",", environment.getActiveProfiles()) + ")");
    }

    private boolean isLocal(String url) {
        return url.contains("//localhost") || url.contains("//127.0.0.1") || url.contains("//[::1]");
    }

    private boolean hasSsl(String url) {
        return Arrays.stream(ACCEPTED_SSL_MODES).anyMatch(url::contains);
    }
}
