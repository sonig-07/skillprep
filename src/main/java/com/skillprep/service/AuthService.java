package com.skillprep.service;

import com.skillprep.dto.Dtos.*;
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
        if (isBlank(req.name()) || isBlank(req.email()) || isBlank(req.password())) {
            throw new IllegalArgumentException("Name, email and password are required.");
        }
        if (req.password().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));   // store the hash, never the password
        user.setExperienceLevel(req.experienceLevel() != null ? req.experienceLevel() : 0);
        user = userRepository.save(user);

        return toResponse(user, jwtUtil.createToken(user.getId()));
    }

    public AuthResponse signIn(SignInRequest req) {
        if (isBlank(req.email()) || isBlank(req.password())) {
            throw new IllegalArgumentException("Email and password are required.");
        }
        User user = userRepository.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return toResponse(user, jwtUtil.createToken(user.getId()));
    }

    public User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public AuthResponse me(String userId) {
        return toResponse(getUser(userId), null);
    }

    /** Saves the dark/light theme choice. */
    public AuthResponse updateProfile(String userId, ProfileRequest req) {
        User user = getUser(userId);
        if ("dark".equals(req.themePreference()) || "light".equals(req.themePreference())) {
            user.setThemePreference(req.themePreference());
            userRepository.save(user);
        }
        return toResponse(user, null);
    }

    private AuthResponse toResponse(User u, String token) {
        return new AuthResponse(token, u.getId(), u.getName(), u.getEmail(), u.getExperienceLevel(), u.getThemePreference());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
