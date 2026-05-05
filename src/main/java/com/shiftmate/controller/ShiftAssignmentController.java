package com.shiftmate.controller;

import com.shiftmate.dto.AssignmentResponse;
import com.shiftmate.dto.CreateAssignmentRequest;
import com.shiftmate.dto.ShiftResponse;
import com.shiftmate.security.ShiftMateUserDetails;
import com.shiftmate.service.ShiftAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts/{shiftId}/assignments")
@RequiredArgsConstructor
public class ShiftAssignmentController {

    private final ShiftAssignmentService assignmentService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<AssignmentResponse> listAssignments(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long shiftId) {

        return assignmentService.listAssignments(principal.getRestaurantId(), shiftId);
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponse assign(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long shiftId,
            @Valid @RequestBody CreateAssignmentRequest request) {

        return assignmentService.assign(principal.getRestaurantId(), shiftId, request);
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long shiftId,
            @PathVariable Long assignmentId) {

        assignmentService.unassign(principal.getRestaurantId(), shiftId, assignmentId);
    }
}
