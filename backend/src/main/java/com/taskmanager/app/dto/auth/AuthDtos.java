package com.taskmanager.app.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record SignupRequest(@NotBlank String fullName, @Email String email, @NotBlank String password, String role) {}
    public record LoginRequest(@Email String email, @NotBlank String password) {}
    public record AuthResponse(String token, String fullName, String email, String role) {}
}
