package com.familytree.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * JWT authentication response DTO
 */
@Data
@AllArgsConstructor
public class JwtAuthenticationResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private UserResponse user;

    public JwtAuthenticationResponse(String accessToken, UserResponse user) {
        this.accessToken = accessToken;
        this.user = user;
    }
}
