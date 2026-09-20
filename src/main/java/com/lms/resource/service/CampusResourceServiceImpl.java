package com.lms.resource.service;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.service.StorageService;
import com.lms.resource.dto.request.CreateCampusResourceRequest;
import com.lms.resource.dto.request.PresignedResourceUploadUrlRequest;
import com.lms.resource.dto.request.UpdateCampusResourceRequest;
import com.lms.resource.dto.response.CampusResourceResponse;
import com.lms.resource.dto.response.PresignedResourceUploadUrlResponse;
import com.lms.resource.dto.response.ResourceDownloadResponse;
import com.lms.resource.entity.CampusResource;
import com.lms.resource.repository.CampusResourceRepository;
import com.lms.security.authentication.LmsUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampusResourceServiceImpl implements CampusResourceService {

    private final CampusResourceRepository resourceRepository;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public List<CampusResourceResponse> listResources(String category, String status, String search, LmsUserDetails principal) {
        boolean isAdminOrInstructor = principal != null &&
                (principal.getRoles().contains("ADMIN") ||
                 principal.getRoles().contains("SUPER_ADMIN") ||
                 principal.getRoles().contains("INSTRUCTOR"));

        String effectiveStatus = status;
        if (!isAdminOrInstructor || (status != null && status.equalsIgnoreCase("ALL"))) {
            if (!isAdminOrInstructor) {
                effectiveStatus = "PUBLISHED";
            } else {
                effectiveStatus = null; // show all statuses
            }
        }

        String effectiveCategory = (category != null && !category.equalsIgnoreCase("ALL")) ? category : null;
        String effectiveSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        List<CampusResource> resources;
        if (effectiveSearch != null && !effectiveSearch.isBlank()) {
            resources = resourceRepository.searchResources(effectiveCategory, effectiveStatus, effectiveSearch);
        } else if (effectiveCategory != null && effectiveStatus != null) {
            resources = resourceRepository.findByCategoryAndStatusOrderByCreatedAtDesc(effectiveCategory, effectiveStatus);
        } else if (effectiveStatus != null) {
            resources = resourceRepository.findByStatusOrderByCreatedAtDesc(effectiveStatus);
        } else if (effectiveCategory != null) {
            resources = resourceRepository.findByCategoryAndStatusOrderByCreatedAtDesc(effectiveCategory, "PUBLISHED");
        } else {
            resources = resourceRepository.findAllByOrderByCreatedAtDesc();
        }

        return resources.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CampusResourceResponse getResource(UUID id, LmsUserDetails principal) {
        CampusResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Campus resource not found"));
        return toResponse(resource);
    }

    @Override
    public PresignedResourceUploadUrlResponse generateUploadUrl(PresignedResourceUploadUrlRequest request, LmsUserDetails principal) {
        String categoryFolder = formatCategoryFolder(request.getCategory());
        String rawName = request.getFileName() != null ? request.getFileName() : "resource_file";
        String sanitizedName = rawName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = storageService.scopedKey(String.format("resources/%s/%s", categoryFolder, sanitizedName));

        String uploadUrl = storageService.generatePresignedUploadUrl(key, request.getContentType());
        String publicUrl = storageService.getPublicUrl(key);

        log.info("Generated R2 presigned upload URL for user {} with key {}", principal.getUserId(), key);

        return PresignedResourceUploadUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .fileKey(key)
                .publicUrl(publicUrl)
                .build();
    }

    @Override
    @Transactional
    public CampusResourceResponse createResource(CreateCampusResourceRequest request, LmsUserDetails principal) {
        boolean isAdmin = principal.getRoles().contains("ADMIN") || principal.getRoles().contains("SUPER_ADMIN");
        String authorRole = isAdmin ? "Admin" : "Instructor";

        String fileType = request.getFileType();
        if (fileType == null || fileType.isBlank()) {
            fileType = inferFileType(request.getFileName());
        }

        String fileSize = request.getFileSize();
        if ((fileSize == null || fileSize.isBlank()) && request.getFileSizeBytes() != null) {
            fileSize = formatFileSize(request.getFileSizeBytes());
        }

        CampusResource resource = CampusResource.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .fileKey(request.getFileKey())
                .fileName(request.getFileName())
                .fileType(fileType.toUpperCase())
                .fileSize(fileSize != null ? fileSize : "Unknown")
                .fileSizeBytes(request.getFileSizeBytes())
                .targetAudience(request.getTargetAudience() != null ? request.getTargetAudience() : "ALL_STUDENTS")
                .authorId(principal.getUserId())
                .authorName(principal.getName() != null ? principal.getName() : principal.getUsername())
                .authorRole(authorRole)
                .downloadsCount(0)
                .status(request.getStatus() != null ? request.getStatus() : "PUBLISHED")
                .storageProvider("CLOUDFLARE_R2")
                .build();

        CampusResource saved = resourceRepository.save(resource);
        log.info("Persisted campus resource {} ({}) by {}", saved.getId(), saved.getTitle(), principal.getUsername());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CampusResourceResponse updateResource(UUID id, UpdateCampusResourceRequest request, LmsUserDetails principal) {
        CampusResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Campus resource not found"));

        checkResourceEditAccess(resource, principal);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            resource.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            resource.setCategory(request.getCategory());
        }
        if (request.getTargetAudience() != null && !request.getTargetAudience().isBlank()) {
            resource.setTargetAudience(request.getTargetAudience());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            resource.setStatus(request.getStatus());
        }
        if (request.getFileKey() != null && !request.getFileKey().isBlank()) {
            resource.setFileKey(request.getFileKey());
        }
        if (request.getFileName() != null && !request.getFileName().isBlank()) {
            resource.setFileName(request.getFileName());
        }
        if (request.getFileType() != null && !request.getFileType().isBlank()) {
            resource.setFileType(request.getFileType().toUpperCase());
        }
        if (request.getFileSize() != null && !request.getFileSize().isBlank()) {
            resource.setFileSize(request.getFileSize());
        }
        if (request.getFileSizeBytes() != null) {
            resource.setFileSizeBytes(request.getFileSizeBytes());
        }

        CampusResource updated = resourceRepository.save(resource);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteResource(UUID id, LmsUserDetails principal) {
        CampusResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Campus resource not found"));

        checkResourceEditAccess(resource, principal);

        if (resource.getFileKey() != null && !resource.getFileKey().isBlank()) {
            storageService.deleteObject(resource.getFileKey());
        }

        resourceRepository.delete(resource);
        log.info("Deleted campus resource {} by {}", id, principal.getUsername());
    }

    @Override
    @Transactional
    public ResourceDownloadResponse prepareDownload(UUID id, LmsUserDetails principal) {
        CampusResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Campus resource not found"));

        resource.setDownloadsCount(resource.getDownloadsCount() + 1);
        resourceRepository.save(resource);

        String downloadUrl = storageService.generatePresignedGetUrl(resource.getFileKey(), resource.getFileName());
        if (downloadUrl == null) {
            downloadUrl = storageService.getPublicUrl(resource.getFileKey());
        }

        return ResourceDownloadResponse.builder()
                .downloadUrl(downloadUrl)
                .fileName(resource.getFileName())
                .fileType(resource.getFileType())
                .downloadsCount(resource.getDownloadsCount())
                .build();
    }

    @Override
    public PresignedResourceUploadUrlResponse uploadFileDirect(org.springframework.web.multipart.MultipartFile file, String category, LmsUserDetails principal) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED, "File must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "resource_" + UUID.randomUUID();
        }

        String sanitizedName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String catFolder = formatCategoryFolder(category);
        String key = storageService.scopedKey(String.format("resources/%s/%s", catFolder, sanitizedName));

        try {
            storageService.uploadFile(key, file.getInputStream(), file.getSize(), file.getContentType());
            log.info("Direct multipart upload completed for key {} by user {}", key, principal.getUsername());
        } catch (Exception e) {
            log.error("Failed to upload file stream to R2 for key {}", key, e);
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Failed to upload file to storage: " + e.getMessage());
        }

        String downloadUrl = storageService.generatePresignedGetUrl(key, sanitizedName);
        String publicUrl = storageService.getPublicUrl(key);

        return PresignedResourceUploadUrlResponse.builder()
                .fileKey(key)
                .uploadUrl(downloadUrl)
                .publicUrl(publicUrl != null ? publicUrl : downloadUrl)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CampusResource getResourceEntity(UUID id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Campus resource not found"));
    }

    private void checkResourceEditAccess(CampusResource resource, LmsUserDetails principal) {
        boolean isAdmin = principal.getRoles().contains("ADMIN") || principal.getRoles().contains("SUPER_ADMIN");
        if (isAdmin) {
            return;
        }

        if (resource.getAuthorId() == null || !resource.getAuthorId().equals(principal.getUserId())) {
            throw new ApplicationException(ErrorCode.ACCESS_DENIED, "You can only modify or delete your own uploaded resources");
        }
    }

    private CampusResourceResponse toResponse(CampusResource resource) {
        String downloadUrl = storageService.generatePresignedGetUrl(resource.getFileKey(), resource.getFileName());
        if (downloadUrl == null) {
            downloadUrl = storageService.getPublicUrl(resource.getFileKey());
        }

        return CampusResourceResponse.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .category(resource.getCategory())
                .fileKey(resource.getFileKey())
                .fileName(resource.getFileName())
                .fileType(resource.getFileType())
                .fileSize(resource.getFileSize())
                .fileSizeBytes(resource.getFileSizeBytes())
                .targetAudience(resource.getTargetAudience())
                .authorId(resource.getAuthorId())
                .authorName(resource.getAuthorName())
                .authorRole(resource.getAuthorRole())
                .downloadsCount(resource.getDownloadsCount())
                .status(resource.getStatus())
                .storageProvider(resource.getStorageProvider())
                .downloadUrl(downloadUrl)
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }

    private String inferFileType(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) return "DOC";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String formatCategoryFolder(String category) {
        if (category == null || category.isBlank()) return "guides";
        return switch (category.toUpperCase()) {
            case "CHEATSHEET" -> "cheatsheets";
            case "ACADEMIC_GUIDE" -> "guides";
            case "POLICY_EXAM" -> "policies";
            case "SOFTWARE_KIT" -> "software";
            case "TEMPLATE" -> "templates";
            default -> category.toLowerCase().replaceAll("[^a-z0-9_-]", "-");
        };
    }
}
