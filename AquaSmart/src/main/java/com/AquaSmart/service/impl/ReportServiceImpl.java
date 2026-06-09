package com.AquaSmart.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.AquaSmart.dto.WeeklyConsumptionDto;
import com.AquaSmart.dto.WeeklyReportDto;
import com.AquaSmart.model.LecturaConsumo;
import com.AquaSmart.model.Medidor;
import com.AquaSmart.model.Titular;
import com.AquaSmart.repository.LecturaConsumoRepository;
import com.AquaSmart.repository.MedidorRepository;
import com.AquaSmart.repository.TitularRepository;
import com.AquaSmart.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_DATE;

    private final LecturaConsumoRepository lecturaConsumoRepository;
    private final TitularRepository titularRepository;
    private final MedidorRepository medidorRepository;

    public ReportServiceImpl(LecturaConsumoRepository lecturaConsumoRepository,
                             TitularRepository titularRepository,
                             MedidorRepository medidorRepository) {
        this.lecturaConsumoRepository = lecturaConsumoRepository;
        this.titularRepository = titularRepository;
        this.medidorRepository = medidorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyReportDto getWeeklyReport(LocalDate from, LocalDate to, String email) {
        LocalDate safeTo = to == null ? LocalDate.now() : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(6) : from;

        if (safeFrom.isAfter(safeTo)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser mayor que la fecha final");
        }

        // 1. Localizar el medidor del usuario/titular solicitado
        Optional<Medidor> medidorOpt = Optional.empty();
        Optional<Titular> titularOpt = Optional.empty();
        if (email != null && !email.isBlank()) {
            titularOpt = titularRepository.findAll().stream()
                    .filter(t -> email.trim().equalsIgnoreCase(t.correo))
                    .findFirst();
            if (titularOpt.isPresent()) {
                medidorOpt = medidorRepository.findByTitular(titularOpt.get());
            }
        }
        
        // Si no se encuentra o no se pasa el email, usar el primer medidor como fallback
        if (medidorOpt.isEmpty()) {
            medidorOpt = medidorRepository.findAll().stream().findFirst();
            if (medidorOpt.isPresent()) {
                titularOpt = Optional.ofNullable(medidorOpt.get().titular);
            }
        }

        final Optional<Medidor> finalMedidor = medidorOpt;

        Map<LocalDate, BigDecimal> litersByDay = new HashMap<>();
        for (LocalDate date = safeFrom; !date.isAfter(safeTo); date = date.plusDays(1)) {
            litersByDay.put(date, BigDecimal.ZERO);
        }

        java.util.Set<LocalDate> anomalousDays = new java.util.HashSet<>();
        java.util.Set<LocalDate> datesWithReadings = new java.util.HashSet<>();
        if (finalMedidor.isPresent()) {
            String targetMedidorId = finalMedidor.get().idMedidor;
            List<LecturaConsumo> rangeLecturas = lecturaConsumoRepository.findByMedidorAndDateRange(targetMedidorId, safeFrom, safeTo);
            for (LecturaConsumo lectura : rangeLecturas) {
                if (lectura.fecha == null || lectura.volumenRegistrado == null) {
                    continue;
                }
                litersByDay.compute(lectura.fecha, (key, value) -> value == null ? lectura.volumenRegistrado : value.add(lectura.volumenRegistrado));
                datesWithReadings.add(lectura.fecha);
                if (lectura.tipoFlujo != null && "Anómalo".equalsIgnoreCase(lectura.tipoFlujo.nombreTipoFlujo)) {
                    anomalousDays.add(lectura.fecha);
                }
            }
        }

        List<WeeklyConsumptionDto> points = new ArrayList<>();
        for (LocalDate date = safeFrom; !date.isAfter(safeTo); date = date.plusDays(1)) {
            double liters = litersByDay.getOrDefault(date, BigDecimal.ZERO).doubleValue();
            points.add(new WeeklyConsumptionDto(
                    DATE_FORMAT.format(date),
                    date.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es", "PE")),
                    roundTwoDecimals(liters),
                    false));
        }

        double average = points.stream().mapToDouble(WeeklyConsumptionDto::liters).average().orElse(0.0);
        double averageForAnomaly = points.stream()
                .filter(p -> {
                    LocalDate d = LocalDate.parse(p.date(), DATE_FORMAT);
                    return datesWithReadings.contains(d);
                })
                .mapToDouble(WeeklyConsumptionDto::liters)
                .average()
                .orElse(0.0);

        double peak = points.stream().mapToDouble(WeeklyConsumptionDto::liters).max().orElse(0.0);
        String peakDay = points.stream()
                .max(Comparator.comparingDouble(WeeklyConsumptionDto::liters))
                .map(WeeklyConsumptionDto::dayLabel)
                .orElse("N/D");

        final java.util.Set<LocalDate> finalAnomalousDays = anomalousDays;
        List<WeeklyConsumptionDto> withAnomalies = points.stream()
                .map(point -> {
                    LocalDate d = LocalDate.parse(point.date(), DATE_FORMAT);
                    boolean isAnomaly = finalAnomalousDays.contains(d) || (averageForAnomaly > 0 && point.liters() > averageForAnomaly * 1.5);
                    return new WeeklyConsumptionDto(
                            point.date(),
                            point.dayLabel(),
                            point.liters(),
                            isAnomaly);
                })
                .toList();

        double total = withAnomalies.stream().mapToDouble(WeeklyConsumptionDto::liters).sum();
        int anomalyCount = (int) withAnomalies.stream().filter(WeeklyConsumptionDto::anomaly).count();

        // Obtener el nombre del titular y código de medidor para el DTO
        String titularName = "María Fernanda Quispe Rojas";
        String medidorId = "ASM-2048";

        if (titularOpt.isPresent()) {
            Titular tt = titularOpt.get();
            StringBuilder sb = new StringBuilder();
            if (tt.nombreTitular != null && !tt.nombreTitular.isBlank()) sb.append(tt.nombreTitular.trim());
            if (tt.apellidoPaterno != null && !tt.apellidoPaterno.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(tt.apellidoPaterno.trim());
            }
            if (tt.apellidoMaterno != null && !tt.apellidoMaterno.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(tt.apellidoMaterno.trim());
            }
            if (sb.length() > 0) {
                titularName = sb.toString();
            }
        }

        if (finalMedidor.isPresent()) {
            medidorId = finalMedidor.get().idMedidor;
        }

        return new WeeklyReportDto(
                ISO_DATE_FORMAT.format(safeFrom),
                ISO_DATE_FORMAT.format(safeTo),
                DATE_FORMAT.format(safeFrom) + " - " + DATE_FORMAT.format(safeTo),
                roundTwoDecimals(total),
                roundTwoDecimals(average),
                peakDay,
                roundTwoDecimals(peak),
                anomalyCount,
                withAnomalies,
                titularName,
                medidorId);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}