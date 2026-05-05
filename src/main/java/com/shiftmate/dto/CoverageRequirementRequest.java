package com.shiftmate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for setting a coverage requirement on a shift.
 * If a requirement already exists for the given role it is updated in place;
 * otherwise a new one is created (upsert semantics).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoverageRequirementRequest {

    @NotNull(message = "Role ID is required.")
    private Long roleId;

    @Min(value = 1, message = "Minimum count must be at least 1.")
    private int minCount = 1;
}
