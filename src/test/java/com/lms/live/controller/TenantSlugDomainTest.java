package com.lms.live.controller;

import com.lms.platform.dto.CreateTenantRequest;
import com.lms.platform.runtime.TenantSlug;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A tenant's slug is what becomes its subdomain, so whatever creation accepts
 * must also be usable as a hostname label.
 *
 * <p>The display name is deliberately not involved: it is free text and is
 * never folded into the domain. These tests pin that separation, and the
 * agreement between the two places that judge a slug.
 */
@DisplayName("tenant slug and domain")
class TenantSlugDomainTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private static boolean creationAccepts(String slug) {
        CreateTenantRequest request = new CreateTenantRequest(
                "Any Name", slug, "Owner", "owner@example.com", "longenoughpassword", null);
        return VALIDATOR.validateProperty(request, "slug").isEmpty();
    }

    private static boolean redirectAccepts(String slug) {
        try {
            GoogleCalendarController.tenantFrontendUrl("https://example.com", "example.com", slug);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "lms", "school", "st-marys", "campus-one", "abc123", "a1-b2-c3",
            "my--school",      // consecutive hyphens
            "-leading",        // leading hyphen
            "trailing-",       // trailing hyphen
            "ab",              // too short
            "UPPER",           // not lowercase
            "has space",
            "has.dot",
            "under_score"
    })
    @DisplayName("anything creation accepts must also work as a hostname label")
    void creationAndRedirectAgree(String slug) {
        // The slug becomes the subdomain, so a value that survives creation but
        // breaks hostname building would leave that tenant unable to complete
        // an OAuth callback.
        if (creationAccepts(slug)) {
            assertThat(redirectAccepts(slug))
                    .as("creation accepted \"%s\" but it cannot be used as a hostname label", slug)
                    .isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"lms", "school", "st-marys", "campus-one", "abc123", "a1-b2-c3"})
    @DisplayName("ordinary slugs are accepted")
    void ordinarySlugsAccepted(String slug) {
        assertThat(creationAccepts(slug)).as(slug).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "my--school", "-leading", "trailing-", "ab", "a", "UPPER",
            "has space", "has.dot", "under_score", "sub.domain"
    })
    @DisplayName("malformed slugs are refused at creation")
    void malformedSlugsRefused(String slug) {
        assertThat(creationAccepts(slug)).as(slug).isFalse();
    }

    @Test
    @DisplayName("a 63 character slug is allowed, 64 is not")
    void lengthBounds() {
        assertThat(creationAccepts("a".repeat(63))).isTrue();
        assertThat(creationAccepts("a".repeat(64))).isFalse();
        assertThat(creationAccepts("abc")).isTrue();
    }

    @Test
    @DisplayName("the display name never reaches the domain")
    void nameIsNotPartOfTheDomain() {
        // A name can be anything; only the slug decides the host.
        assertThat(creationAccepts("st-marys")).isTrue();
        assertThat(GoogleCalendarController.tenantFrontendUrl(
                "https://example.com", "example.com", "st-marys"))
                .isEqualTo("https://st-marys.example.com");
    }

    @Test
    @DisplayName("the slug is placed as a label in front of the base domain")
    void slugBecomesTheSubdomain() {
        assertThat(GoogleCalendarController.tenantFrontendUrl(
                "http://localhost:3000", "localhost", "lms"))
                .isEqualTo("http://lms.localhost:3000");
        assertThat(GoogleCalendarController.tenantFrontendUrl(
                "https://example.com", "example.com", "school"))
                .isEqualTo("https://school.example.com");
    }

    @Test
    @DisplayName("a slug that is already the subdomain of the current host still resolves")
    void alreadyOnTheTenantHost() {
        assertThatCode(() -> GoogleCalendarController.tenantFrontendUrl(
                "https://school.example.com", "example.com", "school"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"www", "api", "admin", "platform", "mail", "localhost", "WWW", " api ",
            "chat", "notify", "notifications"})
    @DisplayName("labels that name infrastructure are reserved")
    void infrastructureLabelsReserved(String slug) {
        // Well-formed, but it would shadow a real host on the base domain.
        assertThat(TenantSlug.isReserved(slug)).as(slug).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"lms", "school", "st-marys", "campus-one"})
    @DisplayName("an ordinary workspace slug is not reserved")
    void ordinarySlugsNotReserved(String slug) {
        assertThat(TenantSlug.isReserved(slug)).as(slug).isFalse();
    }

    @Test
    @DisplayName("a host outside the configured base domain is refused")
    void foreignHostRefused() {
        assertThatThrownBy(() -> GoogleCalendarController.tenantFrontendUrl(
                "https://evil.example", "example.com", "school"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
