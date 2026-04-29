package com.shiftmate.service;

import com.shiftmate.dto.EmployeeHoursResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    List<EmployeeHoursResponse> getWeeklyHours(Long restaurantId, LocalDate weekStart);
}
