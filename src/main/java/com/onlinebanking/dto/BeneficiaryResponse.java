package com.onlinebanking.dto;

import com.onlinebanking.entity.BeneficiaryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryResponse {
    private Long id;
    private String beneficiaryName;
    private String beneficiaryAccountNumber;
    private String beneficiaryBank;
    private BeneficiaryStatus status;
    private Instant createdAt;
}
