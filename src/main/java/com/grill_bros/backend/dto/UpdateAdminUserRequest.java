package com.grill_bros.backend.dto;

import com.grill_bros.backend.records.Role;
import lombok.Data;

@Data
public class UpdateAdminUserRequest {

    private String fullName;
    private Role role;
}
