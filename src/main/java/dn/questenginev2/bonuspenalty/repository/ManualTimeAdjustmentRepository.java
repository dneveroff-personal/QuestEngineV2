package dn.questenginev2.bonuspenalty.repository;

import dn.questenginev2.bonuspenalty.entity.ManualTimeAdjustment;
import dn.questenginev2.bonuspenalty.entity.TimeAdjustmentType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManualTimeAdjustmentRepository extends JpaRepository<ManualTimeAdjustment, Long> {

  @Query(
      """
      select coalesce(sum(m.seconds), 0)
      from ManualTimeAdjustment m
      where m.questProgress.id = :questProgressId
        and m.type = :type
        and m.revokedAt is null
      """)
  long sumActiveSecondsByQuestProgressIdAndType(
      @Param("questProgressId") Long questProgressId, @Param("type") TimeAdjustmentType type);

  List<ManualTimeAdjustment> findByQuestProgressIdOrderByCreatedAt(Long questProgressId);
}
