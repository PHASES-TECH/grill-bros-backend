package com.grill_bros.backend.dto;

import com.grill_bros.backend.records.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAdminUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 200)
    private String fullName;

    @NotNull(message = "Role is required")
    private Role role;

}
