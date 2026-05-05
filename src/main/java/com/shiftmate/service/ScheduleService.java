package com.shiftmate.service;

import com.shiftmate.dto.ScheduleResponse;

/**
 * Builds the weekly schedule view for a restaurant, enriched with per-shift
 * coverage validation. Accepts ISO week strings ({@code "YYYY-Www"}) so the
 * frontend can pass human-readable week identifiers rather than raw dates.
 */
public interface ScheduleService {

    /**
     * Returns the full weekly schedule for a restaurant with coverage status
     * for every shift.
     *
     * @param restaurantId the restaurant's ID
     * @param week         ISO week string, e.g. {@code "2026-W18"}
     * @return the schedule with a per-shift and overall coverage flag
     * @throws com.shiftmate.exception.ResourceNotFoundException if the restaurant does not exist
     * @throws IllegalArgumentException                          if {@code week} is not a valid ISO week
     */
    ScheduleResponse getWeeklySchedule(Long restaurantId, String week);
}
