package com.shiftmate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmployeeHoursResponse {

    private final Long employeeId;
    private final String employeeName;
    private final double totalHours;
}
