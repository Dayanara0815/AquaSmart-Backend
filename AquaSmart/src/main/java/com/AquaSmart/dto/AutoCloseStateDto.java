package com.AquaSmart.dto;

public record AutoCloseStateDto(
        boolean autoClose,
        String message,
        String timestamp) {
}
