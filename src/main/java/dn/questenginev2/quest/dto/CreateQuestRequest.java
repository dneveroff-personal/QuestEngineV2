package dn.questenginev2.quest.dto;

import dn.questenginev2.quest.entity.QuestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateQuestRequest(
    @NotBlank(message = "Название квеста не может быть пустым")
        @Size(min = 1, max = 255, message = "Название квеста должно быть от 1 до 255 символов")
        String title,
    @NotBlank(message = "Описание квеста не может быть пустым")
        @Size(max = 5000, message = "Описание квеста должно быть не более 5000 символов")
        String description,
    @NotNull(message = "Тип квеста не может быть пустым") QuestType type,
    Instant startTime,
    Instant finishTime) {}
