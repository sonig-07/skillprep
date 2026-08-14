package com.skillprep.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record SignUpRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String password,
            @Min(0) @Max(5) Integer experienceLevel
    ) {}

    public record SignInRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String token,
            String userId,
            String name,
            String email,
            Integer experienceLevel,
            String themePreference
    ) {}

    public record UpdateProfileRequest(
            @Min(0) @Max(5) Integer experienceLevel,
            String themePreference
    ) {}
}
