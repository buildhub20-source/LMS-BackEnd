package com.lms.common.controller;

import com.lms.config.CertificateServiceConfig;
import com.lms.platform.runtime.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.UUID;

/** Same-origin student certificate gateway; the certificate service enforces ownership. */
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateProxyController {
    private final CertificateServiceConfig config;
    private final RestTemplate internalRestTemplate;

    @GetMapping
    public ResponseEntity<byte[]> list(HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        String query = "?page=" + Math.max(0, page) + "&size=" + Math.min(100, Math.max(1, size));
        return forward("/api/v1/certificates" + query, request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id, HttpServletRequest request) {
        return forward("/api/v1/certificates/" + id, request);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id, HttpServletRequest request) {
        return forward("/api/v1/certificates/" + id + "/download", request);
    }

    private ResponseEntity<byte[]> forward(String path, HttpServletRequest request) {
        if (!config.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Certificate service is not enabled");
        HttpHeaders headers = new HttpHeaders();
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        TenantContext.current().ifPresent(tenant -> headers.set("X-Tenant-Slug", tenant.slug()));
        try {
            var response = internalRestTemplate.exchange(config.getBaseUrl().replaceAll("/+$", "") + path,
                    HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
            HttpHeaders output = new HttpHeaders();
            output.setContentType(response.getHeaders().getContentType() == null ? MediaType.APPLICATION_JSON : response.getHeaders().getContentType());
            output.setCacheControl(CacheControl.noStore());
            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            if (disposition != null) output.set(HttpHeaders.CONTENT_DISPOSITION, disposition);
            return new ResponseEntity<>(response.getBody(), output, response.getStatusCode());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(ex.getResponseBodyAsByteArray());
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Certificate service is unavailable", ex);
        }
    }
}
