package com.skillnez.cloudstorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserRegistrationResponseDto {

    private Long id;

    @NotBlank(message = "Account name can't be blank")
    @Size(min = 4, max = 32, message = "Account name must be at least 4 and no more than 32 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can contain only Latin letters and digits without spaces")
    private String username;
}
