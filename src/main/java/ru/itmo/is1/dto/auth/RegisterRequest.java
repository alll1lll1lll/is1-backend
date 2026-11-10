package ru.itmo.is1.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username must be 3 to 50 characters long")
    @Pattern(regexp = "[A-Za-z0-9_.-]*", message = "username may contain only letters, digits, '.', '_' and '-'")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 72, message = "password must be 6 to 72 characters long")
    private String password;
}
