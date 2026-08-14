package com.neurowiki.service;

import com.neurowiki.dto.*;
import com.neurowiki.entity.User;
import com.neurowiki.exception.BadRequestException;
import com.neurowiki.exception.DuplicateResourceException;
import com.neurowiki.repository.UserRepository;
import com.neurowiki.security.JwtService;
import com.neurowiki.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityUtils securityUtils;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityUtils = securityUtils;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {}", savedUser.getUsername());

        String token = jwtService.generateToken(savedUser.getUsername());
        UserResponse userResponse = new UserResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());

        return new AuthResponse(token, userResponse);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        logger.info("Login successful for user: {}", user.getUsername());
        String token = jwtService.generateToken(user.getUsername());
        UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail());

        return new AuthResponse(token, userResponse);
    }

    public UserResponse getCurrentUser() {
        User user = securityUtils.getCurrentAuthenticatedUser();
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }

    public UserResponse updateProfile(ProfileUpdateRequest request) {
        User currentUser = securityUtils.getCurrentAuthenticatedUser();

        if (!currentUser.getUsername().equalsIgnoreCase(request.getUsername().trim()) &&
                userRepository.existsByUsername(request.getUsername().trim())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }

        if (!currentUser.getEmail().equalsIgnoreCase(request.getEmail().trim()) &&
                userRepository.existsByEmail(request.getEmail().trim())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        currentUser.setUsername(request.getUsername().trim());
        currentUser.setEmail(request.getEmail().trim().toLowerCase());
        User updated = userRepository.save(currentUser);

        return new UserResponse(updated.getId(), updated.getUsername(), updated.getEmail());
    }

    public void changePassword(PasswordChangeRequest request) {
        User currentUser = securityUtils.getCurrentAuthenticatedUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Current password does not match");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        logger.info("Password updated successfully for user: {}", currentUser.getUsername());
    }
}
