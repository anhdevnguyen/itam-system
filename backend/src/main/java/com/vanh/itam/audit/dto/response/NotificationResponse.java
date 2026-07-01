package com.vanh.itam.audit.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private String type;
    private String message;
    private boolean read;
    private Long relatedEntityId;
    private Instant createdAt;
}
