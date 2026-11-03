package com.lms.common.controller;

import com.lms.config.CertificateServiceConfig;
import com.lms.platform.runtime.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

/**
 * Same-origin gateway for certificate template administration.
 *
 * <p>Separate from {@link CertificateProxyController}, which serves learners
 * and forwards GET only. Template setup needs POST, DELETE and a multipart
 * upload, and mixing those into the learner gateway would widen its surface
 * for no reason. Authorisation is enforced here as well as in the certificate
 * service, so a misconfigured service key cannot expose template management.
 */
@RestController
@RequestMapping("/api/v1/admin/certificate-templates")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'INSTRUCTOR')")
@RequiredArgsConstructor
public class CertificateTemplateProxyController {

    private static final String UPSTREAM = "/api/v1/admin/certificate-templates";

    private final CertificateServiceConfig config;
    private final RestTemplate internalRestTemplate;

    @GetMapping
    public ResponseEntity<byte[]> list(HttpServletRequest request) {
        return forward(UPSTREAM, HttpMethod.GET, null, request, MediaType.APPLICATION_JSON);
    }

    @PostMapping
    public ResponseEntity<byte[]> save(@RequestBody String body, HttpServletRequest request) {
        return forward(UPSTREAM, HttpMethod.POST, body, request, MediaType.APPLICATION_JSON);
    }

    @PostMapping("/{templateId}/publish")
    public ResponseEntity<byte[]> publish(@PathVariable UUID templateId, HttpServletRequest request) {
        return forward(UPSTREAM + "/" + templateId + "/publish", HttpMethod.POST, "", request,
                MediaType.APPLICATION_JSON);
    }

    @PostMapping("/{templateId}/unpublish")
    public ResponseEntity<byte[]> unpublish(@PathVariable UUID templateId, HttpServletRequest request) {
        return forward(UPSTREAM + "/" + templateId + "/unpublish", HttpMethod.POST, "", request,
                MediaType.APPLICATION_JSON);
    }

    /** Returns the rendered preview PDF rather than JSON. */
    @GetMapping("/{templateId}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable UUID templateId, HttpServletRequest request) {
        return forward(UPSTREAM + "/" + templateId + "/preview", HttpMethod.GET, null, request,
                MediaType.APPLICATION_PDF);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<byte[]> delete(@PathVariable UUID templateId, HttpServletRequest request) {
        return forward(UPSTREAM + "/" + templateId, HttpMethod.DELETE, null, request,
                MediaType.APPLICATION_JSON);
    }

    /**
     * Forwards the uploaded template PDF. The parts are rebuilt rather than
     * streamed through, because the upstream expects a JSON {@code template}
     * part alongside the file and the original request's boundaries are not
     * reusable once Spring has parsed them.
     */
    @PostMapping(value = "/institution-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> uploadInstitutionPdf(@RequestPart("template") String template,
                                                       @RequestPart("file") MultipartFile file,
                                                       HttpServletRequest request) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a PDF to upload");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }

        HttpHeaders jsonPart = new HttpHeaders();
        jsonPart.setContentType(MediaType.APPLICATION_JSON);

        HttpHeaders filePart = new HttpHeaders();
        filePart.setContentType(MediaType.APPLICATION_PDF);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("template", new HttpEntity<>(template, jsonPart));
        parts.add("file", new HttpEntity<>(new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename() == null ? "template.pdf" : file.getOriginalFilename();
            }
        }, filePart));

        HttpHeaders headers = authHeaders(request);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return send(UPSTREAM + "/institution-pdf", HttpMethod.POST, new HttpEntity<>(parts, headers),
                MediaType.APPLICATION_JSON);
    }

    // --- Internal ---

    private ResponseEntity<byte[]> forward(String path, HttpMethod method, String body,
                                           HttpServletRequest request, MediaType expected) {
        HttpHeaders headers = authHeaders(request);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return send(path, method, new HttpEntity<>(body, headers), expected);
    }

    private HttpHeaders authHeaders(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        TenantContext.current().ifPresent(tenant -> headers.set("X-Tenant-Slug", tenant.slug()));
        return headers;
    }

    private ResponseEntity<byte[]> send(String path, HttpMethod method, HttpEntity<?> entity, MediaType expected) {
        if (!config.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Certificate service is not enabled");
        }
        try {
            var response = internalRestTemplate.exchange(
                    config.getBaseUrl().replaceAll("/+$", "") + path, method, entity, byte[].class);
            HttpHeaders output = new HttpHeaders();
            output.setContentType(response.getHeaders().getContentType() == null
                    ? expected : response.getHeaders().getContentType());
            return new ResponseEntity<>(response.getBody(), output, response.getStatusCode());
        } catch (RestClientResponseException ex) {
            // Pass the upstream's own message through; it explains why a
            // publish was refused far better than a generic 502 would.
            HttpHeaders output = new HttpHeaders();
            output.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(ex.getResponseBodyAsByteArray(), output,
                    HttpStatus.valueOf(ex.getStatusCode().value()));
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Certificate service is unreachable");
        }
    }
}
