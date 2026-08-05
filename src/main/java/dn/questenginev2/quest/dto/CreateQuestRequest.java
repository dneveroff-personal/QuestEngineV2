package dn.questenginev2.quest.dto;

import dn.questenginev2.quest.entity.QuestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateQuestRequest(
    @NotBlank @Size(min = 1, max = 255) String title,
    String description,
    QuestType type,
    Instant startTime,
    Instant finishTime) {}
