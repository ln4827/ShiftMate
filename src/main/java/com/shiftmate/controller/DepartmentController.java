package com.shiftmate.controller;

import com.shiftmate.dto.DepartmentResponse;
import com.shiftmate.dto.SetAllowedRolesRequest;
import com.shiftmate.entity.Department;
import com.shiftmate.entity.Role;
import com.shiftmate.repository.DepartmentRepository;
import com.shiftmate.repository.RoleRepository;
import com.shiftmate.security.ShiftMateUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<DepartmentResponse> getDepartments(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return departmentRepository.findByRestaurantIdWithAllowedRoles(principal.getRestaurantId())
                .stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @PutMapping("/{id}/allowed-roles")
    @PreAuthorize("hasRole('MANAGER')")
    @Transactional
    public DepartmentResponse setAllowedRoles(
            @PathVariable Long id,
            @RequestBody @Valid SetAllowedRolesRequest request,
            @AuthenticationPrincipal ShiftMateUserDetails principal) {

        Department dept = departmentRepository.findById(id)
                .filter(d -> d.getRestaurant().getId().equals(principal.getRestaurantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Set<Role> roles = roleRepository.findAllById(request.getRoleIds()).stream()
                .filter(r -> r.getRestaurant().getId().equals(principal.getRestaurantId()))
                .collect(Collectors.toSet());

        dept.getAllowedRoles().clear();
        dept.getAllowedRoles().addAll(roles);

        return DepartmentResponse.from(dept);
    }
}
