package com.lms.student.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.student.dto.request.CreateBatchRequest;
import com.lms.student.dto.request.UpdateBatchRequest;
import com.lms.student.dto.response.BatchResponse;
import com.lms.student.entity.BatchStatus;
import com.lms.student.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

import java.util.UUID;

/**
 * REST API for batches — the dated cohorts learners enrol into.
 *
 * <p>A batch runs one course over a date range, which is the grouping unit for
 * a training centre. Learners may belong to several at once.
 */
@Tag(name = "Batches")
@RestController
@RequestMapping(ApiPaths.BATCHES)
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @Operation(summary = "List batches (paginated + filtered)")
    @GetMapping
    @PreAuthorize("hasAuthority('BATCH_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<BatchResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BatchStatus status,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.of(batchService.search(search, status, pageable)));
    }

    @Operation(summary = "Get a single batch by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BATCH_VIEW')")
    public ResponseEntity<ApiResponse<BatchResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(batchService.findById(id)));
    }

    @Operation(summary = "Schedule a new batch")
    @PostMapping
    @PreAuthorize("hasAuthority('BATCH_CREATE')")
    public ResponseEntity<ApiResponse<BatchResponse>> create(
            @Valid @RequestBody CreateBatchRequest request) {

        BatchResponse created = batchService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @Operation(summary = "Update a batch")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('BATCH_UPDATE')")
    public ResponseEntity<ApiResponse<BatchResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBatchRequest request) {

        return ResponseEntity.ok(ApiResponse.of(batchService.update(id, request)));
    }

    @Operation(summary = "Delete a batch with no learners in it")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BATCH_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        batchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
