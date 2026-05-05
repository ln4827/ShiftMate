package com.shiftmate.service.impl;

import com.shiftmate.dto.NotificationResponse;
import com.shiftmate.entity.Employee;
import com.shiftmate.entity.Notification;
import com.shiftmate.exception.ResourceNotFoundException;
import com.shiftmate.repository.EmployeeRepository;
import com.shiftmate.repository.NotificationRepository;
import com.shiftmate.service.NotificationService;
import com.shiftmate.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    @Transactional
    public void send(Long employeeId, Notification.Type type, String message) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
        Notification notification = Notification.builder()
                .employee(employee)
                .type(type)
                .message(message)
                .build();
        notificationRepository.save(notification);
        log.debug("Sent notification type={} to employee id={}", type, employeeId);

        // Push SSE event after transaction commits so the DB is visible to the next read
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sseEmitterRegistry.send(employeeId, "notification", Map.of("type", type.name()));
            }
        });
    }

    @Override
    @Transactional
    public void sendToManagers(Long restaurantId, Notification.Type type, String message) {
        List<Employee> managers = employeeRepository.findByRestaurantIdAndIsManager(restaurantId, true);
        for (Employee manager : managers) {
            Notification notification = Notification.builder()
                    .employee(manager)
                    .type(type)
                    .message(message)
                    .build();
            notificationRepository.save(notification);
        }
        log.debug("Sent notification type={} to {} managers in restaurant id={}", type, managers.size(), restaurantId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                managers.forEach(m ->
                        sseEmitterRegistry.send(m.getId(), "notification", Map.of("type", type.name())));
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getForEmployee(Long employeeId) {
        return notificationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long employeeId) {
        return notificationRepository.countByEmployeeIdAndIsRead(employeeId, false);
    }

    @Override
    @Transactional
    public void markRead(Long employeeId, Long notificationId) {
        int updated = notificationRepository.markAsRead(notificationId, employeeId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }
    }

    @Override
    @Transactional
    public void markAllRead(Long employeeId) {
        notificationRepository.markAllAsRead(employeeId);
    }
}
