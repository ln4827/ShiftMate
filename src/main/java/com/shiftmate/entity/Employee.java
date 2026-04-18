package com.shiftmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a staff member, both managers and regular employees.
 * The {@code isManager} flag determines access level within the application.
 * Inactive employees ({@code isActive = false}) are soft-deleted — their
 * historical shift and assignment data is preserved for audit purposes.
 * The {@code version} field enables optimistic locking to prevent concurrent
 * modifications from silently overwriting each other.
 */
@Entity
@Table(name = "employee")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"restaurant", "employeeRoles", "assignments", "availability",
                     "timeOffRequests", "notifications"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Employee {

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
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @NotBlank
    @Size(max = 80)
    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /** BCrypt hash of the employee's password. Never exposed in API responses. */
    @NotBlank
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_manager", nullable = false)
    private boolean isManager = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Incremented by Hibernate on every UPDATE; guards against concurrent edits. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmployeeRole> employeeRoles = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Availability> availability = new ArrayList<>();

    @OneToMany(mappedBy = "employee")
    private List<ShiftAssignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeOffRequest> timeOffRequests = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    /**
     * Sets {@code createdAt} to the current timestamp on first persist.
     */
    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Returns the employee's full display name.
     *
     * @return first and last name joined by a space
     */
    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Builder
    public Employee(Restaurant restaurant, String firstName, String lastName,
                    String email, String passwordHash, boolean isManager) {
        this.restaurant = restaurant;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isManager = isManager;
        this.isActive = true;
    }
}
