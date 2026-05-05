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

                // 1. Fetch department (with roles joined)
                Department dept = departmentRepository.findByIdWithAllowedRoles(id)
                                .filter(d -> d.getRestaurant().getId().equals(principal.getRestaurantId()))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

                // 2. Fetch ONLY the valid roles that belong to this restaurant
                // This replaces the .stream().filter(...) which was likely causing the 500
                // error
                List<Role> validRoles = roleRepository.findAllByIdInAndRestaurantId(
                                request.getRoleIds(),
                                principal.getRestaurantId());

                // 3. Sync the collection
                dept.getAllowedRoles().clear();
                dept.getAllowedRoles().addAll(validRoles);

                // 4. Save and flush explicitly to catch database errors before returning
                departmentRepository.saveAndFlush(dept);

                return DepartmentResponse.from(dept);
        }
}
