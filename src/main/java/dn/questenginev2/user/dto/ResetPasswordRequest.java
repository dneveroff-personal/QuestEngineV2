package dn.questenginev2.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank(message = "Новый пароль не может быть пустым") String newPassword) {}
