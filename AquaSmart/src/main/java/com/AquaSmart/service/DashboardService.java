package com.AquaSmart.service;

import java.util.List;

import com.AquaSmart.dto.AlertDto;
import com.AquaSmart.dto.AiProjectionDto;
import com.AquaSmart.dto.ChatResponseDto;
import com.AquaSmart.dto.DashboardStatusDto;
import com.AquaSmart.dto.PresenceStateDto;
import com.AquaSmart.dto.ValveHistoryDto;
import com.AquaSmart.dto.ValveStateDto;

public interface DashboardService {

    DashboardStatusDto getStatus();

    AiProjectionDto getProjection();

    List<AlertDto> getAlerts();

    AlertDto getAlertByIndex(int index);

    List<ValveHistoryDto> getValveHistory();

    ValveStateDto setValve(boolean open);

    PresenceStateDto setHomePresence(boolean home);

    ChatResponseDto askAi(String question);
}