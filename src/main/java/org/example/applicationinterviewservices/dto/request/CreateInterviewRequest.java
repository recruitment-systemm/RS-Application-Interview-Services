package org.example.applicationinterviewservices.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.applicationinterviewservices.entity.InterviewPhase;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateInterviewRequest(

        @NotNull(message = "Application ID is required")
        UUID applicationId,

        @NotNull(message = "Interview phase is required")
        InterviewPhase phase,

        OffsetDateTime scheduledAt
) { }