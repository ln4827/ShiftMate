package com.shiftmate.dto;

import lombok.Getter;

import java.util.List;

/**
 * Top-level response for {@code GET /api/schedule?week=}. Wraps the ordered
 * list of {@link ShiftResponse} objects and surfaces a convenience flag so the
 * frontend can show a coverage-warning banner without iterating every shift.
 */
@Getter
public class ScheduleResponse {

    private final String week;
    private final List<ShiftResponse> shifts;
    private final boolean allCoverageMet;

    private ScheduleResponse(String week, List<ShiftResponse> shifts) {
        this.week   = week;
        this.shifts = shifts;
        this.allCoverageMet = shifts.stream().allMatch(ShiftResponse::isCoverageMet);
    }

    public static ScheduleResponse of(String week, List<ShiftResponse> shifts) {
        return new ScheduleResponse(week, shifts);
    }
}
