package com.familytree.controller;

import com.familytree.dto.request.LoginRequest;
import com.familytree.dto.request.RegisterRequest;
import com.familytree.dto.response.JwtAuthenticationResponse;
import com.familytree.dto.response.MessageResponse;
import com.familytree.dto.response.UserResponse;
import com.familytree.model.User;
import com.familytree.repository.UserRepository;
import com.familytree.security.UserPrincipal;
import com.familytree.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Authentication controller for user registration, login, and user info
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<JwtAuthenticationResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        JwtAuthenticationResponse response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user
     */
    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtAuthenticationResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    /**
     * Logout user (client-side token removal)
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        // JWT is stateless, so logout is handled on client side by removing token
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }
}
