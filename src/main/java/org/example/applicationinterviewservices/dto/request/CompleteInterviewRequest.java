package org.example.applicationinterviewservices.dto.request;

import jakarta.validation.constraints.NotNull;

public record CompleteInterviewRequest(
        @NotNull(message = "Passed is required")
        Boolean passed,
        String notes
) {}