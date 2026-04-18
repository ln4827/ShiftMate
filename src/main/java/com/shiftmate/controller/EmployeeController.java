package com.shiftmate.controller;

import com.shiftmate.dto.AssignRolesRequest;
import com.shiftmate.dto.CreateEmployeeRequest;
import com.shiftmate.dto.EmployeeResponse;
import com.shiftmate.dto.UpdateEmployeeRequest;
import com.shiftmate.entity.Role;
import com.shiftmate.repository.RoleRepository;
import com.shiftmate.security.ShiftMateUserDetails;
import com.shiftmate.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for employee management operations.
 *
 * <p>All endpoints are scoped to the authenticated user's restaurant via
 * {@link ShiftMateUserDetails#getRestaurantId()}, ensuring tenants cannot
 * access each other's data. Manager-only operations are enforced via
 * {@code @PreAuthorize("hasRole('MANAGER')")}.
 */
@Slf4j
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final RoleRepository roleRepository;

    /**
     * Returns all employees for the authenticated manager's restaurant.
     *
     * @param principal the authenticated user
     * @return list of employees with their roles
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<EmployeeResponse> listEmployees(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return employeeService.findAll(principal.getRestaurantId());
    }

    /**
     * Creates a new employee account within the authenticated manager's restaurant.
     *
     * @param principal the authenticated manager
     * @param request   the employee creation payload
     * @return the created employee with HTTP 201
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createEmployee(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @Valid @RequestBody CreateEmployeeRequest request) {
        return employeeService.create(principal.getRestaurantId(), request);
    }

    /**
     * Returns a single employee by ID, scoped to the authenticated user's restaurant.
     *
     * @param principal the authenticated user
     * @param id        the employee's ID
     * @return the employee with their roles
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public EmployeeResponse getEmployee(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {
        return employeeService.findById(principal.getRestaurantId(), id);
    }

    /**
     * Updates an existing employee's profile details.
     *
     * @param principal the authenticated manager
     * @param id        the employee's ID
     * @param request   the update payload
     * @return the updated employee
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public EmployeeResponse updateEmployee(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return employeeService.update(principal.getRestaurantId(), id, request);
    }

    /**
     * Soft-deactivates an employee. Their data is retained for audit purposes.
     *
     * @param principal the authenticated manager
     * @param id        the employee's ID
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateEmployee(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {
        employeeService.deactivate(principal.getRestaurantId(), id);
    }

    /**
     * Re-activates a previously deactivated employee.
     *
     * @param principal the authenticated manager
     * @param id        the employee's ID
     */
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivateEmployee(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {
        employeeService.reactivate(principal.getRestaurantId(), id);
    }

    /**
     * Replaces all role assignments for an employee with the provided list.
     *
     * @param principal the authenticated manager
     * @param id        the employee's ID
     * @param request   contains the replacement list of role IDs
     * @return the updated employee
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('MANAGER')")
    public EmployeeResponse assignRoles(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignRolesRequest request) {
        return employeeService.assignRoles(principal.getRestaurantId(), id, request);
    }

    /**
     * Adds a single role to an employee without affecting their existing roles.
     *
     * @param principal the authenticated manager
     * @param id        the employee's ID
     * @param roleId    the role's ID to add
     * @return the updated employee
     */
    @PostMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasRole('MANAGER')")
    public EmployeeResponse addRole(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id,
            @PathVariable Long roleId) {
        return employeeService.addRole(principal.getRestaurantId(), id, roleId);
    }

    /**
     * Removes a single role from an employee.
     *
     * @param principal the authenticated manager
     * @param id        the employee's ID
     * @param roleId    the role's ID to remove
     * @return the updated employee
     */
    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasRole('MANAGER')")
    public EmployeeResponse removeRole(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id,
            @PathVariable Long roleId) {
        return employeeService.removeRole(principal.getRestaurantId(), id, roleId);
    }

    /**
     * Returns all roles available in the authenticated manager's restaurant.
     * Used to populate the role assignment form.
     *
     * @param principal the authenticated manager
     * @return list of available roles
     */
    @GetMapping("/available-roles")
    @PreAuthorize("hasRole('MANAGER')")
    public List<Role> getAvailableRoles(
            @AuthenticationPrincipal ShiftMateUserDetails principal) {
        return roleRepository.findByRestaurantId(principal.getRestaurantId());
    }
}
