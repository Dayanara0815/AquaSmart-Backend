package com.AquaSmart.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.AquaSmart.dto.AlertDto;
import com.AquaSmart.dto.AiProjectionDto;
import com.AquaSmart.dto.ChatResponseDto;
import com.AquaSmart.dto.DashboardStatusDto;
import com.AquaSmart.dto.PresenceStateDto;
import com.AquaSmart.dto.ValveHistoryDto;
import com.AquaSmart.dto.ValveStateDto;
import com.AquaSmart.integration.AquaSmartIntegration;
import com.AquaSmart.model.Alerta;
import com.AquaSmart.model.EstadoFacturaMensual;
import com.AquaSmart.model.EstadoValvula;
import com.AquaSmart.model.FacturaMensual;
import com.AquaSmart.model.LecturaConsumo;
import com.AquaSmart.model.Medidor;
import com.AquaSmart.model.TipoFlujo;
import com.AquaSmart.repository.AlertaRepository;
import com.AquaSmart.repository.EstadoFacturaMensualRepository;
import com.AquaSmart.repository.EstadoValvulaRepository;
import com.AquaSmart.repository.TipoFlujoRepository;
import com.AquaSmart.repository.FacturaMensualRepository;
import com.AquaSmart.repository.LecturaConsumoRepository;
import com.AquaSmart.repository.MedidorRepository;
import com.AquaSmart.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final BigDecimal TARIFA_POR_M3 = BigDecimal.valueOf(5.0);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MedidorRepository medidorRepository;
    private final LecturaConsumoRepository lecturaConsumoRepository;
    private final AlertaRepository alertaRepository;
    private final FacturaMensualRepository facturaMensualRepository;
    private final EstadoValvulaRepository estadoValvulaRepository;
    private final EstadoFacturaMensualRepository estadoFacturaMensualRepository;
    private final TipoFlujoRepository tipoFlujoRepository;
    private final AquaSmartIntegration aquaSmartIntegration;
    private final AtomicBoolean homePresence = new AtomicBoolean(true);
    private final CopyOnWriteArrayList<ValveHistoryEntry> valveHistory = new CopyOnWriteArrayList<>();

    public DashboardServiceImpl(
            MedidorRepository medidorRepository,
            LecturaConsumoRepository lecturaConsumoRepository,
            AlertaRepository alertaRepository,
            FacturaMensualRepository facturaMensualRepository,
            EstadoValvulaRepository estadoValvulaRepository,
            EstadoFacturaMensualRepository estadoFacturaMensualRepository,
            TipoFlujoRepository tipoFlujoRepository,
            AquaSmartIntegration aquaSmartIntegration) {
        this.medidorRepository = medidorRepository;
        this.lecturaConsumoRepository = lecturaConsumoRepository;
        this.alertaRepository = alertaRepository;
        this.facturaMensualRepository = facturaMensualRepository;
        this.estadoValvulaRepository = estadoValvulaRepository;
        this.estadoFacturaMensualRepository = estadoFacturaMensualRepository;
        this.tipoFlujoRepository = tipoFlujoRepository;
        this.aquaSmartIntegration = aquaSmartIntegration;
        seedValveHistory();
    }

    @Override
    @Transactional
    public DashboardStatusDto getStatus() {
        List<LecturaConsumo> lecturas = lecturaConsumoRepository.findAllWithDetails();

        if (lecturas.isEmpty()) {
            return aquaSmartIntegration.fallbackStatus(homePresence.get());
        }

        LecturaConsumo latestReading = lecturas.stream()
            .filter(lectura -> lectura.medidor != null)
            .max(Comparator.comparing((LecturaConsumo lectura) -> lectura.fecha)
                .thenComparing(lectura -> lectura.hora))
            .orElse(null);

        if (latestReading == null || latestReading.medidor == null) {
            return aquaSmartIntegration.fallbackStatus(homePresence.get());
        }

        Medidor medidor = latestReading.medidor;

        // --- SIMULADOR DE TELEMETRÍA DE FLUJO DE AGUA ---
        boolean valveOpen = medidor.estadoValvula != null
                && medidor.estadoValvula.nombreEstadoValvula != null
                && medidor.estadoValvula.nombreEstadoValvula.equalsIgnoreCase("Abierta");

        LocalDateTime lastDateTime = LocalDateTime.of(latestReading.fecha, latestReading.hora);
        LocalDateTime now = LocalDateTime.now();

        // Si han pasado más de 5 segundos, simulamos una nueva lectura
        if (lastDateTime.isBefore(now.minusSeconds(5))) {
            double value = 0.0;
            if (valveOpen) {
                if (homePresence.get()) {
                    // 40% de probabilidad de uso dinámico de agua (entre 2.0 y 11.5 litros/min)
                    value = Math.random() < 0.4 ? 2.0 + Math.random() * 9.5 : 0.0;
                } else {
                    // Si no hay nadie en casa, ver si hay alerta de fuga activa
                    boolean activeLeak = alertaRepository.findAllWithDetails().stream()
                            .anyMatch(a -> a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado)
                                    && a.tipoAlerta != null && "Fuga".equalsIgnoreCase(a.tipoAlerta.nombreTipoAlerta));
                    if (activeLeak) {
                        // Flujo constante de fuga (entre 1.6 y 2.1 litros/min)
                        value = 1.6 + Math.random() * 0.5;
                    }
                }
            }

            final double finalVal = value;
            TipoFlujo normalFlow = tipoFlujoRepository.findAll().stream()
                    .filter(tf -> "Normal".equalsIgnoreCase(tf.nombreTipoFlujo))
                    .findFirst()
                    .orElseGet(() -> tipoFlujoRepository.save(new TipoFlujo("Normal")));

            TipoFlujo anomaloFlow = tipoFlujoRepository.findAll().stream()
                    .filter(tf -> "Anómalo".equalsIgnoreCase(tf.nombreTipoFlujo))
                    .findFirst()
                    .orElseGet(() -> tipoFlujoRepository.save(new TipoFlujo("Anómalo")));

            boolean isLeak = valveOpen && !homePresence.get() && finalVal > 0.0;

            LecturaConsumo newReading = new LecturaConsumo(
                    now.toLocalDate(),
                    now.toLocalTime(),
                    BigDecimal.valueOf(finalVal),
                    isLeak ? anomaloFlow : normalFlow,
                    medidor
            );

            lecturaConsumoRepository.save(newReading);

            // Limpieza preventiva de datos simulados antiguos (> 7 días) para no inflar la DB
            try {
                LocalDate cutoff = now.toLocalDate().minusDays(7);
                List<LecturaConsumo> old = lecturaConsumoRepository.findAll().stream()
                        .filter(l -> l.fecha != null && l.fecha.isBefore(cutoff))
                        .toList();
                if (!old.isEmpty()) {
                    lecturaConsumoRepository.deleteAll(old);
                }
            } catch (Exception ex) {
                // Ignore silent cleanup errors
            }

            // Recargar lecturas para incluir la recién simulada
            lecturas = lecturaConsumoRepository.findAllWithDetails();
            latestReading = newReading;
        }

        // Si la válvula está cerrada, forzamos que el flujo reportado sea 0.0
        double currentFlow = 0.0;
        if (valveOpen && latestReading.volumenRegistrado != null) {
            currentFlow = latestReading.volumenRegistrado.doubleValue();
        }

        LocalDate today = LocalDate.now();
        BigDecimal litrosHoy = lecturas.stream()
            .filter(lectura -> lectura.medidor != null && medidor.idMedidor.equals(lectura.medidor.idMedidor))
            .filter(lectura -> today.equals(lectura.fecha))
                .map(lectura -> lectura.volumenRegistrado == null ? BigDecimal.ZERO : lectura.volumenRegistrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AlertDto alert;
        try {
            alert = getAlerts().stream().findFirst().orElse(null);
        } catch (Exception ex) {
            alert = aquaSmartIntegration.fallbackAlerts().stream().findFirst().orElse(null);
        }

        AiProjectionDto projection;
        try {
            projection = getProjection();
        } catch (Exception ex) {
            projection = aquaSmartIntegration.fallbackProjection();
        }
        String status = !valveOpen ? "Cerrado" : (currentFlow > 0 ? "En uso" : "Óptimo");
        String lastUpdated = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        return new DashboardStatusDto(
                status,
                roundTwoDecimals(litrosHoy),
                calculateCost(litrosHoy),
                roundTwoDecimals(BigDecimal.valueOf(currentFlow)),
                valveOpen,
                homePresence.get(),
                lastUpdated,
                alert,
                projection);
    }

    @Override
    @Transactional(readOnly = true)
    public AiProjectionDto getProjection() {
        List<FacturaMensual> facturas = facturaMensualRepository.findAll();
        if (facturas.isEmpty()) {
            return aquaSmartIntegration.fallbackProjection();
        }

        FacturaMensual factura = facturas.stream()
                .max(Comparator.comparing((FacturaMensual item) -> item.periodoFacturado))
                .orElse(facturas.get(0));

        double projectedBill = factura.montoPagar == null ? 0.0 : factura.montoPagar.doubleValue();
        int realConsumption = factura.consumoRealNeto == null ? 0 : factura.consumoRealNeto.intValue();
        int aiEstimate = factura.consumoBruto == null ? realConsumption : factura.consumoBruto.intValue();
        int leakDetected = factura.volumenAireDescontado == null ? 0 : factura.volumenAireDescontado.intValue();
        int baseConsumption = Math.max(0, realConsumption - leakDetected);

        return new AiProjectionDto(
                projectedBill,
                realConsumption,
                aiEstimate,
                leakDetected,
                leakDetected,
                baseConsumption,
                "Proyección calculada desde la factura mensual registrada en la base de datos.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> getAlerts() {
        List<Alerta> alertas = alertaRepository.findAllWithDetails();
        if (alertas.isEmpty()) {
            return aquaSmartIntegration.fallbackAlerts();
        }

        return alertas.stream().map(this::toAlertDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AlertDto getAlertByIndex(int index) {
        List<AlertDto> alerts = getAlerts();
        if (index < 0 || index >= alerts.size()) {
            throw new IllegalArgumentException("Alerta no encontrada");
        }

        return alerts.get(index);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValveHistoryDto> getValveHistory() {
        return valveHistory.stream().map(this::toValveHistoryDto).toList();
    }

    @Override
    @Transactional
    public ValveStateDto setValve(boolean open) {
        Optional<Medidor> medidorOpt = medidorRepository.findAll().stream().findFirst();

        if (medidorOpt.isPresent()) {
            Medidor medidor = medidorOpt.get();
            medidor.estadoValvula = resolveValveState(open);
            medidorRepository.save(medidor);

            // Registrar lectura instantánea de 0.0 si cerramos la válvula
            if (!open) {
                TipoFlujo normalFlow = tipoFlujoRepository.findAll().stream()
                        .filter(tf -> "Normal".equalsIgnoreCase(tf.nombreTipoFlujo))
                        .findFirst()
                        .orElseGet(() -> tipoFlujoRepository.save(new TipoFlujo("Normal")));
                LecturaConsumo closedReading = new LecturaConsumo(
                        LocalDate.now(),
                        LocalTime.now(),
                        BigDecimal.ZERO,
                        normalFlow,
                        medidor
                );
                lecturaConsumoRepository.save(closedReading);
            }
        }

        if (!open) {
            valveHistory.add(new ValveHistoryEntry(
                    LocalDateTime.now(),
                    "Cierre Manual",
                    "En curso",
                    0,
                    "Cierre activado desde la app para detener el flujo."));
        } else {
            ValveHistoryEntry last = valveHistory.stream()
                    .filter(entry -> "En curso".equalsIgnoreCase(entry.status))
                    .reduce((first, second) -> second)
                    .orElse(null);

            if (last != null) {
                last.status = "Cerrado";
                last.durationMinutes = 25;
                last.reason = "Reapertura manual tras restablecer la presión.";
            }
        }

        String message = open ? "✓ Válvula reabierta. Flujo normal restaurado" : "✓ Válvula cerrada";
        return new ValveStateDto(open, message, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    @Override
    @Transactional
    public PresenceStateDto setHomePresence(boolean home) {
        homePresence.set(home);
        String message = home ? "Presencia en casa activada" : "Presencia en casa desactivada";
        return new PresenceStateDto(home, message, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatResponseDto askAi(String question) {
        return new ChatResponseDto(aquaSmartIntegration.answerQuestion(question));
    }

    private AlertDto toAlertDto(Alerta alerta) {
        String type = alerta.tipoAlerta == null ? "" : alerta.tipoAlerta.nombreTipoAlerta;
        String state = alerta.tipoEstado == null ? "" : alerta.tipoEstado.nombreTipoEstado;
        String timestamp = (alerta.fecha == null || alerta.hora == null)
                ? ""
                : LocalDateTime.of(alerta.fecha, alerta.hora).format(TIMESTAMP_FORMAT);
        boolean active = state.isBlank() || !state.equalsIgnoreCase("Inactiva");

        return new AlertDto(
                active,
                alerta.descripcion,
                timestamp,
                type,
                state,
                alerta.descripcion,
                timestamp);
    }

    private EstadoValvula resolveValveState(boolean open) {
        String expected = open ? "Abierta" : "Cerrada";
        return estadoValvulaRepository.findAll().stream()
                .filter(estado -> expected.equalsIgnoreCase(estado.nombreEstadoValvula))
                .findFirst()
                .orElseGet(() -> estadoValvulaRepository.save(new EstadoValvula(expected)));
    }

    private double calculateCost(BigDecimal liters) {
        return liters
                .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                .multiply(TARIFA_POR_M3)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double roundTwoDecimals(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private ValveHistoryDto toValveHistoryDto(ValveHistoryEntry entry) {
        return new ValveHistoryDto(
                entry.timestamp.format(TIMESTAMP_FORMAT),
                entry.type,
                entry.status,
                entry.durationMinutes,
                entry.reason);
    }

    private void seedValveHistory() {
        if (!valveHistory.isEmpty()) {
            return;
        }

        valveHistory.add(new ValveHistoryEntry(
                LocalDateTime.now().minusHours(3),
                "Cierre Automático",
                "Cerrado",
                25,
                "Golpe de ariete detectado; se cerró la válvula para proteger la tubería."));
        valveHistory.add(new ValveHistoryEntry(
                LocalDateTime.now().minusDays(1).minusHours(2),
                "Cierre Manual",
                "Cerrado",
                18,
                "Cierre manual durante revisión preventiva del medidor."));
        valveHistory.add(new ValveHistoryEntry(
                LocalDateTime.now().minusDays(2),
                "Cierre Automático",
                "Cerrado",
                12,
                "Alta presión nocturna detectada por el sistema."));
    }

    private static class ValveHistoryEntry {
        private final LocalDateTime timestamp;
        private final String type;
        private String status;
        private int durationMinutes;
        private String reason;

        private ValveHistoryEntry(LocalDateTime timestamp, String type, String status, int durationMinutes, String reason) {
            this.timestamp = timestamp;
            this.type = type;
            this.status = status;
            this.durationMinutes = durationMinutes;
            this.reason = reason;
        }
    }
}