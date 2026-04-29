package com.shiftmate.controller;

import com.shiftmate.dto.CreateSwapRequest;
import com.shiftmate.dto.SwapRequestResponse;
import com.shiftmate.security.ShiftMateUserDetails;
import com.shiftmate.service.SwapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/swaps")
@RequiredArgsConstructor
public class SwapController {

    private final SwapService swapService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    @ResponseStatus(HttpStatus.CREATED)
    public SwapRequestResponse requestSwap(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @Valid @RequestBody CreateSwapRequest request) {
        return swapService.requestSwap(
                principal.getEmployeeId(), principal.getRestaurantId(), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public List<SwapRequestResponse> getMy(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return swapService.getMySwaps(principal.getEmployeeId());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public List<SwapRequestResponse> getPending(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return swapService.getPendingForManager(principal.getRestaurantId());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public SwapRequestResponse approve(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {
        return swapService.approve(principal.getRestaurantId(), id, principal.getEmployeeId());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public SwapRequestResponse reject(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {
        return swapService.reject(principal.getRestaurantId(), id, principal.getEmployeeId());
    }
}
