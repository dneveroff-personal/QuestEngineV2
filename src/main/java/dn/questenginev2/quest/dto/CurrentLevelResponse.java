package dn.questenginev2.quest.dto;

import dn.questenginev2.hint.dto.HintProgressResponse;
import dn.questenginev2.level.entity.LevelProgressStatus;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated Game Mode view for the team's current (ACTIVE) level: LevelProgress + level content +
 * autoTransitionAt + visible hints + main-code progress.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrentLevelResponse {

  private Long levelProgressId;
  private LevelProgressStatus levelProgressStatus;
  private Instant openedAt;
  private Instant autoTransitionAt;

  private Long levelId;
  private Integer orderIndex;
  private String title;
  private String content;
  private Integer requiredMainCodesCount;
  private Integer timeoutSeconds;

  /** Distinct CORRECT_MAIN code indexes solved on this level. */
  private long mainCodesSolved;

  /** Same contract as GET .../hints (REGULAR revealed, BONUS/PENALTY available or taken). */
  private List<HintProgressResponse> hints;
}
