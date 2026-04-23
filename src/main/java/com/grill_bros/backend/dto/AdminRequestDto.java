package com.grill_bros.backend.dto;

import com.grill_bros.backend.records.Role;
import lombok.Builder;
import lombok.Data;

@Data
public class AdminRequestDto {

    private String email;
    private String fullName;
    private String phoneNumber;
    private String password;
    private Role role;

}
