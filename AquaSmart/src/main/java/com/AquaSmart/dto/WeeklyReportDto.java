package com.AquaSmart.dto;

import java.util.List;

public record WeeklyReportDto(
        String from,
        String to,
        String period,
        double totalLiters,
        double averageLiters,
        String peakDay,
        double peakLiters,
        int anomalyCount,
        List<WeeklyConsumptionDto> dailyConsumption,
        String titularName,
        String medidorId) {
}