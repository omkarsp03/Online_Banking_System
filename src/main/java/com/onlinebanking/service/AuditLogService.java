package com.onlinebanking.service;

import com.onlinebanking.entity.AuditEventType;
import com.onlinebanking.entity.AuditLog;
import com.onlinebanking.entity.User;
import com.onlinebanking.repository.AuditLogRepository;
import com.onlinebanking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void logEvent(AuditEventType eventType, String eventDetails, String ipAddress, User user) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEventType(eventType);
        auditLog.setEventDetails(eventDetails);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUser(user);
        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void logEvent(AuditEventType eventType, String eventDetails, String ipAddress, String username) {
        User user = userRepository.findByEmail(username).orElse(null);
        AuditLog auditLog = new AuditLog();
        auditLog.setEventType(eventType);
        auditLog.setEventDetails(eventDetails);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUser(user);
        auditLogRepository.save(auditLog);
    }
}
