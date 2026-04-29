package com.shiftmate.service;

import com.shiftmate.dto.CreateTimeOffRequest;
import com.shiftmate.dto.TimeOffResponse;

import java.util.List;

public interface TimeOffService {

    TimeOffResponse request(Long employeeId, CreateTimeOffRequest request);

    List<TimeOffResponse> getMyRequests(Long employeeId);

    List<TimeOffResponse> getPendingForManager(Long restaurantId);

    TimeOffResponse approve(Long restaurantId, Long requestId, Long managerId);

    TimeOffResponse reject(Long restaurantId, Long requestId, Long managerId);
}
