package org.example.applicationinterviewservices.security;

import org.example.applicationinterviewservices.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    public static EmployeePrincipal currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof EmployeePrincipal principal)) {
            throw new UnauthorizedException("Authentication is required for this operation");
        }
        return principal;
    }
}
