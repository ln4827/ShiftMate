package com.shiftmate.service;

import com.shiftmate.dto.AssignEmployeeRequest;
import com.shiftmate.dto.ShiftResponse;

public interface ShiftAssignmentService {

    ShiftResponse assign(Long restaurantId, Long shiftId, AssignEmployeeRequest request);

    void unassign(Long restaurantId, Long shiftId, Long assignmentId);
}
