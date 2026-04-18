package com.shiftmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Records that a specific employee has been assigned to a specific shift
 * in a specific role. The unique constraint on {@code (shift_id, employee_id)}
 * ensures an employee cannot be assigned to the same shift twice. The
 * {@code version} field provides optimistic locking to prevent two managers
 * from creating conflicting assignments simultaneously.
 */
@Entity
@Table(
    name = "shift_assignment",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_sa_shift_employee",
        columnNames = {"shift_id", "employee_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"shift", "employee", "role", "outgoingSwapRequests", "incomingSwapRequests"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShiftAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    /** Incremented by Hibernate on every UPDATE; the critical guard against concurrent assignment races. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "requesterAssignment")
    private List<SwapRequest> outgoingSwapRequests = new ArrayList<>();

    @OneToMany(mappedBy = "targetAssignment")
    private List<SwapRequest> incomingSwapRequests = new ArrayList<>();

    /**
     * Sets {@code assignedAt} to the current timestamp on first persist.
     */
    @PrePersist
    private void prePersist() {
        assignedAt = LocalDateTime.now();
    }

    @Builder
    public ShiftAssignment(Shift shift, Employee employee, Role role) {
        this.shift = shift;
        this.employee = employee;
        this.role = role;
    }
}
