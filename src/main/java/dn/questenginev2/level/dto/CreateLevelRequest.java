package dn.questenginev2.level.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLevelRequest(
    @NotBlank(message = "Название уровня не может быть пустым")
        @Size(max = 255, message = "Название уровня должно быть не более 255 символов")
        String title,
    @Size(max = 10000, message = "Содержимое уровня должно быть не более 10000 символов")
        String content,
    @Min(value = 1, message = "Таймаут должен быть не менее 1 секунды") Integer timeoutSeconds,
    @Min(value = 1, message = "Количество необходимых MAIN-кодов должно быть не менее 1")
        Integer requiredMainCodesCount) {}
