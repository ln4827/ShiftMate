package com.shiftmate.dto;

import com.shiftmate.entity.ShiftAssignment;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Read-only response representing a single shift assignment.
 * Includes enough shift context (date, times) so callers do not need a
 * separate shift fetch to display the assignment in a schedule view.
 */
@Getter
public class AssignmentResponse {

    private final Long id;

    // Shift context
    private final Long shiftId;
    private final LocalDate shiftDate;
    private final LocalTime shiftStart;
    private final LocalTime shiftEnd;

    // Employee
    private final Long employeeId;
    private final String employeeName;

    // Role
    private final Long roleId;
    private final String roleName;

    private final LocalDateTime assignedAt;

    private AssignmentResponse(ShiftAssignment sa) {
        this.id           = sa.getId();
        this.shiftId      = sa.getShift().getId();
        this.shiftDate    = sa.getShift().getShiftDate();
        this.shiftStart   = sa.getShift().getStartTime();
        this.shiftEnd     = sa.getShift().getEndTime();
        this.employeeId   = sa.getEmployee().getId();
        this.employeeName = sa.getEmployee().getFullName();
        this.roleId       = sa.getRole().getId();
        this.roleName     = sa.getRole().getName();
        this.assignedAt   = sa.getAssignedAt();
    }

    public static AssignmentResponse from(ShiftAssignment sa) {
        return new AssignmentResponse(sa);
    }
}
