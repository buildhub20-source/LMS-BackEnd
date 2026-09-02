package com.lms.config;

import com.lms.common.constants.ApiPaths;
import com.lms.config.InternalApiConfig;
import com.lms.security.authentication.CustomUserDetailsService;
import com.lms.security.authorization.JwtAccessDeniedHandler;
import com.lms.security.authorization.PasswordChangeRequiredFilter;
import com.lms.security.authorization.ServiceKeyAuthFilter;
import com.lms.security.jwt.JwtAuthenticationEntryPoint;
import com.lms.security.jwt.JwtAuthenticationFilter;
import com.lms.platform.security.PlatformJwtAuthenticationFilter;
import com.lms.platform.runtime.TenantContextFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Application security configuration.
 *
 * <p>The backend is the real security boundary: every protected endpoint is
 * authorized here or with method security, independently of the frontend.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            ApiPaths.AUTH + "/login",
            ApiPaths.AUTH + "/refresh",
            ApiPaths.AUTH + "/forgot-password",
            ApiPaths.AUTH + "/reset-password",
            ApiPaths.AUTH + "/accept-invitation",
            ApiPaths.PLATFORM + "/auth/login",
            ApiPaths.WELL_KNOWN + "/**",   // JWKS discovery (public, returns no secret)
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /**
     * Paths handled exclusively by {@link ServiceKeyAuthFilter}.
     * They are marked as permitAll in the JWT security chain because
     * the service-key filter already enforces access control on them.
     */
    private static final String[] INTERNAL_ENDPOINTS = {
            ApiPaths.INTERNAL + "/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PlatformJwtAuthenticationFilter platformJwtAuthenticationFilter;
    private final TenantContextFilter tenantContextFilter;
    private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final WebEndpointProperties webEndpointProperties;
    private final InternalApiConfig internalApiConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String actuatorBase = webEndpointProperties.getBasePath();

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(INTERNAL_ENDPOINTS).permitAll() // guarded by ServiceKeyAuthFilter
                        .requestMatchers(ApiPaths.PLATFORM + "/**").hasAuthority("PLATFORM_ADMIN")
                        .requestMatchers(actuatorBase + "/health/**", actuatorBase + "/info").permitAll()
                        .requestMatchers(actuatorBase + "/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .authenticationProvider(daoAuthenticationProvider())
                // Establish tenant context first so both service-to-service and JWT requests
                // use the selected tenant database.
                .addFilterBefore(tenantContextFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(serviceKeyAuthFilter(), TenantContextFilter.class)
                .addFilterAfter(platformJwtAuthenticationFilter, ServiceKeyAuthFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, PlatformJwtAuthenticationFilter.class)
                // Must sit after the JWT filter: it inspects the principal that
                // filter establishes.
                .addFilterAfter(passwordChangeRequiredFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public ServiceKeyAuthFilter serviceKeyAuthFilter() {
        return new ServiceKeyAuthFilter(internalApiConfig);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
