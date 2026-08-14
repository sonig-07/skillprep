package com.skillprep.controller;

import com.skillprep.dto.AuthDtos.*;
import com.skillprep.security.JwtAuthFilter.AuthenticatedUser;
import com.skillprep.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public AuthResponse signUp(@Valid @RequestBody SignUpRequest req) {
        return authService.signUp(req);
    }

    @PostMapping("/signin")
    public AuthResponse signIn(@Valid @RequestBody SignInRequest req) {
        return authService.signIn(req);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        var u = authService.getUserOrThrow(user.userId());
        return new AuthResponse(null, u.getId(), u.getName(), u.getEmail(), u.getExperienceLevel(), u.getThemePreference());
    }

    @PutMapping("/profile")
    public AuthResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                       @RequestBody UpdateProfileRequest req) {
        return authService.updateProfile(user.userId(), req);
    }
}
