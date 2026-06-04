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
            String dateStr = body.get("date");
            String typeStr = body.getOrDefault("type", "Fuga").trim();
            String statusStr = body.getOrDefault("status", "Pendiente").trim();
            String customDesc = body.get("description");

            if (location.isBlank() && (customDesc == null || customDesc.isBlank())) {
                return Map.of("error", "ValidationFailed", "message", "La ubicación o descripción es obligatoria.");
            }

            LocalDate fecha = LocalDate.now();
            if (dateStr != null && !dateStr.isBlank()) {
                try {
                    fecha = LocalDate.parse(dateStr);
                } catch (Exception e) {
                    log.warn("Formato de fecha invalido, usando hoy: {}", dateStr);
                }
            }

            // Buscar primer medidor para asociarle la alerta obligatoria
            Optional<Medidor> firstMedidorOpt = medidorRepository.findAll().stream().findFirst();
            if (firstMedidorOpt.isEmpty()) {
                return Map.of("error", "MedidorNotFound", "message", "No hay medidores sembrados en la base de datos.");
            }
            Medidor medidor = firstMedidorOpt.get();

            // Buscar o crear TipoAlerta
            final String finalTypeStr = typeStr;
            TipoAlerta tipoAlerta = tipoAlertaRepository.findAll().stream()
                    .filter(ta -> finalTypeStr.equalsIgnoreCase(ta.nombreTipoAlerta))
                    .findFirst()
                    .orElseGet(() -> tipoAlertaRepository.save(new TipoAlerta(finalTypeStr)));

            // Buscar o crear TipoEstado
            final String finalStatusStr = statusStr;
            TipoEstado tipoEstado = tipoEstadoRepository.findAll().stream()
                    .filter(te -> finalStatusStr.equalsIgnoreCase(te.nombreTipoEstado))
                    .findFirst()
                    .orElseGet(() -> tipoEstadoRepository.save(new TipoEstado(finalStatusStr)));

            String descripcion = (customDesc != null && !customDesc.isBlank()) 
                    ? customDesc.trim() 
                    : ("Fuga reportada en la via publica: " + location);

            // Guardar nueva alerta
            Alerta alerta = new Alerta(
                    fecha,
                    LocalTime.now(),
                    descripcion,
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
