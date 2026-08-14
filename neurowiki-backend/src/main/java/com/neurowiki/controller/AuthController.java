package com.neurowiki.controller;

import com.neurowiki.dto.*;
import com.neurowiki.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        UserResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        UserResponse response = authService.updateProfile(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
