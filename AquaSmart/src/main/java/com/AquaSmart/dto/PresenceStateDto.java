package com.AquaSmart.dto;

public record PresenceStateDto(
        boolean home,
        String message,
        String timestamp) {
}