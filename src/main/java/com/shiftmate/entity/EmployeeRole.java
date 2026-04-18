package com.shiftmate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * Junction entity for the many-to-many relationship between {@link Employee}
 * and {@link Role}. An employee may hold multiple roles simultaneously,
 * for example Chef and Sous Chef. Uses a composite primary key.
 */
@Entity
@Table(name = "employee_role")
@IdClass(EmployeeRole.EmployeeRoleId.class)
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class EmployeeRole {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * Composite primary key class for {@link EmployeeRole}.
     * Must implement {@link Serializable} and provide correct
     * {@code equals} and {@code hashCode} implementations.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class EmployeeRoleId implements Serializable {
        private Long employee;
        private Long role;
    }

    @Builder
    public EmployeeRole(Employee employee, Role role) {
        this.employee = employee;
        this.role = role;
    }
}
