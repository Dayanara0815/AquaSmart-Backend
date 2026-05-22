package com.AquaSmart.dto;

public record ApiErrorResponse(
        String message,
        String timestamp) {
}