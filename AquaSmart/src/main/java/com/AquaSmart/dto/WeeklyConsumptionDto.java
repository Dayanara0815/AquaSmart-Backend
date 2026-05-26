package com.AquaSmart.dto;

public record WeeklyConsumptionDto(
        String date,
        String dayLabel,
        double liters,
        boolean anomaly) {
}