package org.example.ensembled_project.auth.dto;

public record LoginResult(
        LoginResponse loginResponse,
        String refreshToken
) {
}
