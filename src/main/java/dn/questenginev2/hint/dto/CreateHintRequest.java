package dn.questenginev2.hint.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateHintRequest(
    @NotNull(message = "Порядковый номер подсказки не может быть пустым")
        @Min(value = 0, message = "Порядковый номер подсказки должен быть не менее 0")
        Integer orderIndex,
    @NotNull(message = "Задержка не может быть пустой")
        @Min(value = 0, message = "Задержка должна быть не менее 0 секунд")
        Integer delaySeconds,
    @NotBlank(message = "Содержимое подсказки не может быть пустым") String content) {}
