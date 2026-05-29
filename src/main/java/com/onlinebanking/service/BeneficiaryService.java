package com.onlinebanking.service;

import com.onlinebanking.dto.BeneficiaryRequest;
import com.onlinebanking.dto.BeneficiaryResponse;
import com.onlinebanking.entity.AuditEventType;
import com.onlinebanking.entity.Beneficiary;
import com.onlinebanking.entity.BeneficiaryStatus;
import com.onlinebanking.entity.User;
import com.onlinebanking.exception.ApiException;
import com.onlinebanking.repository.BeneficiaryRepository;
import com.onlinebanking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public BeneficiaryResponse addBeneficiary(BeneficiaryRequest request, String ownerEmail, String remoteAddress) {
        User currentUser = findUserByEmail(ownerEmail);
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setBeneficiaryName(request.getBeneficiaryName());
        beneficiary.setBeneficiaryAccountNumber(request.getBeneficiaryAccountNumber());
        beneficiary.setBeneficiaryBank(request.getBeneficiaryBank());
        beneficiary.setStatus(BeneficiaryStatus.PENDING);
        beneficiary.setOwner(currentUser);

        Beneficiary saved = beneficiaryRepository.save(beneficiary);

        auditLogService.logEvent(
                AuditEventType.ADMIN_ACTION,
                "Added beneficiary " + saved.getBeneficiaryName() + " (" + saved.getBeneficiaryAccountNumber() + ") in PENDING status",
                remoteAddress,
                currentUser
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiaries(String ownerEmail) {
        User currentUser = findUserByEmail(ownerEmail);
        return beneficiaryRepository.findByOwnerId(currentUser.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBeneficiary(Long id, String ownerEmail, String remoteAddress) {
        User currentUser = findUserByEmail(ownerEmail);
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ApiException("Beneficiary not found"));

        if (!beneficiary.getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException("You do not have permission to delete this beneficiary");
        }

        beneficiaryRepository.delete(beneficiary);

        auditLogService.logEvent(
                AuditEventType.ADMIN_ACTION,
                "Deleted beneficiary " + beneficiary.getBeneficiaryName(),
                remoteAddress,
                currentUser
        );
    }

    @Transactional
    public BeneficiaryResponse verifyBeneficiary(Long id, String ownerEmail, String remoteAddress) {
        User currentUser = findUserByEmail(ownerEmail);
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ApiException("Beneficiary not found"));

        if (!beneficiary.getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException("You do not have permission to verify this beneficiary");
        }

        if (beneficiary.getStatus() == BeneficiaryStatus.ACTIVE) {
            throw new ApiException("Beneficiary is already active");
        }

        beneficiary.setStatus(BeneficiaryStatus.ACTIVE);
        Beneficiary saved = beneficiaryRepository.save(beneficiary);

        auditLogService.logEvent(
                AuditEventType.ADMIN_ACTION,
                "Verified and activated beneficiary " + saved.getBeneficiaryName(),
                remoteAddress,
                currentUser
        );

        return toResponse(saved);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getBeneficiaryName(),
                beneficiary.getBeneficiaryAccountNumber(),
                beneficiary.getBeneficiaryBank(),
                beneficiary.getStatus(),
                beneficiary.getCreatedAt()
        );
    }
}
