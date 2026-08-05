package dn.questenginev2.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public abstract class AuthRequestBase {

  @NotBlank(message = "Имя пользователя не может быть пустым")
  @Size(min = 3, max = 64, message = "Имя пользователя должно быть от 3 до 64 символов")
  private String username;

  @NotBlank(message = "Пароль не может быть пустым")
  @Size(min = 5, message = "Пароль должен быть не менее 5 символов")
  private String password;
}
