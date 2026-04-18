package com.shiftmate.service.impl;

import com.shiftmate.dto.AssignRolesRequest;
import com.shiftmate.dto.CreateEmployeeRequest;
import com.shiftmate.dto.EmployeeResponse;
import com.shiftmate.dto.UpdateEmployeeRequest;
import com.shiftmate.entity.Employee;
import com.shiftmate.entity.EmployeeRole;
import com.shiftmate.entity.Restaurant;
import com.shiftmate.entity.Role;
import com.shiftmate.exception.BusinessRuleException;
import com.shiftmate.exception.ResourceNotFoundException;
import com.shiftmate.repository.EmployeeRepository;
import com.shiftmate.repository.EmployeeRoleRepository;
import com.shiftmate.repository.RestaurantRepository;
import com.shiftmate.repository.RoleRepository;
import com.shiftmate.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Default implementation of {@link EmployeeService}.
 *
 * <p>Read operations are annotated with {@code @Transactional(readOnly = true)} so
 * Hibernate skips dirty-checking, reducing overhead on SELECT-only paths. Write
 * operations use a full read-write transaction.
 *
 * <p>The {@link Employee#getVersion()} field is managed automatically by Hibernate.
 * Concurrent modifications to the same employee record cause the second writer to
 * receive an {@link jakarta.persistence.OptimisticLockException}, surfaced to the
 * caller as {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final RestaurantRepository restaurantRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll(Long restaurantId) {
        validateRestaurantExists(restaurantId);
        return employeeRepository.findActiveByRestaurantIdWithRoles(restaurantId)
                .stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAllActive(Long restaurantId) {
        validateRestaurantExists(restaurantId);
        return employeeRepository.findActiveByRestaurantIdWithRoles(restaurantId)
                .stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long restaurantId, Long employeeId) {
        Employee employee = resolveEmployee(restaurantId, employeeId);
        return EmployeeResponse.from(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse create(Long restaurantId, CreateEmployeeRequest request) {
        Restaurant restaurant = resolveRestaurant(restaurantId);

        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException(
                    "An account with email '" + request.getEmail() + "' already exists.");
        }

        Employee employee = Employee.builder()
                .restaurant(restaurant)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isManager(request.isManager())
                .build();

        employee = employeeRepository.save(employee);
        log.info("Created employee id={} email={} manager={}",
                employee.getId(), employee.getEmail(), employee.isManager());

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            attachRoles(employee, request.getRoleIds(), restaurantId);
        }

        return EmployeeResponse.from(
                employeeRepository.findByIdWithRoles(employee.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long restaurantId, Long employeeId,
                                   UpdateEmployeeRequest request) {
        Employee employee = resolveEmployee(restaurantId, employeeId);

        if (!employee.getEmail().equalsIgnoreCase(request.getEmail())
                && employeeRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException(
                    "Email '" + request.getEmail() + "' is already in use.");
        }

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setManager(request.isManager());

        if (StringUtils.hasText(request.getPassword())) {
            employee.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        employee = employeeRepository.save(employee);
        log.info("Updated employee id={}", employee.getId());

        return EmployeeResponse.from(
                employeeRepository.findByIdWithRoles(employee.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public void deactivate(Long restaurantId, Long employeeId) {
        Employee employee = resolveEmployee(restaurantId, employeeId);
        if (!employee.isActive()) {
            throw new BusinessRuleException("Employee is already inactive.");
        }
        employee.setActive(false);
        employeeRepository.save(employee);
        log.info("Deactivated employee id={}", employeeId);
    }

    @Override
    @Transactional
    public void reactivate(Long restaurantId, Long employeeId) {
        Employee employee = resolveEmployee(restaurantId, employeeId);
        if (employee.isActive()) {
            throw new BusinessRuleException("Employee is already active.");
        }
        employee.setActive(true);
        employeeRepository.save(employee);
        log.info("Reactivated employee id={}", employeeId);
    }

    @Override
    @Transactional
    public EmployeeResponse assignRoles(Long restaurantId, Long employeeId,
                                        AssignRolesRequest request) {
        Employee employee = resolveEmployee(restaurantId, employeeId);

        employeeRoleRepository.deleteAllByEmployeeId(employeeId);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            attachRoles(employee, request.getRoleIds(), restaurantId);
        }

        log.info("Replaced roles for employee id={} with roleIds={}",
                employeeId, request.getRoleIds());

        return EmployeeResponse.from(
                employeeRepository.findByIdWithRoles(employeeId).orElseThrow());
    }

    @Override
    @Transactional
    public EmployeeResponse addRole(Long restaurantId, Long employeeId, Long roleId) {
        Employee employee = resolveEmployee(restaurantId, employeeId);
        Role role = resolveRole(restaurantId, roleId);

        if (employeeRoleRepository.existsByEmployeeIdAndRoleId(employeeId, roleId)) {
            throw new BusinessRuleException(
                    "Employee already has role '" + role.getName() + "'.");
        }

        employeeRoleRepository.save(
                EmployeeRole.builder().employee(employee).role(role).build());

        log.info("Added role '{}' to employee id={}", role.getName(), employeeId);

        return EmployeeResponse.from(
                employeeRepository.findByIdWithRoles(employeeId).orElseThrow());
    }

    @Override
    @Transactional
    public EmployeeResponse removeRole(Long restaurantId, Long employeeId, Long roleId) {
        resolveEmployee(restaurantId, employeeId);
        resolveRole(restaurantId, roleId);

        employeeRoleRepository.deleteByEmployeeIdAndRoleId(employeeId, roleId);
        log.info("Removed role id={} from employee id={}", roleId, employeeId);

        return EmployeeResponse.from(
                employeeRepository.findByIdWithRoles(employeeId).orElseThrow());
    }

    /**
     * Resolves a restaurant by ID, throwing if not found.
     *
     * @param restaurantId the restaurant's ID
     * @return the resolved {@link Restaurant}
     * @throws ResourceNotFoundException if no restaurant exists with that ID
     */
    private Restaurant resolveRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
    }

    /**
     * Asserts that a restaurant exists without loading it.
     *
     * @param restaurantId the restaurant's ID
     * @throws ResourceNotFoundException if no restaurant exists with that ID
     */
    private void validateRestaurantExists(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }
    }

    /**
     * Resolves an employee by ID and validates that they belong to the given
     * restaurant. This cross-tenant guard ensures managers cannot access
     * employees from other restaurants.
     *
     * @param restaurantId the restaurant's ID
     * @param employeeId   the employee's ID
     * @return the resolved {@link Employee} with roles loaded
     * @throws ResourceNotFoundException if the employee does not exist or belongs to a different restaurant
     */
    private Employee resolveEmployee(Long restaurantId, Long employeeId) {
        return employeeRepository.findByIdWithRoles(employeeId)
                .filter(e -> e.getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
    }

    /**
     * Resolves a role by ID and validates that it belongs to the given restaurant.
     *
     * @param restaurantId the restaurant's ID
     * @param roleId       the role's ID
     * @return the resolved {@link Role}
     * @throws ResourceNotFoundException if the role does not exist or belongs to a different restaurant
     */
    private Role resolveRole(Long restaurantId, Long roleId) {
        return roleRepository.findById(roleId)
                .filter(r -> r.getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
    }

    /**
     * Attaches a list of roles to an employee, skipping any that are already assigned.
     * Each role is validated to belong to the same restaurant before being attached.
     *
     * @param employee     the employee to attach roles to
     * @param roleIds      the IDs of the roles to attach
     * @param restaurantId the restaurant's ID used to validate each role
     */
    private void attachRoles(Employee employee, List<Long> roleIds, Long restaurantId) {
        for (Long roleId : roleIds) {
            Role role = resolveRole(restaurantId, roleId);
            if (!employeeRoleRepository.existsByEmployeeIdAndRoleId(employee.getId(), roleId)) {
                employeeRoleRepository.save(
                        EmployeeRole.builder().employee(employee).role(role).build());
            }
        }
    }
}
