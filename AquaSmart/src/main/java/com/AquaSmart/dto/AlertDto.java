package com.AquaSmart.dto;

public record AlertDto(
        boolean active,
        String message,
        String schedule,
        String type,
        String state,
        String description,
        String timestamp) {
}