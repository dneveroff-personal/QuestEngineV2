package dn.questenginev2.quest.dto;

import dn.questenginev2.quest.entity.QuestProgressStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestProgressResponse {

  private String teamName;
  private QuestProgressStatus status;
  private Instant questStartedAt;
  private Instant endedAt;
  private Instant finishedAt;
}
