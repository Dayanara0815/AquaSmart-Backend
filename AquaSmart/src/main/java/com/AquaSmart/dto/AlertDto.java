package com.AquaSmart.dto;

public record AlertDto(
        Long id,
        boolean active,
        String message,
        String schedule,
        String type,
        String state,
        String description,
        String timestamp) {
}