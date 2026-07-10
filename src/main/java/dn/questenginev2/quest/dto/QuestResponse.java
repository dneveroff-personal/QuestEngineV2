package dn.questenginev2.quest.dto;

import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestResponse {

    private Long id;
    private String title;
    private String description;
    private QuestType type;
    private QuestStatus status;
    private Instant createdAt;
    private Instant startTime;
    private Instant finishTime;
}
