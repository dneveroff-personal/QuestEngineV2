package dn.questenginev2.level.repository;

import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelProgressRepository extends JpaRepository<LevelProgress, Long> {

  Optional<LevelProgress> findByQuestProgressIdAndLevelId(Long questProgressId, Long levelId);

  Optional<LevelProgress> findByQuestProgressIdAndStatus(
      Long questProgressId, LevelProgressStatus status);

  boolean existsByQuestProgressIdAndLevelId(Long questProgressId, Long levelId);
}
