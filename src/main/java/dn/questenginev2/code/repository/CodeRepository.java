package dn.questenginev2.code.repository;

import dn.questenginev2.code.entity.Code;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeRepository extends JpaRepository<Code, Long> {

  boolean existsByLevelIdAndValue(Long levelId, String value);

  List<Code> findByLevelIdOrderByCreatedAt(Long levelId);

  boolean existsByLevelId(Long levelId);

  /** Число различных MAIN codeIndex на уровне (порог requiredMainCodesCount, ADR-0005). */
  @Query(
      "SELECT COUNT(DISTINCT c.codeIndex) FROM Code c "
          + "WHERE c.level.id = :levelId AND c.type = 'MAIN'")
  long countDistinctMainCodeIndexesByLevelId(@Param("levelId") Long levelId);
}
