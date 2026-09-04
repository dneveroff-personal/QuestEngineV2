package dn.questenginev2.hint.repository;

import dn.questenginev2.hint.entity.HintProgress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HintProgressRepository extends JpaRepository<HintProgress, Long> {

  List<HintProgress> findByLevelProgressIdOrderByShownAt(Long levelProgressId);

  boolean existsByLevelProgressIdAndHintId(Long levelProgressId, Long hintId);
}
