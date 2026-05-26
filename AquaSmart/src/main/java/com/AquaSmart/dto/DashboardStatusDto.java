package com.AquaSmart.dto;

public record DashboardStatusDto(
        String status,
        double litersToday,
        double costToday,
        double currentFlow,
        boolean valveOpen,
        boolean isHome,
        String lastUpdated,
        AlertDto alert,
        AiProjectionDto aiProjection) {
}