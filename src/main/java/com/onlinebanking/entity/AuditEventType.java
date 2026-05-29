package com.onlinebanking.entity;

public enum AuditEventType {
    LOGIN,
    LOGOUT,
    TRANSFER,
    DEPOSIT,
    WITHDRAWAL,
    ACCOUNT_CREATED,
    ROLE_CHANGED,
    ADMIN_ACTION,
    SECURITY_ALERT
}
