package dn.questenginev2.quest.dto;

import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record QuestFilterRequest(
    QuestStatus status,
    QuestType type,
    Instant startTimeAfter,
    Instant finishTimeBefore,
    @Size(max = 100) String title,
    @Size(max = 500) String description) {}
