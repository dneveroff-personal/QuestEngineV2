package dn.questenginev2.quest.repository;

import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestShortProjection;
import dn.questenginev2.quest.entity.QuestStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

  /**
   * Пессимистичная блокировка строки Quest (ADR-0010, Сценарий 1 — concurrency-scenarios.md).
   * Используется перед подсчётом APPROVED-регистраций в {@code approveTeam()}, чтобы
   * сериализовать конкурентные подтверждения заявок для одного и того же Quest и не допустить
   * превышения лимита команд (read-then-write без блокировки — подтверждённая уязвимость).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT q FROM Quest q WHERE q.id = :questId")
  Optional<Quest> findByIdForUpdate(@Param("questId") Long questId);
}
