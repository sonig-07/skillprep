package com.skillprep.controller;

import com.skillprep.dto.Dtos.*;
import com.skillprep.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public AuthResponse signUp(@RequestBody SignUpRequest req) {
        return authService.signUp(req);
    }

    @PostMapping("/signin")
    public AuthResponse signIn(@RequestBody SignInRequest req) {
        return authService.signIn(req);
    }

    
    @GetMapping("/me")
    public AuthResponse me(@RequestAttribute("userId") String userId) {
        return authService.me(userId);
    }

    @PutMapping("/profile")
    public AuthResponse updateProfile(@RequestAttribute("userId") String userId, @RequestBody ProfileRequest req) {
        return authService.updateProfile(userId, req);
    }
}
