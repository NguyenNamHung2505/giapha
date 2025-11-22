package com.familytree.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Generic message response DTO
 */
@Data
@AllArgsConstructor
public class MessageResponse {
    private String message;
}
