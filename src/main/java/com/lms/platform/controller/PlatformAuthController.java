package com.lms.platform.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.platform.dto.PlatformLoginRequest;
import com.lms.platform.dto.PlatformLoginResponse;
import com.lms.platform.service.PlatformAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PLATFORM + "/auth")
public class PlatformAuthController {
    private final PlatformAdminService service;
    public PlatformAuthController(PlatformAdminService service) { this.service = service; }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<PlatformLoginResponse>> login(@Valid @RequestBody PlatformLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.login(request)));
    }
}
