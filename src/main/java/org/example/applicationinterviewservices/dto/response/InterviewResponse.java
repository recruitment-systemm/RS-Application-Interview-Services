package org.example.applicationinterviewservices.dto.response;

import lombok.Builder;
import org.example.applicationinterviewservices.entity.InterviewPhase;
import org.example.applicationinterviewservices.entity.InterviewStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record InterviewResponse(
        UUID id,
        UUID applicationId,
        UUID interviewerId,
        InterviewPhase phase,
        InterviewStatus status,
        OffsetDateTime scheduledAt,
        String notes,
        Boolean passed,
        OffsetDateTime createdAt
) { }