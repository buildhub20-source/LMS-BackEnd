package com.lms.platform.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.platform.dto.PlatformOverviewResponse;
import com.lms.platform.service.PlatformOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PLATFORM + "/overview")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformOverviewController {

    private final PlatformOverviewService overviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<PlatformOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.of(overviewService.getOverview()));
    }
}
