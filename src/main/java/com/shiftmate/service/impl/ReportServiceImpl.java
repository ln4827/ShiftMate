package com.shiftmate.service.impl;

import com.shiftmate.dto.EmployeeHoursResponse;
import com.shiftmate.repository.EmployeeRepository;
import com.shiftmate.repository.ShiftAssignmentRepository;
import com.shiftmate.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ShiftAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeHoursResponse> getWeeklyHours(Long restaurantId, LocalDate weekStart) {
        LocalDate monday = weekStart.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        List<Object[]> rows = assignmentRepository.sumHoursPerEmployee(restaurantId, monday, sunday);
        if (rows.isEmpty()) {
            return List.of();
        }

        Set<Long> ids = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .collect(Collectors.toSet());

        Map<Long, String> names = employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        e -> e.getId(),
                        e -> e.getFullName()
                ));

        return rows.stream()
                .map(r -> {
                    Long empId = ((Number) r[0]).longValue();
                    double hours = r[1] != null ? ((Number) r[1]).doubleValue() : 0.0;
                    return new EmployeeHoursResponse(empId, names.getOrDefault(empId, "Unknown"), hours);
                })
                .sorted(Comparator.comparing(EmployeeHoursResponse::getEmployeeName))
                .toList();
    }
}
