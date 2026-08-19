package org.example.applicationinterviewservices.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.applicationinterviewservices.dto.request.CreateApplicationRequest;
import org.example.applicationinterviewservices.dto.request.UpdateApplicationStatusRequest;
import org.example.applicationinterviewservices.dto.response.ApiResponse;
import org.example.applicationinterviewservices.dto.response.ApplicationResponse;
import org.example.applicationinterviewservices.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationResponse> createApplication(@RequestParam UUID jobId, @RequestParam String firstName, @RequestParam String lastName, @RequestParam String email, @RequestParam(required = false) String phone, @RequestParam MultipartFile resume) {
        CreateApplicationRequest request = new CreateApplicationRequest(jobId, firstName, lastName, email, phone, resume);
        ApplicationResponse response = applicationService.createApplication(request);
        return ApiResponse.success(HttpStatus.CREATED.value(), "Application submitted successfully", response);
    }

    @GetMapping
    public ApiResponse<List<ApplicationResponse>> getApplications() {
        return ApiResponse.success(HttpStatus.OK.value(), "Applications retrieved successfully", applicationService.getApplications());
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<ApplicationResponse> getApplication(@PathVariable UUID applicationId) {
        return ApiResponse.success(HttpStatus.OK.value(), "Application retrieved successfully", applicationService.getApplication(applicationId));
    }

    @PatchMapping("/{applicationId}/status")
    public ApiResponse<ApplicationResponse> updateStatus(@PathVariable UUID applicationId, @Valid @RequestBody UpdateApplicationStatusRequest request) {
        ApplicationResponse response = applicationService.updateStatus(applicationId, request);
        return ApiResponse.success(HttpStatus.OK.value(), "Application status updated successfully", response);
    }
}
