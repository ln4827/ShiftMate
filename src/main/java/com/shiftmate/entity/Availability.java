package com.shiftmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

/**
 * A recurring weekly availability window declared by an employee.
 * An employee may have multiple windows per day to represent split shifts
 * such as a morning block and an evening block. Day of week follows
 * ISO-8601 numbering: 1 = Monday, 7 = Sunday.
 */
@Entity
@Table(name = "availability")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "employee")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** ISO-8601 day of week: 1 = Monday through 7 = Sunday. */
    @NotNull
    @Min(1) @Max(7)
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder
    public Availability(Employee employee, Integer dayOfWeek,
                        LocalTime startTime, LocalTime endTime) {
        this.employee = employee;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
