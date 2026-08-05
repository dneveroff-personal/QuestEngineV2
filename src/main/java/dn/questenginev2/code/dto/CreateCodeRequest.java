package dn.questenginev2.code.dto;

import dn.questenginev2.code.entity.CodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCodeRequest(
    @NotBlank(message = "Значение кода не может быть пустым")
        @Size(max = 255, message = "Значение кода должно быть не более 255 символов")
        String value,
    @NotNull(message = "Тип кода не может быть пустым") CodeType type,
    Integer points) {}
