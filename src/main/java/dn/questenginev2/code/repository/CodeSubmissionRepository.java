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
   * bonus-penalty.md). Поле называется `points` в Code (переименование в
   * `bonusPenaltySeconds` — открытый пункт в backlog, не выполнено, т.к.
   * потребовало бы правки существующих тестов, см. backlog.md), но
   * семантически это уже секунды, а не очки.
   */
  @Query(
      "SELECT COALESCE(SUM(cs.matchedCode.points), 0) FROM CodeSubmission cs "
          + "WHERE cs.levelProgress.questProgress.id = :questProgressId AND cs.result = :result")
  long sumEffectSecondsByQuestProgressIdAndResult(
      @Param("questProgressId") Long questProgressId, @Param("result") CodeSubmissionResult result);
}
