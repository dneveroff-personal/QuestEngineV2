package dn.questenginev2.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetAdminPasswordRequest(
    @NotBlank(message = "Новый пароль не может быть пустым") String newPassword) {}
