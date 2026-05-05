package com.shiftmate.dto;

import com.shiftmate.entity.Employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only response object representing an employee. Constructed from an
 * {@link Employee} entity via the static factory method
 * {@link #from(Employee)}.
 * Never exposes the password hash or other sensitive internal fields.
 */
@Getter
public class EmployeeResponse {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final boolean manager;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final List<RoleDto> roles;

    private EmployeeResponse(Employee employee) {
        this.id = employee.getId();
        this.firstName = employee.getFirstName();
        this.lastName = employee.getLastName();
        this.email = employee.getEmail();
        this.manager = employee.isManager();
        this.active = employee.isActive();
        this.createdAt = employee.getCreatedAt();
        this.roles = employee.getEmployeeRoles().stream()
                .map(er -> new RoleDto(er.getRole().getId(), er.getRole().getName()))
                .toList();
    }

    /**
     * Creates an {@code EmployeeResponse} from a fully loaded {@link Employee}
     * entity.
     * The entity's {@code employeeRoles} collection must be initialised before
     * calling
     * this method to avoid lazy-loading exceptions.
     *
     * @param employee the source entity with roles loaded
     * @return the corresponding response object
     */
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(employee);
    }

    /**
     * Lightweight projection of a {@link com.shiftmate.entity.Role} for embedding
     * inside an {@code EmployeeResponse}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleDto {
        private Long id;
        private String name;
    }
}
