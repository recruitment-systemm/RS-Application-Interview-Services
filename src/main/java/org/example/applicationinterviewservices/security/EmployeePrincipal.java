package org.example.applicationinterviewservices.security;

import java.util.UUID;

public record EmployeePrincipal(
        UUID employeeId,
        UUID organizationId,
        String role,
        UUID sessionId
) { }