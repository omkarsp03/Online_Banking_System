package com.onlinebanking.controller;

import com.onlinebanking.dto.ApiResponse;
import com.onlinebanking.dto.BeneficiaryRequest;
import com.onlinebanking.dto.BeneficiaryResponse;
import com.onlinebanking.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/customer/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiaries", description = "Endpoints for managing beneficiaries for funds transfer")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    @Operation(summary = "Add a new beneficiary", description = "Adds a beneficiary in PENDING state")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> addBeneficiary(
            @Valid @RequestBody BeneficiaryRequest request,
            Principal principal,
            HttpServletRequest servletRequest) {
        BeneficiaryResponse response = beneficiaryService.addBeneficiary(request, principal.getName(), servletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(Instant.now(), HttpStatus.CREATED.value(), "Beneficiary added successfully. Pending activation.", response));
    }

    @GetMapping
    @Operation(summary = "List beneficiaries", description = "Retrieves all beneficiaries for the authenticated user")
    public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> getBeneficiaries(Principal principal) {
        List<BeneficiaryResponse> response = beneficiaryService.getBeneficiaries(principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Beneficiaries retrieved", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete beneficiary", description = "Removes a beneficiary from the user's list")
    public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(
            @PathVariable Long id,
            Principal principal,
            HttpServletRequest servletRequest) {
        beneficiaryService.deleteBeneficiary(id, principal.getName(), servletRequest.getRemoteAddr());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Beneficiary deleted successfully", null));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify beneficiary", description = "Verifies and activates a pending beneficiary")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> verifyBeneficiary(
            @PathVariable Long id,
            Principal principal,
            HttpServletRequest servletRequest) {
        BeneficiaryResponse response = beneficiaryService.verifyBeneficiary(id, principal.getName(), servletRequest.getRemoteAddr());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Beneficiary verified and active", response));
    }
}
