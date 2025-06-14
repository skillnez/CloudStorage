package com.skillnez.cloudstorage.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserRegistrationRequestDto {

    @NotBlank(message = "Account name can't be blank")
    @Size(min = 4, max = 32, message = "Account name must be at least 4 and no more than 32 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can contain only Latin letters and digits without spaces")
    private String username;

    @NotBlank(message = "Password can't be blank")
    @Size(min = 4, max = 64, message = "Password must be at least 4 and no more than 64 characters")
    @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*()_+\\-={}|\\[\\]:;\"'<>,.?/~`]+$",
            message = "Password can contain only letters, digits and special characters without spaces")
    private String password;

    private String repeatPassword;

}
