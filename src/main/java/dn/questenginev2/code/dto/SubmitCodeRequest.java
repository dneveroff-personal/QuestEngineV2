package dn.questenginev2.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitCodeRequest(
    @NotBlank(message = "Код не может быть пустым")
        @Size(max = 255, message = "Код должен быть не более 255 символов")
        String value) {}
