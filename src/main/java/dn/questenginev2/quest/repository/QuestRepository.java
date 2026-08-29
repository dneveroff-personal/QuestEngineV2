package dn.questenginev2.quest.repository;

import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestShortProjection;
import dn.questenginev2.quest.entity.QuestStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestRepository extends JpaRepository<Quest, Long> {

  List<Quest> findByStatus(QuestStatus status);

  List<Quest> findByStatus(QuestStatus status, Pageable pageable);

  List<QuestShortProjection> findAllByStartTimeAfter(Instant startTimeAfter);

  List<QuestShortProjection> findAllByStartTimeAfter(Instant startTimeAfter, Pageable pageable);

  // Кандидаты на автостарт (03-architecture/scheduling.md, Job 1)
  List<Quest> findByStatusAndStartTimeLessThanEqual(QuestStatus status, Instant startTime);

  /**
   * Атомарный переход REGISTRATION -> RUNNING (аналог паттерна ADR-0010): побеждает только один
   * конкурентный вызов на конкретный Quest. Возвращает 1, если этот вызов выполнил переход, 0 —
   * если Quest уже не в REGISTRATION (переход уже произошёл).
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE Quest q SET q.status = dn.questenginev2.quest.entity.QuestStatus.RUNNING WHERE q.id ="
          + " :questId AND q.status = dn.questenginev2.quest.entity.QuestStatus.REGISTRATION")
  int tryStartQuest(@Param("questId") Long questId);
}
