package com.onlinebanking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleChangeRequest {

    @NotBlank(message = "Role name is required")
    private String roleName;
}
