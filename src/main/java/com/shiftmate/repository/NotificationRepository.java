package com.shiftmate.repository;

import com.shiftmate.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link Notification} entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Returns all notifications for an employee, newest first.
     *
     * @param employeeId the employee's ID
     * @return list of notifications ordered by creation time descending
     */
    List<Notification> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    /**
     * Returns only unread notifications for an employee, newest first.
     * Used to populate the notification dropdown in the navigation bar.
     *
     * @param employeeId the employee's ID
     * @return list of unread notifications ordered by creation time descending
     */
    List<Notification> findByEmployeeIdAndIsReadFalseOrderByCreatedAtDesc(Long employeeId);

    /**
     * Counts notifications for an employee by read status.
     * Used to display the unread badge count in the navigation bar.
     *
     * @param employeeId the employee's ID
     * @param isRead     {@code false} to count unread notifications
     * @return number of matching notifications
     */
    long countByEmployeeIdAndIsRead(Long employeeId, boolean isRead);

    /**
     * Marks a single notification as read, scoped to the owning employee
     * to prevent one employee from marking another's notifications.
     *
     * @param id         the notification's ID
     * @param employeeId the ID of the employee who owns the notification
     * @return number of rows updated (0 or 1)
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.employee.id = :employeeId")
    int markAsRead(@Param("id") Long id, @Param("employeeId") Long employeeId);

    /**
     * Marks all unread notifications for an employee as read in a single update.
     *
     * @param employeeId the employee's ID
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.employee.id = :employeeId AND n.isRead = false")
    int markAllAsRead(@Param("employeeId") Long employeeId);
}
