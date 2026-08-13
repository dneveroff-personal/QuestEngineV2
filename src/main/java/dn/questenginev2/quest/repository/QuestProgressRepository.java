package dn.questenginev2.quest.repository;

import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestProgressRepository extends JpaRepository<QuestProgress, Long> {

  @EntityGraph(attributePaths = {"team"})
  Optional<QuestProgress> findByQuestIdAndTeamId(Long questId, Long teamId);

  boolean existsByQuestIdAndTeamId(Long questId, Long teamId);

  Optional<QuestProgress> findByQuestIdAndTeamIdAndStatus(
      Long questId, Long teamId, QuestProgressStatus status);

  @EntityGraph(attributePaths = {"team"})
  List<QuestProgress> findByQuestId(Long questId);
}
