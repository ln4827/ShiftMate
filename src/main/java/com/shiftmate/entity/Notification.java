package com.shiftmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A persistent in-app notification stored in the database for a specific employee.
 * Notifications are polled on page load rather than pushed in real time. The
 * {@code type} field allows the frontend to render appropriate icons and styling
 * per notification category.
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "employee")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotBlank
    @Size(max = 500)
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private Type type = Type.GENERAL;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Categorises the notification so the frontend can apply the appropriate
     * icon and styling. Values must match the CHECK constraint in the SQL schema.
     */
    public enum Type {
        SCHEDULE_PUBLISHED,
        SWAP_APPROVED,
        SWAP_REJECTED,
        TIMEOFF_APPROVED,
        TIMEOFF_REJECTED,
        TIMEOFF_REQUESTED,
        SWAP_REQUESTED,
        GENERAL
    }

    /**
     * Sets {@code createdAt} to the current timestamp on first persist.
     */
    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public Notification(Employee employee, String message, Type type) {
        this.employee = employee;
        this.message = message;
        this.type = type;
        this.isRead = false;
    }
}
