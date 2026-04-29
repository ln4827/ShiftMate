package com.shiftmate.controller;

import com.shiftmate.dto.DepartmentResponse;
import com.shiftmate.repository.DepartmentRepository;
import com.shiftmate.security.ShiftMateUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<DepartmentResponse> getDepartments(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return departmentRepository.findByRestaurantId(principal.getRestaurantId())
                .stream()
                .map(DepartmentResponse::from)
                .toList();
    }
}
