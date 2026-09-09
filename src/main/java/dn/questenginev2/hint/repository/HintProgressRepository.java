package dn.questenginev2.hint.repository;

import dn.questenginev2.hint.entity.HintProgress;
import dn.questenginev2.hint.entity.HintType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HintProgressRepository extends JpaRepository<HintProgress, Long> {

  List<HintProgress> findByLevelProgressIdOrderByShownAt(Long levelProgressId);

  boolean existsByLevelProgressIdAndHintId(Long levelProgressId, Long hintId);

  Optional<HintProgress> findByLevelProgressIdAndHintId(Long levelProgressId, Long hintId);

  /**
   * Эффект BONUS/PENALTY-подсказок для агрегации итогового времени
   * (ADR-0007, ADR-0021, bonus-penalty.md). Наличие записи HintProgress
   * для BONUS/PENALTY-подсказки означает, что команда явно её взяла
   * (HintProgressController.takeHint) — REGULAR-подсказки эффекта не дают,
   * поэтому фильтр по типу обязателен.
   */
  @Query(
      "SELECT COALESCE(SUM(hp.hint.bonusPenaltySeconds), 0) FROM HintProgress hp "
          + "WHERE hp.levelProgress.questProgress.id = :questProgressId AND hp.hint.type = :type")
  long sumEffectSecondsByQuestProgressIdAndHintType(
      @Param("questProgressId") Long questProgressId, @Param("type") HintType type);
}
