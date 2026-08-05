package dn.questenginev2.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "Новый пароль не может быть пустым")
        @Size(min = 6, message = "Пароль должен быть не менее 6 символов")
        String newPassword) {}
