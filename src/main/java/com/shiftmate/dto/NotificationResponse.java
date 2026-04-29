package com.shiftmate.dto;

import com.shiftmate.entity.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponse {

    private final Long id;
    private final String message;
    private final String type;
    private final boolean read;
    private final LocalDateTime createdAt;

    private NotificationResponse(Notification n) {
        this.id        = n.getId();
        this.message   = n.getMessage();
        this.type      = n.getType().name();
        this.read      = n.isRead();
        this.createdAt = n.getCreatedAt();
    }

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n);
    }
}
