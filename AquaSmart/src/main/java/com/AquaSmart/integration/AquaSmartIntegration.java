package com.AquaSmart.integration;

import java.util.List;

import com.AquaSmart.dto.AlertDto;
import com.AquaSmart.dto.AiProjectionDto;
import com.AquaSmart.dto.DashboardStatusDto;

import com.AquaSmart.dto.ChatMessageDto;

public interface AquaSmartIntegration {

    DashboardStatusDto fallbackStatus(boolean isHome);

    AiProjectionDto fallbackProjection();

    List<AlertDto> fallbackAlerts();

    String answerQuestion(String question, String email, List<ChatMessageDto> history);
}