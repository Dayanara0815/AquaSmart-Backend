package com.AquaSmart.controller;

import com.AquaSmart.dto.NotificationSettingsDto;
import com.AquaSmart.service.SettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/settings/notifications")
    public NotificationSettingsDto getNotificationSettings() {
        return settingsService.getNotificationSettings();
    }

    @PutMapping("/settings/notifications")
    public NotificationSettingsDto updateNotificationSettings(@RequestBody NotificationSettingsDto request) {
        return settingsService.updateNotificationSettings(request);
    }
}