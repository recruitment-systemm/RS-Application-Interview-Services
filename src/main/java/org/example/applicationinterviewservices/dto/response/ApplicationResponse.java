package org.example.applicationinterviewservices.dto.response;

import lombok.Builder;
import org.example.applicationinterviewservices.entity.ApplicationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record ApplicationResponse(
        UUID id,
        UUID jobId,
        UUID organizationId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String resumeUrl,
        ApplicationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
