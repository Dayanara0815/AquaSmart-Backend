package com.AquaSmart.service;

import com.AquaSmart.dto.NotificationSettingsDto;

public interface SettingsService {

    NotificationSettingsDto getNotificationSettings();

    NotificationSettingsDto updateNotificationSettings(NotificationSettingsDto request);
}