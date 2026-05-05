package com.shiftmate.controller;

import com.shiftmate.dto.AvailabilityResponse;
import com.shiftmate.dto.SetAvailabilityRequest;
import com.shiftmate.security.ShiftMateUserDetails;
import com.shiftmate.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for employee availability windows.
 *
 * <p>Managers may read or replace any employee's availability within their
 * restaurant. Employees may only access their own.
 */
@RestController
@RequestMapping("/api/employees/{id}/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    /**
     * Returns all availability windows for the given employee.
     *
     * @param principal the authenticated user
     * @param id        the target employee's ID
     * @return availability windows ordered by day of week then start time
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public List<AvailabilityResponse> getAvailability(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {

        enforceOwnership(principal, id);
        return availabilityService.getAvailability(principal.getRestaurantId(), id);
    }

    /**
     * Replaces the full availability schedule for the given employee.
     * Sending an empty {@code windows} list clears all availability.
     *
     * @param principal the authenticated user
     * @param id        the target employee's ID
     * @param request   the new availability windows
     * @return the saved availability windows
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public List<AvailabilityResponse> setAvailability(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody SetAvailabilityRequest request) {

        enforceOwnership(principal, id);
        return availabilityService.setAvailability(principal.getRestaurantId(), id, request);
    }

    private void enforceOwnership(ShiftMateUserDetails principal, Long targetEmployeeId) {
        if (!principal.isManager() && !principal.getEmployeeId().equals(targetEmployeeId)) {
            throw new AccessDeniedException("Employees may only access their own availability.");
        }
    }
}
