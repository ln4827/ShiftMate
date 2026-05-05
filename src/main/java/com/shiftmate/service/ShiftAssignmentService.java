package com.shiftmate.service;

import com.shiftmate.dto.AssignmentResponse;
import com.shiftmate.dto.CreateAssignmentRequest;
import com.shiftmate.dto.ShiftResponse;

import java.util.List;

public interface ShiftAssignmentService {

    ShiftResponse assign(Long restaurantId, Long shiftId, CreateAssignmentRequest request);

    void unassign(Long restaurantId, Long shiftId, Long assignmentId);

    List<AssignmentResponse> listAssignments(Long restaurantId, Long shiftId);
}
