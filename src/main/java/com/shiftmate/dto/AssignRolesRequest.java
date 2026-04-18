package com.shiftmate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

/**
 * Request payload for replacing all role assignments on an employee.
 * Pass an empty list to clear all roles.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AssignRolesRequest {

    @NotNull(message = "Role ID list is required (may be empty to clear all roles).")
    private List<Long> roleIds;
}
