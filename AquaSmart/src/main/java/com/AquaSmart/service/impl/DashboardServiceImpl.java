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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.AquaSmart.dto.AlertDto;
import com.AquaSmart.dto.AiProjectionDto;
import com.AquaSmart.dto.ChatResponseDto;
import com.AquaSmart.dto.DashboardStatusDto;
import com.AquaSmart.dto.PresenceStateDto;
import com.AquaSmart.dto.ValveHistoryDto;
import com.AquaSmart.dto.ValveStateDto;
import com.AquaSmart.dto.ChatMessageDto;
import com.AquaSmart.dto.MedidorDto;
import com.AquaSmart.integration.AquaSmartIntegration;
import com.AquaSmart.model.Alerta;
import com.AquaSmart.model.EstadoFacturaMensual;
import com.AquaSmart.model.EstadoValvula;
import com.AquaSmart.model.FacturaMensual;
import com.AquaSmart.model.LecturaConsumo;
import com.AquaSmart.model.Medidor;
import com.AquaSmart.model.TipoFlujo;
import com.AquaSmart.model.Titular;
import com.AquaSmart.repository.AlertaRepository;
import com.AquaSmart.repository.EstadoFacturaMensualRepository;
import com.AquaSmart.repository.EstadoValvulaRepository;
import com.AquaSmart.repository.TipoFlujoRepository;
import com.AquaSmart.repository.TipoAlertaRepository;
import com.AquaSmart.repository.TipoEstadoRepository;
import com.AquaSmart.repository.FacturaMensualRepository;
import com.AquaSmart.repository.LecturaConsumoRepository;
import com.AquaSmart.repository.MedidorRepository;
import com.AquaSmart.repository.TitularRepository;
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
    private final TipoAlertaRepository tipoAlertaRepository;
    private final TipoEstadoRepository tipoEstadoRepository;
    private final AquaSmartIntegration aquaSmartIntegration;
    private final TitularRepository titularRepository;
    private final com.AquaSmart.repository.PreferenciaRepository preferenciaRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final CopyOnWriteArrayList<ValveHistoryEntry> valveHistory = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> continuousFlowCounters = new ConcurrentHashMap<>();

    public DashboardServiceImpl(
            MedidorRepository medidorRepository,
            LecturaConsumoRepository lecturaConsumoRepository,
            AlertaRepository alertaRepository,
            FacturaMensualRepository facturaMensualRepository,
            EstadoValvulaRepository estadoValvulaRepository,
            EstadoFacturaMensualRepository estadoFacturaMensualRepository,
            TipoFlujoRepository tipoFlujoRepository,
            TipoAlertaRepository tipoAlertaRepository,
            TipoEstadoRepository tipoEstadoRepository,
            AquaSmartIntegration aquaSmartIntegration,
            TitularRepository titularRepository,
            com.AquaSmart.repository.PreferenciaRepository preferenciaRepository,
            jakarta.persistence.EntityManager entityManager) {
        this.medidorRepository = medidorRepository;
        this.lecturaConsumoRepository = lecturaConsumoRepository;
        this.alertaRepository = alertaRepository;
        this.facturaMensualRepository = facturaMensualRepository;
        this.estadoValvulaRepository = estadoValvulaRepository;
        this.estadoFacturaMensualRepository = estadoFacturaMensualRepository;
        this.tipoFlujoRepository = tipoFlujoRepository;
        this.tipoAlertaRepository = tipoAlertaRepository;
        this.tipoEstadoRepository = tipoEstadoRepository;
        this.aquaSmartIntegration = aquaSmartIntegration;
        this.titularRepository = titularRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.entityManager = entityManager;
        seedValveHistory();
    }

    private boolean getHomePresence(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true;
        }
        Optional<Titular> titularOpt = titularRepository.findAll().stream()
                .filter(t -> email.trim().equalsIgnoreCase(t.correo))
                .findFirst();
        if (titularOpt.isPresent()) {
            Optional<com.AquaSmart.model.PreferenciaUsuario> prefOpt = preferenciaRepository.findByTitular(titularOpt.get());
            if (prefOpt.isPresent() && prefOpt.get().presenciaCasa != null) {
                return prefOpt.get().presenciaCasa;
            }
        }
        return true;
    }

    private boolean getAutoClosePreference(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true;
        }
        Optional<Titular> titularOpt = titularRepository.findAll().stream()
                .filter(t -> email.trim().equalsIgnoreCase(t.correo))
                .findFirst();
        if (titularOpt.isPresent()) {
            Optional<com.AquaSmart.model.PreferenciaUsuario> prefOpt = preferenciaRepository.findByTitular(titularOpt.get());
            if (prefOpt.isPresent() && prefOpt.get().autoCierreFuga != null) {
                return prefOpt.get().autoCierreFuga;
            }
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatusDto getStatus(String email) {
        entityManager.clear();
        Optional<Medidor> medidorOpt = Optional.empty();
        if (email != null && !email.trim().isEmpty() && !email.equals("undefined")) {
            Optional<Titular> titularOpt = titularRepository.findAll().stream()
                    .filter(t -> email.trim().equalsIgnoreCase(t.correo))
                    .findFirst();
            if (titularOpt.isPresent()) {
                medidorOpt = medidorRepository.findByTitular(titularOpt.get());
            }
        }
        if (medidorOpt.isEmpty()) {
            medidorOpt = medidorRepository.findAllWithDetails().stream().findFirst();
        }

        final Optional<Medidor> finalMedidorSearch = medidorOpt;
        List<LecturaConsumo> lecturas = lecturaConsumoRepository.findAllWithDetails();

        if (lecturas.isEmpty()) {
            return aquaSmartIntegration.fallbackStatus(getHomePresence(email));
        }

        LecturaConsumo latestReading = null;
        if (finalMedidorSearch.isPresent()) {
            String medId = finalMedidorSearch.get().idMedidor;
            latestReading = lecturas.stream()
                .filter(lectura -> lectura.medidor != null && medId.equals(lectura.medidor.idMedidor) && lectura.fecha != null && lectura.hora != null)
                .max(Comparator.comparing((LecturaConsumo lectura) -> lectura.fecha)
                    .thenComparing(lectura -> lectura.hora))
                .orElse(null);
        }

        if (latestReading == null) {
            latestReading = lecturas.stream()
                .filter(lectura -> lectura.medidor != null && lectura.fecha != null && lectura.hora != null)
                .max(Comparator.comparing((LecturaConsumo lectura) -> lectura.fecha)
                    .thenComparing(lectura -> lectura.hora))
                .orElse(null);
        }

        if (latestReading == null || latestReading.medidor == null) {
            return aquaSmartIntegration.fallbackStatus(getHomePresence(email));
        }

        Medidor medidor = finalMedidorSearch.orElse(latestReading.medidor);
        System.out.println("DEBUG getStatus: email=" + email + ", medidorId=" + medidor.idMedidor + ", finalSearchPresent=" + finalMedidorSearch.isPresent());
        if (medidor.estadoValvula != null) {
            System.out.println("DEBUG getStatus: estadoValvulaId=" + medidor.estadoValvula.idEstadoValvula + ", nombre=" + medidor.estadoValvula.nombreEstadoValvula);
        } else {
            System.out.println("DEBUG getStatus: estadoValvula is null!");
        }

        boolean valveOpen = medidor.estadoValvula != null
                && medidor.estadoValvula.nombreEstadoValvula != null
                && medidor.estadoValvula.nombreEstadoValvula.equalsIgnoreCase("Abierta");
        System.out.println("DEBUG getStatus: evaluated valveOpen=" + valveOpen);

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
            projection = getProjection(email);
        } catch (Exception ex) {
            projection = aquaSmartIntegration.fallbackProjection();
        }
        String status = !valveOpen ? "Cerrado" : (currentFlow > 0 ? "En uso" : "Óptimo");
        String lastUpdated = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        long count = medidorRepository.count();
        return new DashboardStatusDto(
                status,
                roundTwoDecimals(litrosHoy),
                calculateCost(litrosHoy),
                roundTwoDecimals(BigDecimal.valueOf(currentFlow)),
                valveOpen,
                getHomePresence(email),
                getAutoClosePreference(email),
                lastUpdated,
                alert,
                projection,
                count);
    }

    @Scheduled(fixedRate = 6000)
    @Transactional
    public void simulateSensorReadings() {
        entityManager.clear();
        List<Medidor> medidores = medidorRepository.findAllWithDetails();
        LocalDateTime now = LocalDateTime.now();

        // Encontrar o crear tipos comunes fuera del bucle para optimizar
        TipoFlujo normalFlow = tipoFlujoRepository.findAll().stream()
                .filter(tf -> "Normal".equalsIgnoreCase(tf.nombreTipoFlujo))
                .findFirst()
                .orElseGet(() -> tipoFlujoRepository.save(new TipoFlujo("Normal")));

        TipoFlujo anomaloFlow = tipoFlujoRepository.findAll().stream()
                .filter(tf -> "Anómalo".equalsIgnoreCase(tf.nombreTipoFlujo))
                .findFirst()
                .orElseGet(() -> tipoFlujoRepository.save(new TipoFlujo("Anómalo")));

        for (Medidor medidor : medidores) {
            boolean valveOpen = medidor.estadoValvula != null
                    && medidor.estadoValvula.nombreEstadoValvula != null
                    && medidor.estadoValvula.nombreEstadoValvula.equalsIgnoreCase("Abierta");

            double value = 0.0;
            boolean newLeakDetected = false;
            boolean newContinuousFlowAlertDetected = false;

            if (valveOpen) {
                // Comportamiento diferenciado según rol del titular
                boolean isComercio = medidor.titular != null && "COMERCIO".equalsIgnoreCase(medidor.titular.rol);
                String email = (medidor.titular != null) ? medidor.titular.correo : null;
                boolean isUserAtHome = getHomePresence(email);

                if (isComercio) {
                    // Comercio: Luis Condori
                    boolean activeLeak = alertaRepository.findAllWithDetails().stream()
                            .anyMatch(a -> a.medidor != null && medidor.idMedidor.equals(a.medidor.idMedidor)
                                     && a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado)
                                     && a.tipoAlerta != null && "Fuga".equalsIgnoreCase(a.tipoAlerta.nombreTipoAlerta));
                    if (activeLeak) {
                        value = 5.0 + Math.random() * 7.0;
                    } else {
                        if (Math.random() < 0.10) {
                            value = 5.0 + Math.random() * 7.0;
                            newLeakDetected = true;
                        } else {
                            value = Math.random() < 0.6 ? 5.0 + Math.random() * 15.0 : 0.0;
                        }
                    }
                } else {
                    // Doméstico: María Fernanda
                    if (isUserAtHome) {
                        value = Math.random() < 0.4 ? 2.0 + Math.random() * 9.5 : 0.0;
                        
                        if (value > 0.0) {
                            int cycles = continuousFlowCounters.merge(medidor.idMedidor, 1, Integer::sum);
                            if (cycles >= 5) {
                                newContinuousFlowAlertDetected = true;
                            }
                        } else {
                            continuousFlowCounters.put(medidor.idMedidor, 0);
                        }
                    } else {
                        boolean activeLeak = alertaRepository.findAllWithDetails().stream()
                                .anyMatch(a -> a.medidor != null && medidor.idMedidor.equals(a.medidor.idMedidor)
                                        && a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado)
                                        && a.tipoAlerta != null && "Fuga".equalsIgnoreCase(a.tipoAlerta.nombreTipoAlerta));
                        if (activeLeak) {
                            value = 1.6 + Math.random() * 0.5;
                        } else {
                            if (Math.random() < 0.15) {
                                value = 0.5 + Math.random() * 2.5;
                                newLeakDetected = true;
                            } else {
                                value = 0.0;
                            }
                        }
                        continuousFlowCounters.put(medidor.idMedidor, 0);
                    }
                }
            } else {
                value = 0.0;
                continuousFlowCounters.put(medidor.idMedidor, 0);
            }

            String email = (medidor.titular != null) ? medidor.titular.correo : null;
            boolean isUserAtHome = getHomePresence(email);

            boolean hasLeak = newLeakDetected || newContinuousFlowAlertDetected || alertaRepository.findAllWithDetails().stream()
                    .anyMatch(a -> a.medidor != null && medidor.idMedidor.equals(a.medidor.idMedidor)
                             && a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado)
                             && a.tipoAlerta != null && "Fuga".equalsIgnoreCase(a.tipoAlerta.nombreTipoAlerta));

            if (valveOpen && hasLeak) {
                boolean autoClosePref = getAutoClosePreference(email);
                if (!isUserAtHome || autoClosePref) {
                    medidor.estadoValvula = resolveValveState(false);
                    medidorRepository.saveAndFlush(medidor);
                    valveOpen = false; // update local variable for this loop
                    valveHistory.add(new ValveHistoryEntry(
                            LocalDateTime.now(),
                            "Cierre Automático",
                            "Cerrado",
                            0,
                            "Cierre automático por detección de fuga (" + (isUserAtHome ? "Usuario en casa con cierre automático activado" : "Usuario fuera de casa") + ")."
                    ));
                    value = 0.0;
                    System.out.println("DEBUG simulateSensorReadings: Closed valve automatically for medidor " + medidor.idMedidor);
                }
            }

            final double finalVal = value;
            boolean isLeak = valveOpen && (newLeakDetected || (medidor.titular != null && !"COMERCIO".equalsIgnoreCase(medidor.titular.rol) && !isUserAtHome && finalVal > 0.0));

            LecturaConsumo newReading = new LecturaConsumo(
                    now.toLocalDate(),
                    now.toLocalTime(),
                    BigDecimal.valueOf(finalVal),
                    isLeak ? anomaloFlow : normalFlow,
                    medidor
            );

            lecturaConsumoRepository.save(newReading);

            // Evaluar y guardar alertas
            if (newLeakDetected) {
                boolean exists = alertaRepository.findAllWithDetails().stream()
                        .anyMatch(a -> a.medidor != null && medidor.idMedidor.equals(a.medidor.idMedidor)
                                && a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado)
                                && a.tipoAlerta != null && "Fuga".equalsIgnoreCase(a.tipoAlerta.nombreTipoAlerta)
                                && (a.descripcion.contains("atípico") || a.descripcion.contains("anómalo")));
                if (!exists) {
                    com.AquaSmart.model.TipoAlerta tipoAlerta = tipoAlertaRepository.findAll().stream()
                            .filter(ta -> "Fuga".equalsIgnoreCase(ta.nombreTipoAlerta))
                            .findFirst()
                            .orElseGet(() -> tipoAlertaRepository.save(new com.AquaSmart.model.TipoAlerta("Fuga")));

                    com.AquaSmart.model.TipoEstado tipoEstado = tipoEstadoRepository.findAll().stream()
                            .filter(te -> "Activa".equalsIgnoreCase(te.nombreTipoEstado))
                            .findFirst()
                            .orElseGet(() -> tipoEstadoRepository.save(new com.AquaSmart.model.TipoEstado("Activa")));

                    String desc = (medidor.titular != null && "COMERCIO".equalsIgnoreCase(medidor.titular.rol))
                            ? String.format(java.util.Locale.US, "Análisis IA: Se detectó un consumo comercial atípico de %.2f L/min en la lavandería.", finalVal)
                            : String.format(java.util.Locale.US, "Análisis IA: Se detectó un consumo anómalo de %.2f L/min mientras el usuario se encontraba fuera de casa.", finalVal);

                    Alerta newAlert = new Alerta(
                            now.toLocalDate(),
                            now.toLocalTime(),
                            desc,
                            tipoAlerta,
                            tipoEstado,
                            medidor
                    );
                    alertaRepository.save(newAlert);
                }
            }

            if (newContinuousFlowAlertDetected) {
                boolean exists = alertaRepository.findAllWithDetails().stream()
                        .anyMatch(a -> a.medidor != null && medidor.idMedidor.equals(a.medidor.idMedidor)
                                && a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado)
                                && a.tipoAlerta != null && "Fuga".equalsIgnoreCase(a.tipoAlerta.nombreTipoAlerta)
                                && a.descripcion.contains("flujo continuo inusual"));
                if (!exists) {
                    com.AquaSmart.model.TipoAlerta tipoAlerta = tipoAlertaRepository.findAll().stream()
                            .filter(ta -> "Fuga".equalsIgnoreCase(ta.nombreTipoAlerta))
                            .findFirst()
                            .orElseGet(() -> tipoAlertaRepository.save(new com.AquaSmart.model.TipoAlerta("Fuga")));

                    com.AquaSmart.model.TipoEstado tipoEstado = tipoEstadoRepository.findAll().stream()
                            .filter(te -> "Activa".equalsIgnoreCase(te.nombreTipoEstado))
                            .findFirst()
                            .orElseGet(() -> tipoEstadoRepository.save(new com.AquaSmart.model.TipoEstado("Activa")));

                    Alerta newAlert = new Alerta(
                            now.toLocalDate(),
                            now.toLocalTime(),
                            "Análisis IA: Tu patrón de consumo indica un flujo continuo inusual. Solicitamos revisar si dejó un caño abierto.",
                            tipoAlerta,
                            tipoEstado,
                            medidor
                    );
                    alertaRepository.save(newAlert);
                }
            }
        }

        // Limpieza de datos antiguos
        try {
            LocalDate cutoff = now.toLocalDate().minusDays(45);
            List<LecturaConsumo> old = lecturaConsumoRepository.findAll().stream()
                    .filter(l -> l.fecha != null && l.fecha.isBefore(cutoff))
                    .toList();
            if (!old.isEmpty()) {
                lecturaConsumoRepository.deleteAll(old);
            }
        } catch (Exception ex) {
            // Ignorar errores silenciosos de limpieza
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AiProjectionDto getProjection(String email) {
        Optional<Medidor> medidorOpt = Optional.empty();
        if (email != null && !email.trim().isEmpty()) {
            Optional<Titular> titularOpt = titularRepository.findAll().stream()
                    .filter(t -> email.trim().equalsIgnoreCase(t.correo))
                    .findFirst();
            if (titularOpt.isPresent()) {
                medidorOpt = medidorRepository.findByTitular(titularOpt.get());
            }
        }

        // Si no se encuentra medidor por email, usar el primero de la base de datos
        if (medidorOpt.isEmpty()) {
            medidorOpt = medidorRepository.findAll().stream().findFirst();
        }

        if (medidorOpt.isEmpty()) {
            return aquaSmartIntegration.fallbackProjection();
        }

        Medidor medidor = medidorOpt.get();
        String medId = medidor.idMedidor;

        // Obtener lecturas de consumo del medidor
        List<LecturaConsumo> allLecturas = lecturaConsumoRepository.findAllWithDetails();
        List<LecturaConsumo> medidorLecturas = allLecturas.stream()
                .filter(l -> l.medidor != null && medId.equals(l.medidor.idMedidor) && l.fecha != null && l.volumenRegistrado != null)
                .toList();

        double totalLiters = 0.0;
        double leakLiters = 0.0;

        for (LecturaConsumo l : medidorLecturas) {
            double vol = l.volumenRegistrado.doubleValue();
            totalLiters += vol;
            if (l.tipoFlujo != null && "Anómalo".equalsIgnoreCase(l.tipoFlujo.nombreTipoFlujo)) {
                leakLiters += vol;
            }
        }

        // Extrapolación a 30 días basándose en días distintos registrados
        long distinctDays = medidorLecturas.stream()
                .map(l -> l.fecha)
                .distinct()
                .count();
        if (distinctDays < 1) {
            distinctDays = 7;
        }

        double averageLitersPerDay = totalLiters / distinctDays;
        double averageLeakPerDay = leakLiters / distinctDays;

        int realConsumption = (int) totalLiters;
        int leakDetected = (int) leakLiters;

        int aiEstimate = (int) (averageLitersPerDay * 30);
        int leakEstimate = (int) (averageLeakPerDay * 30);
        int baseConsumption = Math.max(0, aiEstimate - leakEstimate);

        // Tarifa de S/. 5.0 por m3 = S/. 0.005 por litro
        double projectedBill = aiEstimate * 0.005;
        projectedBill = Math.round(projectedBill * 100.0) / 100.0;

        String aiMessage;
        if (leakDetected > 0) {
            aiMessage = String.format(java.util.Locale.US,
                    "Análisis IA: Hemos detectado un volumen de fuga de %d litros en tu historial. Si esta anomalía persiste, tu recibo proyecta un recargo adicional de S/. %.2f a fin de mes.",
                    leakDetected, leakEstimate * 0.005);
        } else {
            aiMessage = String.format(java.util.Locale.US,
                    "Análisis IA: Tu nivel de consumo de agua es óptimo y estable. Proyección estimada del recibo mensual: S/. %.2f.",
                    projectedBill);
        }

        return new AiProjectionDto(
                projectedBill,
                realConsumption,
                aiEstimate,
                leakDetected,
                leakEstimate,
                baseConsumption,
                aiMessage);
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
    public List<MedidorDto> getMedidores() {
        List<Medidor> medidores = medidorRepository.findAllWithDetails();
        List<MedidorDto> list = new ArrayList<>();
        List<LecturaConsumo> lecturas = lecturaConsumoRepository.findAllWithDetails();
        List<Alerta> alertas = alertaRepository.findAllWithDetails();

        for (Medidor medidor : medidores) {
            String id = medidor.idMedidor;
            
            // Determinar dirección basada en el titular
            String address = "Calle UTP Lote " + id;
            if (medidor.titular != null) {
                if ("maria.quispe@example.com".equalsIgnoreCase(medidor.titular.correo)) {
                    address = "Av. Buenos Aires 124";
                } else if ("luis.condori@example.com".equalsIgnoreCase(medidor.titular.correo)) {
                    address = "Calle Los Próceres 452";
                } else {
                    address = "Jr. Tacna 890 (Lote " + id + ")";
                }
            }

            // Encontrar la última lectura de consumo
            LecturaConsumo latest = lecturas.stream()
                    .filter(l -> l.medidor != null && id.equals(l.medidor.idMedidor) && l.fecha != null && l.hora != null)
                    .max(Comparator.comparing((LecturaConsumo l) -> l.fecha)
                            .thenComparing(l -> l.hora))
                    .orElse(null);

            boolean valveOpen = medidor.estadoValvula != null 
                    && "Abierta".equalsIgnoreCase(medidor.estadoValvula.nombreEstadoValvula);

            double flowVal = 0.0;
            if (valveOpen && latest != null && latest.volumenRegistrado != null) {
                flowVal = latest.volumenRegistrado.doubleValue();
            }

            String flow = String.format(java.util.Locale.US, "%.1f L/min", flowVal);

            // Determinar si hay alerta de fuga activa para este medidor
            boolean activeLeak = alertas.stream()
                    .anyMatch(a -> a.medidor != null && id.equals(a.medidor.idMedidor)
                            && a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado)
                            && a.tipoAlerta != null && "Fuga".equalsIgnoreCase(a.tipoAlerta.nombreTipoAlerta));

            String status = "Óptimo";
            String pressure = "4.2 bar";
            if (!valveOpen) {
                status = "Cierre Manual";
                pressure = "0.1 bar";
            } else if (activeLeak) {
                status = "Fuga Activa";
                pressure = "1.8 bar";
            }

            String valve = valveOpen ? "Abierta" : "Cerrada";

            list.add(new MedidorDto(id, address, flow, pressure, valve, status));
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValveHistoryDto> getValveHistory() {
        return valveHistory.stream().map(this::toValveHistoryDto).toList();
    }

    @Override
    @Transactional
    public ValveStateDto setValve(boolean open, String email) {
        Optional<Medidor> medidorOpt = Optional.empty();
        if (email != null && !email.trim().isEmpty()) {
            Optional<Titular> titularOpt = titularRepository.findAll().stream()
                    .filter(t -> email.trim().equalsIgnoreCase(t.correo))
                    .findFirst();
            if (titularOpt.isPresent()) {
                medidorOpt = medidorRepository.findByTitular(titularOpt.get());
            }
        }
        if (medidorOpt.isEmpty()) {
            medidorOpt = medidorRepository.findAllWithDetails().stream().findFirst();
        }

        if (medidorOpt.isPresent()) {
            Medidor medidor = medidorOpt.get();
            medidor.estadoValvula = resolveValveState(open);
            medidorRepository.saveAndFlush(medidor);

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
    public PresenceStateDto setHomePresence(boolean home, String email) {
        if (email != null && !email.trim().isEmpty()) {
            Optional<Titular> titularOpt = titularRepository.findAll().stream()
                    .filter(t -> email.trim().equalsIgnoreCase(t.correo))
                    .findFirst();
            if (titularOpt.isPresent()) {
                Titular titular = titularOpt.get();
                com.AquaSmart.model.PreferenciaUsuario pref = preferenciaRepository.findByTitular(titular)
                        .orElseGet(() -> new com.AquaSmart.model.PreferenciaUsuario(titular, "light"));
                pref.presenciaCasa = home;
                preferenciaRepository.save(pref);
            }
        }
        String message = home ? "Presencia en casa activada" : "Presencia en casa desactivada";
        return new PresenceStateDto(home, message, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    @Override
    @Transactional
    public com.AquaSmart.dto.AutoCloseStateDto setAutoClose(boolean autoClose, String email) {
        if (email != null && !email.trim().isEmpty()) {
            Optional<Titular> titularOpt = titularRepository.findAll().stream()
                    .filter(t -> email.trim().equalsIgnoreCase(t.correo))
                    .findFirst();
            if (titularOpt.isPresent()) {
                Titular titular = titularOpt.get();
                com.AquaSmart.model.PreferenciaUsuario pref = preferenciaRepository.findByTitular(titular)
                        .orElseGet(() -> new com.AquaSmart.model.PreferenciaUsuario(titular, "light"));
                pref.autoCierreFuga = autoClose;
                preferenciaRepository.save(pref);
            }
        }
        String message = autoClose ? "Cierre automático por fuga activado" : "Cierre automático por fuga desactivado";
        return new com.AquaSmart.dto.AutoCloseStateDto(autoClose, message, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatResponseDto askAi(String question, String email, List<ChatMessageDto> history) {
        return new ChatResponseDto(aquaSmartIntegration.answerQuestion(question, email, history));
    }

    private AlertDto toAlertDto(Alerta alerta) {
        String type = alerta.tipoAlerta == null ? "" : alerta.tipoAlerta.nombreTipoAlerta;
        String state = alerta.tipoEstado == null ? "" : alerta.tipoEstado.nombreTipoEstado;
        String timestamp = (alerta.fecha == null || alerta.hora == null)
                ? ""
                : LocalDateTime.of(alerta.fecha, alerta.hora).format(TIMESTAMP_FORMAT);
        boolean active = state.isBlank() || (!state.equalsIgnoreCase("Inactiva") && !state.equalsIgnoreCase("Cerrada") && !state.equalsIgnoreCase("Resuelta") && !state.equalsIgnoreCase("Cumplido"));

        return new AlertDto(
                alerta.idAlerta,
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