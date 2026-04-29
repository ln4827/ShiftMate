package com.shiftmate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignEmployeeRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private Long roleId;
}
