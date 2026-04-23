package com.grill_bros.backend.dto;

import lombok.Data;

@Data
public class UserEmailLoginDto {

//    @Email(message = "Invalid email address")
//    @NotBlank
    private String email;
    private String password;
}
