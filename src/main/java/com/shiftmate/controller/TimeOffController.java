package com.shiftmate.controller;

import com.shiftmate.dto.CreateTimeOffRequest;
import com.shiftmate.dto.TimeOffResponse;
import com.shiftmate.security.ShiftMateUserDetails;
import com.shiftmate.service.TimeOffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/time-off")
@RequiredArgsConstructor
public class TimeOffController {

    private final TimeOffService timeOffService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeOffResponse request(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @Valid @RequestBody CreateTimeOffRequest request) {
        return timeOffService.request(principal.getEmployeeId(), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public List<TimeOffResponse> getMy(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return timeOffService.getMyRequests(principal.getEmployeeId());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public List<TimeOffResponse> getPending(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return timeOffService.getPendingForManager(principal.getRestaurantId());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public TimeOffResponse approve(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {
        return timeOffService.approve(principal.getRestaurantId(), id, principal.getEmployeeId());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public TimeOffResponse reject(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {
        return timeOffService.reject(principal.getRestaurantId(), id, principal.getEmployeeId());
    }
}
