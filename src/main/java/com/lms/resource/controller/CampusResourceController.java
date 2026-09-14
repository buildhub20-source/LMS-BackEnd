package com.lms.resource.controller;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.response.ApiResponse;
import com.lms.common.service.StorageService;
import com.lms.resource.dto.request.CreateCampusResourceRequest;
import com.lms.resource.dto.request.PresignedResourceUploadUrlRequest;
import com.lms.resource.dto.request.UpdateCampusResourceRequest;
import com.lms.resource.dto.response.CampusResourceResponse;
import com.lms.resource.dto.response.PresignedResourceUploadUrlResponse;
import com.lms.resource.dto.response.ResourceDownloadResponse;
import com.lms.resource.entity.CampusResource;
import com.lms.resource.service.CampusResourceService;
import com.lms.security.authentication.AuthenticationService;
import com.lms.security.authentication.LmsUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Tag(name = "Campus Resources", description = "Global campus study toolkits, cheatsheets, and academic guides")
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class CampusResourceController {

    private final CampusResourceService resourceService;
    private final StorageService storageService;

    @Operation(summary = "List campus resources with optional category, status, and search filters")
    @GetMapping
    @PreAuthorize("hasAuthority('RESOURCE_VIEW') or hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<CampusResourceResponse>>> listResources(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        List<CampusResourceResponse> results = resourceService.listResources(category, status, search, principal);
        return ResponseEntity.ok(ApiResponse.of(results));
    }

    @Operation(summary = "Get single campus resource by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RESOURCE_VIEW') or hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CampusResourceResponse>> getResource(@PathVariable UUID id) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        return ResponseEntity.ok(ApiResponse.of(resourceService.getResource(id, principal)));
    }

    @Operation(summary = "Generate presigned Cloudflare R2 upload URL for study resource")
    @PostMapping("/upload-url")
    @PreAuthorize("hasAuthority('RESOURCE_CREATE') or hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PresignedResourceUploadUrlResponse>> generateUploadUrl(
            @Valid @RequestBody PresignedResourceUploadUrlRequest request) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        PresignedResourceUploadUrlResponse response = resourceService.generateUploadUrl(request, principal);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "Direct multipart file upload to Cloudflare R2")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('RESOURCE_CREATE') or hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PresignedResourceUploadUrlResponse>> uploadDirect(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "category", required = false) String category) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        PresignedResourceUploadUrlResponse response = resourceService.uploadFileDirect(file, category, principal);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "Persist campus resource metadata after file upload")
    @PostMapping
    @PreAuthorize("hasAuthority('RESOURCE_CREATE') or hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CampusResourceResponse>> createResource(
            @Valid @RequestBody CreateCampusResourceRequest request) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        CampusResourceResponse created = resourceService.createResource(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @Operation(summary = "Update campus resource metadata")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('RESOURCE_UPDATE') or hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CampusResourceResponse>> updateResource(
            @PathVariable UUID id,
            @RequestBody UpdateCampusResourceRequest request) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        CampusResourceResponse updated = resourceService.updateResource(id, request, principal);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    @Operation(summary = "Delete campus resource and purge its Cloudflare R2 object")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('RESOURCE_DELETE') or hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable UUID id) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        resourceService.deleteResource(id, principal);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @Operation(summary = "Record download and retrieve presigned Cloudflare R2 download URL")
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('RESOURCE_VIEW') or hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ResourceDownloadResponse>> downloadResource(@PathVariable UUID id) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        ResourceDownloadResponse response = resourceService.prepareDownload(id, principal);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "Direct streaming download endpoint for campus resource file")
    @GetMapping("/{id}/stream")
    @PreAuthorize("hasAuthority('RESOURCE_VIEW') or hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<InputStreamResource> streamResource(@PathVariable UUID id) {
        CampusResource resource = resourceService.getResourceEntity(id);

        var s3Stream = storageService.getObjectStream(resource.getFileKey());
        if (s3Stream == null) {
            throw new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Resource file is not available in storage");
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (resource.getFileType() != null) {
            switch (resource.getFileType().toUpperCase()) {
                case "PDF" -> mediaType = MediaType.APPLICATION_PDF;
                case "ZIP" -> mediaType = MediaType.parseMediaType("application/zip");
                case "DOCX" -> mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                case "DOC" -> mediaType = MediaType.parseMediaType("application/msword");
                case "MD", "TXT" -> mediaType = MediaType.TEXT_PLAIN;
                case "JSON" -> mediaType = MediaType.APPLICATION_JSON;
                default -> mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(resource.getFileName(), StandardCharsets.UTF_8)
                .build());

        if (s3Stream.response() != null && s3Stream.response().contentLength() != null && s3Stream.response().contentLength() > 0) {
            headers.setContentLength(s3Stream.response().contentLength());
        } else if (resource.getFileSizeBytes() != null && resource.getFileSizeBytes() > 0) {
            headers.setContentLength(resource.getFileSizeBytes());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(s3Stream));
    }
}
