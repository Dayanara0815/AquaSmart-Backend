package com.AquaSmart.service.impl;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import com.AquaSmart.dto.NotificationSettingsDto;
import com.AquaSmart.service.SettingsService;

@Service
public class SettingsServiceImpl implements SettingsService {

    private final AtomicReference<NotificationSettingsDto> settings = new AtomicReference<>(
            new NotificationSettingsDto(true, true, true, "22:00", "08:00", true));

    @Override
    public NotificationSettingsDto getNotificationSettings() {
        return settings.get();
    }

    @Override
    public NotificationSettingsDto updateNotificationSettings(NotificationSettingsDto request) {
        NotificationSettingsDto current = settings.get();
        NotificationSettingsDto updated = new NotificationSettingsDto(
                request.airAlertsEnabled(),
                request.leakAlertsEnabled(),
                request.nightSilenceEnabled(),
                request.silentFrom() == null || request.silentFrom().isBlank() ? current.silentFrom() : request.silentFrom(),
                request.silentTo() == null || request.silentTo().isBlank() ? current.silentTo() : request.silentTo(),
                request.criticalOverrideEnabled());
        settings.set(updated);
        return updated;
    }
}