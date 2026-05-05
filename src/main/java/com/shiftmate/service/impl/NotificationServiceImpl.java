package com.shiftmate.service.impl;

import com.shiftmate.dto.NotificationResponse;
import com.shiftmate.entity.Employee;
import com.shiftmate.entity.Notification;
import com.shiftmate.exception.ResourceNotFoundException;
import com.shiftmate.repository.EmployeeRepository;
import com.shiftmate.repository.NotificationRepository;
import com.shiftmate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;

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
