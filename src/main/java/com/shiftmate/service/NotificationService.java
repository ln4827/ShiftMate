package com.shiftmate.service;

import com.shiftmate.dto.NotificationResponse;
import com.shiftmate.entity.Notification;

import java.util.List;

public interface NotificationService {

    void send(Long employeeId, Notification.Type type, String message);

    List<NotificationResponse> getForEmployee(Long employeeId);

    long getUnreadCount(Long employeeId);

    void markRead(Long employeeId, Long notificationId);

    void markAllRead(Long employeeId);
}
