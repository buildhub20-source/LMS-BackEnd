package com.lms.resource.service;

import com.lms.resource.dto.request.CreateCampusResourceRequest;
import com.lms.resource.dto.request.PresignedResourceUploadUrlRequest;
import com.lms.resource.dto.request.UpdateCampusResourceRequest;
import com.lms.resource.dto.response.CampusResourceResponse;
import com.lms.resource.dto.response.PresignedResourceUploadUrlResponse;
import com.lms.resource.dto.response.ResourceDownloadResponse;
import com.lms.resource.entity.CampusResource;
import com.lms.security.authentication.LmsUserDetails;

import java.util.List;
import java.util.UUID;

public interface CampusResourceService {

    List<CampusResourceResponse> listResources(String category, String status, String search, LmsUserDetails principal);

    CampusResourceResponse getResource(UUID id, LmsUserDetails principal);

    PresignedResourceUploadUrlResponse generateUploadUrl(PresignedResourceUploadUrlRequest request, LmsUserDetails principal);

    CampusResourceResponse createResource(CreateCampusResourceRequest request, LmsUserDetails principal);

    CampusResourceResponse updateResource(UUID id, UpdateCampusResourceRequest request, LmsUserDetails principal);

    void deleteResource(UUID id, LmsUserDetails principal);

    ResourceDownloadResponse prepareDownload(UUID id, LmsUserDetails principal);

    CampusResource getResourceEntity(UUID id);
}
