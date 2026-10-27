package com.lms.assessment.controller;

import com.lms.assessment.dto.response.SubmissionHistoryResponse;
import com.lms.assessment.service.SubmissionHistoryService;
import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.security.authentication.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.ADMIN_ASSESSMENTS + "/submissions")
@RequiredArgsConstructor
public class SubmissionHistoryController {
    private final SubmissionHistoryService history;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('INSTRUCTOR') or hasAuthority('ASSESSMENT_ANALYTICS_VIEW')")
    public ApiResponse<List<SubmissionHistoryResponse>> list() {
        var principal = AuthenticationService.requirePrincipal();
        boolean scoped = principal.getRoles().contains("INSTRUCTOR") && !principal.getRoles().contains("ADMIN")
                && !principal.getRoles().contains("SUPER_ADMIN");
        return ApiResponse.of(history.list(scoped ? principal.getUserId() : null));
    }
}
