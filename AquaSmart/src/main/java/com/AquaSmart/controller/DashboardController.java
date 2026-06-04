package com.AquaSmart.controller;

import java.util.List;

import com.AquaSmart.dto.AlertDto;
import com.AquaSmart.dto.AiProjectionDto;
import com.AquaSmart.dto.ChatRequestDto;
import com.AquaSmart.dto.ChatResponseDto;
import com.AquaSmart.dto.DashboardStatusDto;
import com.AquaSmart.dto.PresenceCommandDto;
import com.AquaSmart.dto.PresenceStateDto;
import com.AquaSmart.dto.ValveHistoryDto;
import com.AquaSmart.dto.ValveCommandDto;
import com.AquaSmart.dto.ValveStateDto;
import com.AquaSmart.dto.MedidorDto;
import com.AquaSmart.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/water/status")
    public DashboardStatusDto getWaterStatus(@org.springframework.web.bind.annotation.RequestParam(required = false) String email) {
        return dashboardService.getStatus(email);
    }

    @GetMapping("/ai/projection")
    public AiProjectionDto getAiProjection(@org.springframework.web.bind.annotation.RequestParam(required = false) String email) {
        return dashboardService.getProjection(email);
    }

    @GetMapping("/water/medidores")
    public ResponseEntity<?> getMedidores() {
        try {
            return ResponseEntity.ok(dashboardService.getMedidores());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error in getMedidores: " + e.getMessage() + "\n" + java.util.Arrays.toString(e.getStackTrace()));
        }
    }

    @GetMapping("/alerts")
    public List<AlertDto> getAlerts() {
        return dashboardService.getAlerts();
    }

    @GetMapping("/alerts/{index}")
    public AlertDto getAlertByIndex(@org.springframework.web.bind.annotation.PathVariable int index) {
        return dashboardService.getAlertByIndex(index);
    }

    @GetMapping("/water/valve/history")
    public List<ValveHistoryDto> getValveHistory() {
        return dashboardService.getValveHistory();
    }

    @PutMapping("/water/valve")
    public ValveStateDto setValve(@RequestBody ValveCommandDto request, @org.springframework.web.bind.annotation.RequestParam(required = false) String email) {
        return dashboardService.setValve(request.open(), email);
    }

    @PutMapping("/water/presence")
    public PresenceStateDto setHomePresence(@RequestBody PresenceCommandDto request, @org.springframework.web.bind.annotation.RequestParam(required = false) String email) {
        return dashboardService.setHomePresence(request.home(), email);
    }

    @PutMapping("/water/autoclose")
    public com.AquaSmart.dto.AutoCloseStateDto setAutoClose(@RequestBody com.AquaSmart.dto.AutoCloseCommandDto request, @org.springframework.web.bind.annotation.RequestParam(required = false) String email) {
        return dashboardService.setAutoClose(request.autoClose(), email);
    }

    @PostMapping("/ai/chat")
    public ResponseEntity<ChatResponseDto> askAi(@RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(dashboardService.askAi(request.question(), request.email(), request.history()));
    }
}