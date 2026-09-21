package dn.questenginev2.statistic.dto;

import dn.questenginev2.quest.entity.QuestProgressStatus;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsTeamRow {

  private Long teamId;
  private String teamName;
  private QuestProgressStatus progressStatus;

  /** 1-based place; null if not ranked (e.g. still on first level during RUNNING). */
  private Integer rank;

  private int levelsCompleted;
  private Integer lastCompletedLevelOrderIndex;
  private Instant lastLevelCompletedAt;

  private List<StatisticsLevelCell> levelCells;

  /** Aggregated bonus/penalty seconds (positive = penalty). */
  private Long bonusPenaltySeconds;

  /**
   * Final ranking key for FINISHED teams: wall time from quest start to finish plus
   * bonusPenaltySeconds. Null for non-finished progress.
   */
  private Long totalTimeSeconds;
}
