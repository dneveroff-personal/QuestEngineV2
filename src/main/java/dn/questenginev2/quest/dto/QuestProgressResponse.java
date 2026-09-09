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

  private Long id;
  private String teamName;
  private QuestProgressStatus status;
  private Instant questStartedAt;
  private Instant endedAt;
  private Instant finishedAt;

  /**
   * Агрегат трёх источников (ADR-0007, bonus-penalty.md): ManualTimeAdjustment
   * + эффект кодов + эффект подсказок. Положительное значение — штраф
   * (увеличивает итоговое время), отрицательное — бонус (уменьшает).
   * Считается на каждый запрос, не хранится (см. BonusPenaltyServiceImpl).
   */
  private Long bonusPenaltySeconds;
}
