package dn.questenginev2.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RegisterRequest extends AuthRequestBase {

  @NotBlank(message = "Email не может быть пустым")
  @Email(message = "Email должен быть валидным")
  @Size(max = 254, message = "Email должен быть не более 254 символов")
  private String email;

  @Size(max = 128, message = "Публичное имя должно быть не более 128 символов")
  private String publicName;
}
