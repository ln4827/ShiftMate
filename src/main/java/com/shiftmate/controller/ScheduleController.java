package com.shiftmate.controller;

import com.shiftmate.dto.ScheduleResponse;
import com.shiftmate.security.ShiftMateUserDetails;
import com.shiftmate.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the weekly schedule view.
 *
 * <p>Accepts ISO week strings ({@code "2026-W18"}) rather than raw dates so the
 * frontend can use natural week identifiers. The response wraps all shifts for
 * the week with per-shift coverage status and a top-level {@code allCoverageMet}
 * flag for the coverage warning banner.
 *
 * <p>Manager-only: employees access their personal schedule via
 * {@code GET /api/shifts/my}.
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * Returns the full weekly schedule with coverage validation for every shift.
     *
     * @param principal the authenticated manager
     * @param week      ISO week string, e.g. {@code "2026-W18"}
     * @return the schedule with shifts ordered by date/time and coverage flags
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ScheduleResponse getWeeklySchedule(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @RequestParam String week) {

        return scheduleService.getWeeklySchedule(principal.getRestaurantId(), week);
    }
}
