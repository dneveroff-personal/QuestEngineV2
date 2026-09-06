package dn.questenginev2.bonuspenalty.dto;

import dn.questenginev2.bonuspenalty.entity.TimeAdjustmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateManualTimeAdjustmentRequest(
    @NotNull(message = "Тип корректировки не может быть пустым") TimeAdjustmentType type,
    @NotNull(message = "Количество секунд не может быть пустым")
        @Positive(message = "Количество секунд должно быть больше 0")
        Integer seconds,
    @NotBlank(message = "Причина не может быть пустой")
        @Size(max = 1000, message = "Причина должна быть не более 1000 символов")
        String reason) {}
