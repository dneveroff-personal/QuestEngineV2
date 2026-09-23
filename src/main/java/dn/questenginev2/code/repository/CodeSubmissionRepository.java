package dn.questenginev2.code.repository;

import dn.questenginev2.code.entity.CodeSubmission;
import dn.questenginev2.code.entity.CodeSubmissionResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {

  List<CodeSubmission> findByLevelProgressIdOrderBySubmittedAtDesc(Long levelProgressId);

  long countByLevelProgressIdAndSubmittedById(Long levelProgressId, Long submittedById);

  @Query(
      "SELECT COUNT(DISTINCT cs.matchedCode.codeIndex) FROM CodeSubmission cs "
          + "WHERE cs.levelProgress.id = :levelProgressId AND cs.result = 'CORRECT_MAIN'")
  long countDistinctSolvedCodeIndexes(@Param("levelProgressId") Long levelProgressId);

  /**
   * Эффект BONUS/PENALTY-кодов для агрегации итогового времени (ADR-0007,
   * bonus-penalty.md).
   */
  @Query(
      "SELECT COALESCE(SUM(cs.matchedCode.bonusPenaltySeconds), 0) FROM CodeSubmission cs "
          + "WHERE cs.levelProgress.questProgress.id = :questProgressId AND cs.result = :result")
  long sumEffectSecondsByQuestProgressIdAndResult(
      @Param("questProgressId") Long questProgressId, @Param("result") CodeSubmissionResult result);

  /**
   * Проверка одноразового применения BONUS/PENALTY-кода (bonus-penalty.md):
   * один и тот же код не должен засчитываться повторно в рамках одного прохождения.
   */
  @Query(
      "SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END FROM CodeSubmission cs "
          + "WHERE cs.levelProgress.questProgress.id = :questProgressId "
          + "AND cs.matchedCode.id = :matchedCodeId "
          + "AND cs.result = :result")
  boolean existsByQuestProgressIdAndMatchedCodeIdAndResult(
      @Param("questProgressId") Long questProgressId,
      @Param("matchedCodeId") Long matchedCodeId,
      @Param("result") CodeSubmissionResult result);

  /**
   * Попытки на одном LevelProgress с подгрузкой submittedBy и matchedCode
   * (для GET команды на активном уровне).
   */
  @Query(
      "SELECT cs FROM CodeSubmission cs "
          + "JOIN FETCH cs.submittedBy "
          + "LEFT JOIN FETCH cs.matchedCode "
          + "WHERE cs.levelProgress.id = :levelProgressId "
          + "ORDER BY cs.submittedAt DESC")
  List<CodeSubmission> findDetailedByLevelProgressIdOrderBySubmittedAtDesc(
      @Param("levelProgressId") Long levelProgressId);

  /**
   * Все попытки квеста (все команды, все уровни) с контекстом team/level
   * (для GET автора).
   */
  @Query(
      "SELECT cs FROM CodeSubmission cs "
          + "JOIN FETCH cs.submittedBy "
          + "LEFT JOIN FETCH cs.matchedCode "
          + "JOIN FETCH cs.levelProgress lp "
          + "JOIN FETCH lp.level "
          + "JOIN FETCH lp.questProgress qp "
          + "JOIN FETCH qp.team "
          + "WHERE qp.quest.id = :questId "
          + "ORDER BY cs.submittedAt DESC")
  List<CodeSubmission> findDetailedByQuestIdOrderBySubmittedAtDesc(
      @Param("questId") Long questId);
}
