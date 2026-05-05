package com.shiftmate.dto;

import com.shiftmate.entity.SwapRequest;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
public class SwapRequestResponse {

    private final Long id;
    private final String status;
    private final LocalDateTime requestedAt;
    private final LocalDateTime resolvedAt;
    private final String resolvedByName;

    private final Long requesterAssignmentId;
    private final Long requesterId;
    private final String requesterName;
    private final LocalDate requesterShiftDate;
    private final LocalTime requesterShiftStart;
    private final LocalTime requesterShiftEnd;
    private final String requesterShiftDepartment;
    private final String requesterRoleName;

    private final Long targetAssignmentId;
    private final Long targetId;
    private final String targetName;
    private final LocalDate targetShiftDate;
    private final LocalTime targetShiftStart;
    private final LocalTime targetShiftEnd;
    private final String targetShiftDepartment;
    private final String targetRoleName;

    private SwapRequestResponse(SwapRequest sr) {
        this.id             = sr.getId();
        this.status         = sr.getStatus().name();
        this.requestedAt    = sr.getRequestedAt();
        this.resolvedAt     = sr.getResolvedAt();
        this.resolvedByName = sr.getResolvedBy() != null ? sr.getResolvedBy().getFullName() : null;

        var ra = sr.getRequesterAssignment();
        this.requesterAssignmentId    = ra.getId();
        this.requesterId              = ra.getEmployee().getId();
        this.requesterName            = ra.getEmployee().getFullName();
        this.requesterShiftDate       = ra.getShift().getShiftDate();
        this.requesterShiftStart      = ra.getShift().getStartTime();
        this.requesterShiftEnd        = ra.getShift().getEndTime();
        this.requesterShiftDepartment = ra.getShift().getDepartment().getName();
        this.requesterRoleName        = ra.getRole().getName();

        var ta = sr.getTargetAssignment();
        this.targetAssignmentId    = ta.getId();
        this.targetId              = ta.getEmployee().getId();
        this.targetName            = ta.getEmployee().getFullName();
        this.targetShiftDate       = ta.getShift().getShiftDate();
        this.targetShiftStart      = ta.getShift().getStartTime();
        this.targetShiftEnd        = ta.getShift().getEndTime();
        this.targetShiftDepartment = ta.getShift().getDepartment().getName();
        this.targetRoleName        = ta.getRole().getName();
    }

    public static SwapRequestResponse from(SwapRequest sr) {
        return new SwapRequestResponse(sr);
    }
}
