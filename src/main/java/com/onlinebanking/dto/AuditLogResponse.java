package com.onlinebanking.dto;

import com.onlinebanking.entity.AuditEventType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private AuditEventType eventType;
    private String eventDetails;
    private String ipAddress;
    private String userEmail;
    private Instant createdAt;
}
