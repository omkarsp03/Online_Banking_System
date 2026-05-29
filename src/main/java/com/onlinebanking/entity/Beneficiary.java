package com.onlinebanking.entity;

import com.onlinebanking.util.AttributeEncryptor;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "beneficiary_name", nullable = false, length = 150)
    private String beneficiaryName;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "beneficiary_account_number", nullable = false, length = 256)
    private String beneficiaryAccountNumber;

    @Column(name = "beneficiary_bank", nullable = false, length = 150)
    private String beneficiaryBank;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BeneficiaryStatus status = BeneficiaryStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;
}
