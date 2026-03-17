package org.example.ensembled_project.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        String username,
        String role,
        Long employeeId
) {
}
