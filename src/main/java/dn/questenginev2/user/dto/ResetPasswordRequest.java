package dn.questenginev2.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {

  @NotBlank(message = "Новый пароль не может быть пустым")
  private String newPassword;
}
