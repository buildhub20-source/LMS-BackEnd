package com.lms.coderunner;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.security.authentication.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** "Run" for coding questions in languages the browser cannot execute. */
@RestController
@RequestMapping(ApiPaths.CODE)
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CodeRunController {

    private final CodeRunService codeRunService;

    /** Lets the editor say up front whether server-side runs are available. */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(ApiResponse.of(Map.of("available", codeRunService.isAvailable())));
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<List<CodeRunResult>>> run(@Valid @RequestBody RunRequest request) {
        return ResponseEntity.ok(ApiResponse.of(codeRunService.run(
                AuthenticationService.requirePrincipal().getUserId(),
                request.language(), request.sourceCode(), request.inputs())));
    }

    public record RunRequest(
            @NotBlank @Pattern(regexp = "java|python|cpp|c|javascript") String language,
            @NotBlank @Size(max = 65_536) String sourceCode,
            @Size(max = 20) List<@Size(max = 65_536) String> inputs) {}
}
