package com.AquaSmart.controller;

import com.AquaSmart.dto.AlertDto;
import com.AquaSmart.model.*;
import com.AquaSmart.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/alerts")
public class AlertController {
    private static final Logger log = LoggerFactory.getLogger(AlertController.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AlertaRepository alertaRepository;
    private final MedidorRepository medidorRepository;
    private final TipoAlertaRepository tipoAlertaRepository;
    private final TipoEstadoRepository tipoEstadoRepository;

    public AlertController(AlertaRepository alertaRepository,
                           MedidorRepository medidorRepository,
                           TipoAlertaRepository tipoAlertaRepository,
                           TipoEstadoRepository tipoEstadoRepository) {
        this.alertaRepository = alertaRepository;
        this.medidorRepository = medidorRepository;
        this.tipoAlertaRepository = tipoAlertaRepository;
        this.tipoEstadoRepository = tipoEstadoRepository;
    }

    @PostMapping("/municipal")
    @Transactional
    public Object reportMunicipalLeak(@RequestBody Map<String, String> body) {
        try {
            String location = body.getOrDefault("location", "").trim();
            if (location.isBlank()) {
                return Map.of("error", "ValidationFailed", "message", "La ubicación es obligatoria.");
            }

            // Buscar primer medidor para asociarle la alerta obligatoria
            Optional<Medidor> firstMedidorOpt = medidorRepository.findAll().stream().findFirst();
            if (firstMedidorOpt.isEmpty()) {
                return Map.of("error", "MedidorNotFound", "message", "No hay medidores sembrados en la base de datos.");
            }
            Medidor medidor = firstMedidorOpt.get();

            // Buscar o crear TipoAlerta "Fuga"
            TipoAlerta tipoAlerta = tipoAlertaRepository.findAll().stream()
                    .filter(ta -> "Fuga".equalsIgnoreCase(ta.nombreTipoAlerta))
                    .findFirst()
                    .orElseGet(() -> tipoAlertaRepository.save(new TipoAlerta("Fuga")));

            // Buscar o crear TipoEstado "Pendiente"
            TipoEstado tipoEstado = tipoEstadoRepository.findAll().stream()
                    .filter(te -> "Pendiente".equalsIgnoreCase(te.nombreTipoEstado))
                    .findFirst()
                    .orElseGet(() -> tipoEstadoRepository.save(new TipoEstado("Pendiente")));

            // Guardar nueva alerta
            Alerta alerta = new Alerta(
                    LocalDate.now(),
                    LocalTime.now(),
                    "Fuga reportada en la vía pública: " + location,
                    tipoAlerta,
                    tipoEstado,
                    medidor
            );
            alerta = alertaRepository.save(alerta);

            String timestamp = LocalDateTime.of(alerta.fecha, alerta.hora).format(TIMESTAMP_FORMAT);
            return new AlertDto(
                    alerta.idAlerta,
                    true,
                    alerta.descripcion,
                    timestamp,
                    tipoAlerta.nombreTipoAlerta,
                    tipoEstado.nombreTipoEstado,
                    alerta.descripcion,
                    timestamp
            );
        } catch (Exception ex) {
            log.error("Error al reportar fuga municipal", ex);
            return Map.of("error", ex.getClass().getName(), "message", ex.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @Transactional
    public Object updateAlertStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String statusName = body.getOrDefault("status", "").trim();
            if (statusName.isBlank()) {
                return Map.of("error", "ValidationFailed", "message", "El estado es obligatorio.");
            }

            Optional<Alerta> alertaOpt = alertaRepository.findById(id);
            if (alertaOpt.isEmpty()) {
                return Map.of("error", "AlertNotFound", "message", "La alerta no existe.");
            }
            Alerta alerta = alertaOpt.get();

            // Buscar o crear el estado deseado dinámicamente
            TipoEstado tipoEstado = tipoEstadoRepository.findAll().stream()
                    .filter(te -> statusName.equalsIgnoreCase(te.nombreTipoEstado))
                    .findFirst()
                    .orElseGet(() -> tipoEstadoRepository.save(new TipoEstado(statusName)));

            alerta.tipoEstado = tipoEstado;
            alerta = alertaRepository.save(alerta);

            boolean active = !statusName.equalsIgnoreCase("Inactiva") 
                    && !statusName.equalsIgnoreCase("Cerrada") 
                    && !statusName.equalsIgnoreCase("Resuelta")
                    && !statusName.equalsIgnoreCase("Cumplido");

            String timestamp = LocalDateTime.of(alerta.fecha, alerta.hora).format(TIMESTAMP_FORMAT);
            return new AlertDto(
                    alerta.idAlerta,
                    active,
                    alerta.descripcion,
                    timestamp,
                    alerta.tipoAlerta != null ? alerta.tipoAlerta.nombreTipoAlerta : "Fuga",
                    tipoEstado.nombreTipoEstado,
                    alerta.descripcion,
                    timestamp
            );
        } catch (Exception ex) {
            log.error("Error al actualizar estado de la alerta", ex);
            return Map.of("error", ex.getClass().getName(), "message", ex.getMessage());
        }
    }
}
