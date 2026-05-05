package com.shiftmate.dto;

import com.shiftmate.entity.Availability;
import lombok.Getter;

import java.time.LocalTime;

@Getter
public class AvailabilityResponse {

    private final Long id;
    private final Long employeeId;
    private final Integer dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;

    private AvailabilityResponse(Availability a) {
        this.id         = a.getId();
        this.employeeId = a.getEmployee().getId();
        this.dayOfWeek  = a.getDayOfWeek();
        this.startTime  = a.getStartTime();
        this.endTime    = a.getEndTime();
    }

    public static AvailabilityResponse from(Availability a) {
        return new AvailabilityResponse(a);
    }
}
