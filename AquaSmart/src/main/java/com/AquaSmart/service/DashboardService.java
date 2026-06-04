package com.AquaSmart.service;

import java.util.List;

import com.AquaSmart.dto.AlertDto;
import com.AquaSmart.dto.AiProjectionDto;
import com.AquaSmart.dto.ChatResponseDto;
import com.AquaSmart.dto.DashboardStatusDto;
import com.AquaSmart.dto.PresenceStateDto;
import com.AquaSmart.dto.ValveHistoryDto;
import com.AquaSmart.dto.ValveStateDto;
import com.AquaSmart.dto.ChatMessageDto;
import com.AquaSmart.dto.MedidorDto;
import com.AquaSmart.dto.AutoCloseStateDto;

public interface DashboardService {

    DashboardStatusDto getStatus(String email);

    AiProjectionDto getProjection(String email);

    List<AlertDto> getAlerts();

    AlertDto getAlertByIndex(int index);

    List<ValveHistoryDto> getValveHistory();

    ValveStateDto setValve(boolean open, String email);

    PresenceStateDto setHomePresence(boolean home, String email);

    ChatResponseDto askAi(String question, String email, List<ChatMessageDto> history);

    List<MedidorDto> getMedidores();

    AutoCloseStateDto setAutoClose(boolean autoClose, String email);
}