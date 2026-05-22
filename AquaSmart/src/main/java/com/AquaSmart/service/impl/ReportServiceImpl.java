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

import org.springframework.stereotype.Service;

import com.AquaSmart.dto.WeeklyConsumptionDto;
import com.AquaSmart.dto.WeeklyReportDto;
import com.AquaSmart.model.LecturaConsumo;
import com.AquaSmart.repository.LecturaConsumoRepository;
import com.AquaSmart.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_DATE;

    private final LecturaConsumoRepository lecturaConsumoRepository;

    public ReportServiceImpl(LecturaConsumoRepository lecturaConsumoRepository) {
        this.lecturaConsumoRepository = lecturaConsumoRepository;
    }

    @Override
    public WeeklyReportDto getWeeklyReport(LocalDate from, LocalDate to) {
        LocalDate safeTo = to == null ? LocalDate.now() : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(6) : from;

        if (safeFrom.isAfter(safeTo)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser mayor que la fecha final");
        }

        Map<LocalDate, BigDecimal> litersByDay = new HashMap<>();
        for (LocalDate date = safeFrom; !date.isAfter(safeTo); date = date.plusDays(1)) {
            litersByDay.put(date, BigDecimal.ZERO);
        }

        for (LecturaConsumo lectura : lecturaConsumoRepository.findAll()) {
            if (lectura.fecha == null || lectura.volumenRegistrado == null) {
                continue;
            }

            if (!lectura.fecha.isBefore(safeFrom) && !lectura.fecha.isAfter(safeTo)) {
                litersByDay.compute(lectura.fecha, (key, value) -> value == null ? lectura.volumenRegistrado : value.add(lectura.volumenRegistrado));
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
        double peak = points.stream().mapToDouble(WeeklyConsumptionDto::liters).max().orElse(0.0);
        String peakDay = points.stream()
                .max(Comparator.comparingDouble(WeeklyConsumptionDto::liters))
                .map(WeeklyConsumptionDto::dayLabel)
                .orElse("N/D");

        List<WeeklyConsumptionDto> withAnomalies = points.stream()
                .map(point -> new WeeklyConsumptionDto(
                        point.date(),
                        point.dayLabel(),
                        point.liters(),
                        average > 0 && point.liters() > average * 1.5))
                .toList();

        double total = withAnomalies.stream().mapToDouble(WeeklyConsumptionDto::liters).sum();
        int anomalyCount = (int) withAnomalies.stream().filter(WeeklyConsumptionDto::anomaly).count();

        return new WeeklyReportDto(
                ISO_DATE_FORMAT.format(safeFrom),
                ISO_DATE_FORMAT.format(safeTo),
                DATE_FORMAT.format(safeFrom) + " - " + DATE_FORMAT.format(safeTo),
                roundTwoDecimals(total),
                roundTwoDecimals(average),
                peakDay,
                roundTwoDecimals(peak),
                anomalyCount,
                withAnomalies);
    }


    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}