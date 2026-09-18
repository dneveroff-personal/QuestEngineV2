package dn.questenginev2.statistic.dto;

import dn.questenginev2.quest.entity.QuestStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Snapshot of live/final ranking table for a quest (statistics-ranking.md). SSE is backlog #19. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestStatisticsResponse {

  private Long questId;
  private String questTitle;
  private QuestStatus questStatus;
  private List<StatisticsLevelColumn> levels;
  private List<StatisticsTeamRow> rows;
}
