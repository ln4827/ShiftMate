package com.shiftmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A staff role such as Chef, Waiter, or Bartender. Roles are scoped per
 * restaurant so each restaurant can define its own role names. Role names
 * must be unique within a single restaurant.
 */
@Entity
@Table(
    name = "role",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_role_name",
        columnNames = {"restaurant_id", "name"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"restaurant", "employeeRoles", "shiftAssignments", "coverageRequirements"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @NotBlank
    @Size(max = 80)
    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmployeeRole> employeeRoles = new ArrayList<>();

    @OneToMany(mappedBy = "role")
    private List<ShiftAssignment> shiftAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShiftCoverageRequirement> coverageRequirements = new ArrayList<>();

    @Builder
    public Role(Restaurant restaurant, String name) {
        this.restaurant = restaurant;
        this.name = name;
    }
}
