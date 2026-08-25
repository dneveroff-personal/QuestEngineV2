package dn.questenginev2.code.repository;

import dn.questenginev2.code.entity.CodeSubmission;
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
}
