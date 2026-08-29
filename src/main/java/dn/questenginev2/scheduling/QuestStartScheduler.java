package dn.questenginev2.scheduling;

import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestRegistration;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.RegistrationStatus;
import dn.questenginev2.quest.repository.QuestRegistrationRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.quest.service.QuestProgressService;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job 1 (docs/03-architecture/scheduling.md): переводит Quest из REGISTRATION в RUNNING по
 * достижении Quest.startTime и создаёт QuestProgress для всех APPROVED-регистраций.
 *
 * <p>LevelProgress первого уровня НЕ создаётся здесь (eager) — только лениво, при первом входе
 * команды (см. ADR-0002 в финальной редакции).
 */
@Component
@AllArgsConstructor
@Slf4j
public class QuestStartScheduler {

  private final QuestRepository questRepository;
  private final QuestRegistrationRepository questRegistrationRepository;
  private final QuestProgressService questProgressService;
  private final Clock clock;

  @Autowired
  public QuestStartScheduler(
      QuestRepository questRepository,
      QuestRegistrationRepository questRegistrationRepository,
      QuestProgressService questProgressService) {
    this(questRepository, questRegistrationRepository, questProgressService, Clock.systemUTC());
  }

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void startDueQuests() {
    Instant now = clock.instant();
    List<Quest> candidates =
        questRepository.findByStatusAndStartTimeLessThanEqual(QuestStatus.REGISTRATION, now);

    for (Quest quest : candidates) {
      int updatedRows = questRepository.tryStartQuest(quest.getId());
      if (updatedRows == 1) {
        createProgressForApprovedTeams(quest.getId());
      }
    }
  }

  private void createProgressForApprovedTeams(Long questId) {
    List<QuestRegistration> approvedRegistrations =
        questRegistrationRepository.findByQuestIdAndStatus(questId, RegistrationStatus.APPROVED);

    for (QuestRegistration registration : approvedRegistrations) {
      Long teamId = registration.getTeam().getId();
      try {
        questProgressService.createProgress(questId, teamId);
      } catch (IllegalArgumentException alreadyExists) {
        // Сценарий 7 (docs/02-processes/concurrency-scenarios.md): QuestProgress для этой
        // команды уже создан параллельно (например, через approveTeam() "подтверждение после
        // старта"). Идемпотентно пропускаем, не прерывая обработку остальных команд.
        log.debug(
            "QuestProgress уже существует для quest={}, team={} — пропускаем (Сценарий 7)",
            questId,
            teamId);
      }
    }
  }
}
