package dn.questenginev2.code.repository;

import dn.questenginev2.code.entity.Code;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeRepository extends JpaRepository<Code, Long> {

  boolean existsByLevelIdAndValue(Long levelId, String value);

  List<Code> findByLevelIdOrderByCreatedAt(Long levelId);
}
