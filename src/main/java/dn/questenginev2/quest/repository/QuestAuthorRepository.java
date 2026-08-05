package dn.questenginev2.quest.repository;

import dn.questenginev2.quest.entity.QuestAuthor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestAuthorRepository extends JpaRepository<QuestAuthor, Long> {

  boolean existsByQuestId(Long questId);

  boolean existsByQuestIdAndUserId(Long questId, Long userId);

  void deleteByQuestId(Long questId);

  @Query("SELECT qa FROM QuestAuthor qa JOIN FETCH qa.quest WHERE qa.user.id = :userId")
  List<QuestAuthor> findByUserId(@Param("userId") Long userId);
}
