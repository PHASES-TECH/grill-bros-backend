package com.grill_bros.backend.dto;

import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.records.Role;

import java.util.UUID;

public record UserResponseDto(
        String phoneNumber,
        String fullName,
        Role role
) {
    public static UserResponseDto from(Users user) {
        return new UserResponseDto(
                user.getPhoneNumber(),
                user.getFullName(),
                user.getRole()

        );
    }
}
