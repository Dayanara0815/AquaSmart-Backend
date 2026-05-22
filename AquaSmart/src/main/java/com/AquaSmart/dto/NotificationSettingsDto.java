package com.AquaSmart.dto;

public record NotificationSettingsDto(
        boolean airAlertsEnabled,
        boolean leakAlertsEnabled,
        boolean nightSilenceEnabled,
        String silentFrom,
        String silentTo,
        boolean criticalOverrideEnabled) {
}