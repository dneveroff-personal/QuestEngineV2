package dn.questenginev2.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetAdminPasswordRequest {

    @NotBlank(message = "Новый пароль не может быть пустым")
    private String newPassword;
}
