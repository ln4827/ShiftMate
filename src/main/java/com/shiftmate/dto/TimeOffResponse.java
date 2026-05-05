package com.shiftmate.dto;

import com.shiftmate.entity.TimeOffRequest;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class TimeOffResponse {

    private final Long id;
    private final Long employeeId;
    private final String employeeName;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String reason;
    private final String status;
    private final LocalDateTime requestedAt;
    private final LocalDateTime resolvedAt;
    private final String resolvedByName;

    private TimeOffResponse(TimeOffRequest r) {
        this.id             = r.getId();
        this.employeeId     = r.getEmployee().getId();
        this.employeeName   = r.getEmployee().getFullName();
        this.startDate      = r.getStartDate();
        this.endDate        = r.getEndDate();
        this.reason         = r.getReason();
        this.status         = r.getStatus().name();
        this.requestedAt    = r.getRequestedAt();
        this.resolvedAt     = r.getResolvedAt();
        this.resolvedByName = r.getResolvedBy() != null ? r.getResolvedBy().getFullName() : null;
    }

    public static TimeOffResponse from(TimeOffRequest r) {
        return new TimeOffResponse(r);
    }
}
