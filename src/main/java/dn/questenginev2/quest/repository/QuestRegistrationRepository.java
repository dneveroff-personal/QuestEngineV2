package dn.questenginev2.quest.repository;

import dn.questenginev2.quest.entity.QuestRegistration;
import dn.questenginev2.quest.entity.RegistrationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestRegistrationRepository extends JpaRepository<QuestRegistration, Long> {

  @EntityGraph(attributePaths = {"team", "quest"})
  List<QuestRegistration> findByQuestId(Long questId);

  Optional<QuestRegistration> findByQuestIdAndTeamId(Long questId, Long teamId);

  Optional<QuestRegistration> findByQuestIdAndTeamIdAndStatus(
      Long questId, Long teamId, RegistrationStatus status);

  List<QuestRegistration> findByTeamIdAndQuestIdAndStatus(
      Long teamId, Long questId, RegistrationStatus status);

  boolean existsByQuestIdAndTeamId(Long questId, Long teamId);

  long countByQuestIdAndStatus(Long questId, RegistrationStatus status);
}
