package com.lms.platform.runtime;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * The grammar of a tenant slug.
 *
 * <p>A slug is used verbatim as the DNS label in front of the base domain, so
 * it has to be a legal hostname label and not merely a legal identifier. This
 * class is the single place that decides so: the rule was previously written
 * out twice, once where a tenant is created and once where that subdomain is
 * built, and the two did not agree — a slug with consecutive hyphens passed
 * creation and then broke the OAuth callback for that tenant, because only the
 * second rule rejected it.
 *
 * <p>Deliberately stricter than DNS, which does allow consecutive hyphens:
 * a doubled hyphen in the third and fourth position is reserved for punycode,
 * and nothing is lost by requiring single separators throughout.
 */
public final class TenantSlug {

    /**
     * Lowercase alphanumeric groups joined by single hyphens, with no hyphen
     * at either end. Length is bounded separately so that callers which only
     * need the shape — such as building a host for an existing tenant — are
     * not applying today's length rule to a slug stored long ago.
     */
    public static final String SHAPE = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

    public static final int MIN_LENGTH = 3;

    public static final int MAX_LENGTH = 63;

    /**
     * Labels that must not become a tenant, because they already name
     * something else on the base domain. A tenant on {@code www} or
     * {@code api} would shadow the real host. {@code chat} and {@code notify}
     * are where the chat and notification services are served.
     */
    private static final Set<String> RESERVED = Set.of(
            "www", "api", "app", "admin", "platform", "mail", "smtp", "imap",
            "static", "assets", "cdn", "status", "support", "help", "docs",
            "login", "auth", "test", "staging", "dev", "localhost",
            "chat", "notify", "notifications", "ws", "socket");

    private static final Pattern COMPILED = Pattern.compile(SHAPE);

    private TenantSlug() {
    }

    /** True when the slug can be used as a hostname label. */
    public static boolean isWellFormed(String slug) {
        return slug != null && COMPILED.matcher(slug).matches();
    }

    /** True when the slug names infrastructure rather than a tenant. */
    public static boolean isReserved(String slug) {
        return slug != null && RESERVED.contains(slug.trim().toLowerCase());
    }
}
