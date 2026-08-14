package com.skillprep.service;

import com.skillprep.dto.AuthDtos.AuthResponse;
import com.skillprep.dto.AuthDtos.SignInRequest;
import com.skillprep.dto.AuthDtos.SignUpRequest;
import com.skillprep.dto.AuthDtos.UpdateProfileRequest;
import com.skillprep.model.User;
import com.skillprep.repository.UserRepository;
import com.skillprep.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse signUp(SignUpRequest req) {
        if (userRepository.existsByEmail(req.email().toLowerCase())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setExperienceLevel(req.experienceLevel() != null ? req.experienceLevel() : 0);
        user.setThemePreference("dark");
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return toAuthResponse(user, token);
    }

    public AuthResponse signIn(SignInRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return toAuthResponse(user, token);
    }

    public User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public AuthResponse updateProfile(String userId, UpdateProfileRequest req) {
        User user = getUserOrThrow(userId);
        if (req.experienceLevel() != null) user.setExperienceLevel(req.experienceLevel());
        if (req.themePreference() != null && (req.themePreference().equals("dark") || req.themePreference().equals("light"))) {
            user.setThemePreference(req.themePreference());
        }
        user = userRepository.save(user);
        return toAuthResponse(user, null);
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(),
                user.getExperienceLevel(), user.getThemePreference());
    }
}
