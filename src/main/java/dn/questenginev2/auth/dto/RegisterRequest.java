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

    @Email(message = "Email should be valid")
    private String email;

    @Size(max = 128, message = "Public name must be at most 128 characters")
    private String publicName;

}
