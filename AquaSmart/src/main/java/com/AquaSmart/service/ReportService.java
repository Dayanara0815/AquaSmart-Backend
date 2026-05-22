package com.AquaSmart.service;

import java.time.LocalDate;

import com.AquaSmart.dto.WeeklyReportDto;

public interface ReportService {

    WeeklyReportDto getWeeklyReport(LocalDate from, LocalDate to);

}