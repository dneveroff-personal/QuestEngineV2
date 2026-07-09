package dn.questenginev2.quest.dto;

import dn.questenginev2.quest.entity.QuestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateQuestRequest {

    @NotBlank
    @Size(min = 1, max = 255)
    private String title;

    private String description;
    private QuestType type;
    private Instant startAt;
    private Instant endAt;

}
