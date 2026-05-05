package com.shiftmate.controller;

import com.shiftmate.dto.CreateSwapRequest;
import com.shiftmate.dto.SwapRequestResponse;
import com.shiftmate.security.ShiftMateUserDetails;
import com.shiftmate.service.SwapRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/swap-requests")
@RequiredArgsConstructor
public class SwapRequestController {

    private final SwapRequestService swapRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    @ResponseStatus(HttpStatus.CREATED)
    public SwapRequestResponse create(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @Valid @RequestBody CreateSwapRequest request) {
        return swapRequestService.createSwapRequest(
                principal.getEmployeeId(), principal.getRestaurantId(), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public List<SwapRequestResponse> getMy(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return swapRequestService.getMySwapRequests(principal.getEmployeeId());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public List<SwapRequestResponse> getPending(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return swapRequestService.getPendingForManager(principal.getRestaurantId());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public SwapRequestResponse approve(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {
        return swapRequestService.approve(principal.getRestaurantId(), id, principal.getEmployeeId());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public SwapRequestResponse reject(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {
        return swapRequestService.reject(principal.getRestaurantId(), id, principal.getEmployeeId());
    }
}
