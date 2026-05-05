package com.shiftmate.controller;

import com.shiftmate.dto.EmployeeHoursResponse;
import com.shiftmate.security.ShiftMateUserDetails;
import com.shiftmate.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/hours")
    @PreAuthorize("hasRole('MANAGER')")
    public List<EmployeeHoursResponse> getHours(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        LocalDate monday = (weekStart != null ? weekStart : LocalDate.now())
                .with(DayOfWeek.MONDAY);
        return reportService.getWeeklyHours(principal.getRestaurantId(), monday);
    }
}
