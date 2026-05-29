package com.onlinebanking.controller;

import com.onlinebanking.dto.AdminAccountResponse;
import com.onlinebanking.dto.ApiResponse;
import com.onlinebanking.dto.AuditLogResponse;
import com.onlinebanking.dto.PagedResponse;
import com.onlinebanking.dto.RoleChangeRequest;
import com.onlinebanking.dto.TransactionResponse;
import com.onlinebanking.dto.UserResponse;
import com.onlinebanking.entity.AuditEventType;
import com.onlinebanking.entity.RoleName;
import com.onlinebanking.service.AdminService;
import com.onlinebanking.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import java.security.Principal;
import java.time.Instant;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Endpoints for bank administrators to manage users, accounts, audit logs, and transaction monitoring")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Retrieves a paginated list of all registered users in the banking system")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> listUsers(
            Principal principal,
            HttpServletRequest servletRequest,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<UserResponse> users = PagedResponse.fromPage(adminService.listUsers(pageable));
        auditLogService.logEvent(AuditEventType.ADMIN_ACTION, "Viewed all users", servletRequest.getRemoteAddr(), principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Users fetched", users));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details", description = "Retrieves complete details of a specific user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable @NotNull Long userId, Principal principal, HttpServletRequest servletRequest) {
        UserResponse user = adminService.getUserById(userId);
        auditLogService.logEvent(AuditEventType.ADMIN_ACTION, "Viewed user " + userId, servletRequest.getRemoteAddr(), principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "User fetched", user));
    }

    @GetMapping("/accounts")
    @Operation(summary = "List all bank accounts", description = "Retrieves a paginated list of all active/inactive bank accounts across the system")
    public ResponseEntity<ApiResponse<PagedResponse<AdminAccountResponse>>> listAccounts(
            Principal principal,
            HttpServletRequest servletRequest,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AdminAccountResponse> accounts = PagedResponse.fromPage(adminService.listAccounts(pageable));
        auditLogService.logEvent(AuditEventType.ADMIN_ACTION, "Viewed all accounts", servletRequest.getRemoteAddr(), principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Accounts fetched", accounts));
    }

    @PutMapping("/accounts/{accountNumber}/activate")
    @Operation(summary = "Activate a bank account", description = "Activates a deactivated bank account, enabling transactions")
    public ResponseEntity<ApiResponse<AdminAccountResponse>> activateAccount(@PathVariable String accountNumber,
                                                                              Principal principal,
                                                                              HttpServletRequest servletRequest) {
        AdminAccountResponse account = adminService.setAccountActiveState(accountNumber, true);
        auditLogService.logEvent(AuditEventType.ADMIN_ACTION, "Activated account " + accountNumber, servletRequest.getRemoteAddr(), principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Account activated", account));
    }

    @PutMapping("/accounts/{accountNumber}/deactivate")
    @Operation(summary = "Deactivate a bank account", description = "Deactivates a bank account, freezing all deposits/withdrawals/transfers")
    public ResponseEntity<ApiResponse<AdminAccountResponse>> deactivateAccount(@PathVariable String accountNumber,
                                                                                Principal principal,
                                                                                HttpServletRequest servletRequest) {
        AdminAccountResponse account = adminService.setAccountActiveState(accountNumber, false);
        auditLogService.logEvent(AuditEventType.ADMIN_ACTION, "Deactivated account " + accountNumber, servletRequest.getRemoteAddr(), principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Account deactivated", account));
    }

    @GetMapping("/transactions")
    @Operation(summary = "List system-wide transactions", description = "Retrieves all transactions in the system, with optional filters by account number and type")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> listTransactions(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String transactionType,
            Principal principal,
            HttpServletRequest servletRequest,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TransactionResponse> transactions = PagedResponse.fromPage(
                adminService.listTransactions(accountNumber, transactionType, pageable));
        auditLogService.logEvent(AuditEventType.ADMIN_ACTION, "Viewed transactions", servletRequest.getRemoteAddr(), principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Transactions fetched", transactions));
    }

    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Change user role", description = "Updates a user's access role (e.g. CUSTOMER to ADMIN)")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(@PathVariable @NotNull Long userId,
                                                                    @Valid @RequestBody RoleChangeRequest request,
                                                                    Principal principal,
                                                                    HttpServletRequest servletRequest) {
        UserResponse user = adminService.changeUserRole(userId, RoleName.valueOf(request.getRoleName().toUpperCase()));
        auditLogService.logEvent(AuditEventType.ADMIN_ACTION, "Changed role for user " + userId + " to " + request.getRoleName(), servletRequest.getRemoteAddr(), principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "User role updated", user));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get system audit logs", description = "Retrieves security and action audit logs for system auditability")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> listAuditLogs(
            Principal principal,
            HttpServletRequest servletRequest,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AuditLogResponse> auditLogs = PagedResponse.fromPage(adminService.listAuditLogs(pageable));
        auditLogService.logEvent(AuditEventType.ADMIN_ACTION, "Viewed audit logs", servletRequest.getRemoteAddr(), principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Audit logs fetched", auditLogs));
    }
}
