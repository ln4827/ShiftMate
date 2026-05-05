package com.shiftmate.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request payload for creating a new shift. All fields are validated before
 * the request reaches the service layer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateShiftRequest {

    @NotNull(message = "Department ID is required.")
    private Long departmentId;

    @NotNull(message = "Shift date is required.")
    private LocalDate shiftDate;

    @NotNull(message = "Start time is required.")
    private LocalTime startTime;

    @NotNull(message = "End time is required.")
    private LocalTime endTime;

    /** Rejects zero-duration shifts (startTime == endTime). Overnight shifts are allowed. */
    @AssertTrue(message = "Start time and end time must not be equal.")
    public boolean isValidTimeRange() {
        return startTime == null || endTime == null || !startTime.equals(endTime);
    }
}
