package com.shiftmate.service;

import com.shiftmate.dto.AssignRolesRequest;
import com.shiftmate.dto.CreateEmployeeRequest;
import com.shiftmate.dto.EmployeeResponse;
import com.shiftmate.dto.UpdateEmployeeRequest;

import java.util.List;

/**
 * Business operations for employee management. All methods are scoped to a
 * restaurant via {@code restaurantId}, which acts as the cross-tenant guard to
 * prevent one restaurant from accessing another's employee data.
 */
public interface EmployeeService {

    /**
     * Returns all employees for a restaurant, both active and inactive.
     *
     * @param restaurantId the restaurant's ID
     * @return list of all employees with their roles
     * @throws com.shiftmate.exception.ResourceNotFoundException if the restaurant does not exist
     */
    List<EmployeeResponse> findAll(Long restaurantId);

    /**
     * Returns only active employees for a restaurant with their roles loaded.
     * Used by the schedule builder to populate the assignment dropdown.
     *
     * @param restaurantId the restaurant's ID
     * @return list of active employees with their roles
     * @throws com.shiftmate.exception.ResourceNotFoundException if the restaurant does not exist
     */
    List<EmployeeResponse> findAllActive(Long restaurantId);

    /**
     * Returns a single employee with their roles.
     *
     * @param restaurantId the restaurant's ID
     * @param employeeId   the employee's ID
     * @return the employee response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the employee does not exist or
     *         does not belong to the given restaurant
     */
    EmployeeResponse findById(Long restaurantId, Long employeeId);

    /**
     * Creates a new employee account with a BCrypt-hashed password and optionally
     * assigns an initial set of roles in the same transaction.
     *
     * @param restaurantId the restaurant's ID
     * @param request      the creation payload
     * @return the newly created employee response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the restaurant does not exist
     * @throws com.shiftmate.exception.BusinessRuleException     if the email is already in use
     */
    EmployeeResponse create(Long restaurantId, CreateEmployeeRequest request);

    /**
     * Updates an existing employee's profile details. Supplying a non-null password
     * replaces the stored BCrypt hash; omitting it leaves the existing hash unchanged.
     *
     * @param restaurantId the restaurant's ID
     * @param employeeId   the employee's ID
     * @param request      the update payload
     * @return the updated employee response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the employee does not exist
     * @throws com.shiftmate.exception.BusinessRuleException     if the new email is already taken
     */
    EmployeeResponse update(Long restaurantId, Long employeeId, UpdateEmployeeRequest request);

    /**
     * Soft-deactivates an employee account. The employee record is retained in
     * the database to preserve historical shift and assignment data.
     *
     * @param restaurantId the restaurant's ID
     * @param employeeId   the employee's ID
     * @throws com.shiftmate.exception.ResourceNotFoundException if the employee does not exist
     * @throws com.shiftmate.exception.BusinessRuleException     if the employee is already inactive
     */
    void deactivate(Long restaurantId, Long employeeId);

    /**
     * Re-activates a previously deactivated employee.
     *
     * @param restaurantId the restaurant's ID
     * @param employeeId   the employee's ID
     * @throws com.shiftmate.exception.ResourceNotFoundException if the employee does not exist
     * @throws com.shiftmate.exception.BusinessRuleException     if the employee is already active
     */
    void reactivate(Long restaurantId, Long employeeId);

    /**
     * Replaces all role assignments for an employee with the provided list.
     * Passing an empty list removes all roles.
     *
     * @param restaurantId the restaurant's ID
     * @param employeeId   the employee's ID
     * @param request      contains the replacement list of role IDs
     * @return the updated employee response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the employee or any role is not found
     */
    EmployeeResponse assignRoles(Long restaurantId, Long employeeId, AssignRolesRequest request);

    /**
     * Adds a single role to an employee without affecting their existing roles.
     *
     * @param restaurantId the restaurant's ID
     * @param employeeId   the employee's ID
     * @param roleId       the role's ID
     * @return the updated employee response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the employee or role is not found
     * @throws com.shiftmate.exception.BusinessRuleException     if the employee already holds this role
     */
    EmployeeResponse addRole(Long restaurantId, Long employeeId, Long roleId);

    /**
     * Removes a single role from an employee.
     *
     * @param restaurantId the restaurant's ID
     * @param employeeId   the employee's ID
     * @param roleId       the role's ID
     * @return the updated employee response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the employee or role is not found
     */
    EmployeeResponse removeRole(Long restaurantId, Long employeeId, Long roleId);
}
