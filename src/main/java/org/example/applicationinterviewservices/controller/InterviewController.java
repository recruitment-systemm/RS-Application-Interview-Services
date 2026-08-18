package org.example.applicationinterviewservices.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.applicationinterviewservices.dto.request.CompleteInterviewRequest;
import org.example.applicationinterviewservices.dto.request.CreateInterviewRequest;
import org.example.applicationinterviewservices.dto.response.ApiResponse;
import org.example.applicationinterviewservices.dto.response.InterviewResponse;
import org.example.applicationinterviewservices.service.InterviewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InterviewResponse> createInterview(@Valid @RequestBody CreateInterviewRequest request) {
        InterviewResponse response = interviewService.createInterview(request);
        return ApiResponse.success("Interview scheduled successfully", response);
    }

    @GetMapping("/{id}")
    public ApiResponse<InterviewResponse> getInterview(@PathVariable UUID id) {
        return ApiResponse.success("Interview retrieved successfully", interviewService.getInterview(id));
    }

    @GetMapping("/application/{applicationId}")
    public ApiResponse<List<InterviewResponse>> getApplicationInterviews(@PathVariable UUID applicationId) {
        return ApiResponse.success("Interviews retrieved successfully", interviewService.getApplicationInterviews(applicationId));
    }

    @GetMapping
    public ApiResponse<List<InterviewResponse>> getInterviews() {
        return ApiResponse.success("Interviews retrieved successfully", interviewService.getOrganizationInterviews());
    }

    @GetMapping("/interviewer/{interviewerId}")
    public ApiResponse<List<InterviewResponse>> getInterviewerInterviews(@PathVariable UUID interviewerId) {
        return ApiResponse.success("Interviews retrieved successfully", interviewService.getInterviewerInterviews(interviewerId));
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<InterviewResponse> completeInterview(@PathVariable UUID id, @Valid @RequestBody CompleteInterviewRequest request) {
        InterviewResponse response = interviewService.completeInterview(id, request);
        return ApiResponse.success("Interview completed successfully", response);
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<InterviewResponse> cancelInterview(@PathVariable UUID id) {
        InterviewResponse response = interviewService.cancelInterview(id);
        return ApiResponse.success("Interview cancelled successfully", response);
    }
}
