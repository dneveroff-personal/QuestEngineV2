package dn.questenginev2.level.repository;

import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LevelProgressRepository extends JpaRepository<LevelProgress, Long> {

  Optional<LevelProgress> findByQuestProgressIdAndLevelId(Long questProgressId, Long levelId);

  Optional<LevelProgress> findByQuestProgressIdAndStatus(
      Long questProgressId, LevelProgressStatus status);

  boolean existsByQuestProgressIdAndLevelId(Long questProgressId, Long levelId);

  /**
   * Атомарный переход ACTIVE -> COMPLETED, выполняемый только если количество различных решённых
   * кодов (по CodeSubmission.result = CORRECT_MAIN, сгруппированных по Code.codeIndex) достигло
   * requiredCount. Считает порог прямо в WHERE, без предварительного чтения счётчика в коде
   * приложения — под высокой конкурентной нагрузкой (десятки попыток в секунду на одну и ту же
   * строку) только один конкурентный вызов "выигрывает" переход. См.
   * docs/02-processes/concurrency-scenarios.md, Сценарий 6.
   *
   * @return количество обновлённых строк: 1, если этот вызов выполнил переход, 0 — если уровень
   *     уже не ACTIVE (переход уже произошёл параллельно, либо статус изменён иначе) или порог
   *     ещё не достигнут.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          UPDATE level_progress
          SET status = 'COMPLETED', completed_at = :now
          WHERE id = :levelProgressId
            AND status = 'ACTIVE'
            AND (
              SELECT COUNT(DISTINCT c.code_index)
              FROM code_submissions cs
              JOIN codes c ON c.id = cs.matched_code_id
              WHERE cs.level_progress_id = :levelProgressId
                AND cs.result = 'CORRECT_MAIN'
            ) >= :requiredCount
          """,
      nativeQuery = true)
  int tryCompleteByCodes(
      @Param("levelProgressId") Long levelProgressId,
      @Param("requiredCount") long requiredCount,
      @Param("now") Instant now);
}
