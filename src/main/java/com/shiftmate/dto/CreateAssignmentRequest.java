package com.shiftmate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for assigning an employee to a shift in a specific role.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateAssignmentRequest {

    @NotNull(message = "Employee ID is required.")
    private Long employeeId;

    @NotNull(message = "Role ID is required.")
    private Long roleId;
}
