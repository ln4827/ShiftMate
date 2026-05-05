package com.shiftmate.controller;

import com.shiftmate.dto.NotificationResponse;
import com.shiftmate.security.ShiftMateUserDetails;
import com.shiftmate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public List<NotificationResponse> getAll(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return notificationService.getForEmployee(principal.getEmployeeId());
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public Map<String, Long> getUnreadCount(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return Map.of("count", notificationService.getUnreadCount(principal.getEmployeeId()));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public void markRead(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {
        notificationService.markRead(principal.getEmployeeId(), id);
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public void markAllRead(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        notificationService.markAllRead(principal.getEmployeeId());
    }
}
