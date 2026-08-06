package dn.questenginev2.quest.dto;

import dn.questenginev2.quest.entity.QuestShortProjection;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestResponseBrief implements QuestShortProjection {

  private Long id;
  private String title;
  private Instant startTime;

  @Override
  public Instant startTime() {
    return startTime;
  }
}
