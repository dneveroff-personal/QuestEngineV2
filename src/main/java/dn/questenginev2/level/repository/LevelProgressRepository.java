package dn.questenginev2.level.repository;

import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LevelProgressRepository extends JpaRepository<LevelProgress, Long> {

  Optional<LevelProgress> findByQuestProgressIdAndLevelId(Long questProgressId, Long levelId);

  Optional<LevelProgress> findByQuestProgressIdAndStatus(
      Long questProgressId, LevelProgressStatus status);

  Optional<LevelProgress> findTopByQuestProgressIdOrderByIdDesc(Long questProgressId);

  List<LevelProgress> findByStatus(LevelProgressStatus status);

  List<LevelProgress> findByStatusAndAutoTransitionAtLessThanEqual(
      LevelProgressStatus status, Instant autoTransitionAt);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE level_progress SET status = 'AUTO_TRANSITIONED', completed_at = :now "
              + "WHERE id = :levelProgressId AND status = 'ACTIVE' AND auto_transition_at <= :now",
      nativeQuery = true)
  int tryAutoTransition(@Param("levelProgressId") Long levelProgressId, @Param("now") Instant now);

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
  int tryCompleteByCodesThreshold(
      @Param("levelProgressId") Long levelProgressId,
      @Param("requiredCount") long requiredCount,
      @Param("now") Instant now);

  /** All level progress for a quest (statistics ranking). */
  @Query(
      "SELECT lp FROM LevelProgress lp JOIN FETCH lp.level JOIN FETCH lp.questProgress qp"
          + " JOIN FETCH qp.team WHERE qp.quest.id = :questId")
  List<LevelProgress> findAllByQuestId(@Param("questId") Long questId);

  List<LevelProgress> findByQuestProgressId(Long questProgressId);
}
