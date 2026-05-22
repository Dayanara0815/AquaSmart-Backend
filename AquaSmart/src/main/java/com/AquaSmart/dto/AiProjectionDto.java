package com.AquaSmart.dto;

public record AiProjectionDto(
        double projectedBill,
        int realConsumption,
        int aiEstimate,
        int leakDetected,
        int leakEstimate,
        int baseConsumption,
        String aiMessage) {
}