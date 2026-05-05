package com.shiftmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Organisational unit within a restaurant, such as Kitchen, Bar, or Front of House.
 * Department names must be unique within a single restaurant.
 */
@Entity
@Table(
    name = "department",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_department_name",
        columnNames = {"restaurant_id", "name"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"restaurant", "shifts", "allowedRoles"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Department {

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

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Shift> shifts = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "department_allowed_roles",
        joinColumns = @JoinColumn(name = "department_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> allowedRoles = new HashSet<>();

    @Builder
    public Department(Restaurant restaurant, String name) {
        this.restaurant = restaurant;
        this.name = name;
    }
}
