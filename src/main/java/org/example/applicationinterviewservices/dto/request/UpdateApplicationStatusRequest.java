package org.example.applicationinterviewservices.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.applicationinterviewservices.entity.ApplicationStatus;

public record UpdateApplicationStatusRequest(
        @NotNull(message = "Status is required")
        ApplicationStatus status
) { }
