package org.example.applicationinterviewservices.dto.response;

import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID organizationId
) { }