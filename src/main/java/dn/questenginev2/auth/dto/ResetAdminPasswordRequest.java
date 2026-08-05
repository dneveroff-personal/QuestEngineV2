package dn.questenginev2.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetAdminPasswordRequest(
    @NotBlank(message = "Новый пароль не может быть пустым")
        @Size(min = 5, message = "Пароль должен быть не менее 5 символов")
        String newPassword) {}
