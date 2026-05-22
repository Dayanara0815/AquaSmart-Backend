package com.AquaSmart.dto;

public record ValveHistoryDto(
        String timestamp,
        String type,
        String status,
        int durationMinutes,
        String reason) {
}