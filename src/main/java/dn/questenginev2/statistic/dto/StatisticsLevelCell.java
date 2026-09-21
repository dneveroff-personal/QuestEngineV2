package dn.questenginev2.statistic.dto;

import dn.questenginev2.level.entity.LevelProgressStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsLevelCell {

  private Integer orderIndex;
  private Long levelId;
  private LevelProgressStatus status;
  private Instant openedAt;
  private Instant completedAt;
}
