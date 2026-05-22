package com.AquaSmart.dto;

public record ValveStateDto(
        boolean open,
        String message,
        String timestamp) {
}