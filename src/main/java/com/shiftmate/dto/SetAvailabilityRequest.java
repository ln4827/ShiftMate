package com.shiftmate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

/**
 * Request payload for replacing an employee's full availability schedule.
 * Sending an empty {@code windows} list clears all existing availability.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetAvailabilityRequest {

    @NotNull(message = "Windows list is required.")
    @Valid
    private List<Window> windows;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Window {

        /** ISO-8601 day of week: 1 = Monday, 7 = Sunday. */
        @NotNull(message = "Day of week is required.")
        @Min(value = 1, message = "Day of week must be between 1 (Monday) and 7 (Sunday).")
        @Max(value = 7, message = "Day of week must be between 1 (Monday) and 7 (Sunday).")
        private Integer dayOfWeek;

        @NotNull(message = "Start time is required.")
        private LocalTime startTime;

        @NotNull(message = "End time is required.")
        private LocalTime endTime;

        @AssertTrue(message = "Start time and end time must not be equal.")
        public boolean isValidTimeRange() {
            return startTime == null || endTime == null || !startTime.equals(endTime);
        }
    }
}
