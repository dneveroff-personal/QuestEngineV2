package dn.questenginev2.scheduling;

import dn.questenginev2.gameplay.event.GameplayEventPublisher;
import dn.questenginev2.gameplay.event.GameplayEventType;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.quest.dto.QuestProgressResponse;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.service.QuestProgressService;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job 2 (docs/03-architecture/scheduling.md): переводит LevelProgress из ACTIVE в
 * AUTO_TRANSITIONED по достижении autoTransitionAt, затем открывает следующий уровень либо
 * завершает QuestProgress (ADR-0009 — без разницы со способом завершения CODES).
 *
 * <p>Использует атомарный {@link LevelProgressRepository#tryAutoTransition}, а не устаревший
 * {@code LevelProgressService#autoTransitionLevel} — последний не защищён от гонки с
 * CodeSubmission (см. docs/02-processes/concurrency-scenarios.md, Сценарий 5).
 *
 * <p>ADR-022: публикует {@code LEVEL_AUTO_TRANSITIONED} и при необходимости {@code QUEST_FINISHED}.
 */
@Component
@AllArgsConstructor
public class LevelAutoTransitionScheduler {

  private final LevelProgressRepository levelProgressRepository;
  private final QuestProgressService questProgressService;
  private final GameplayEventPublisher gameplayEventPublisher;
  private final Clock clock;

  @Autowired
  public LevelAutoTransitionScheduler(
      LevelProgressRepository levelProgressRepository,
      QuestProgressService questProgressService,
      GameplayEventPublisher gameplayEventPublisher) {
    this(
        levelProgressRepository, questProgressService, gameplayEventPublisher, Clock.systemUTC());
  }

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void autoTransitionDueLevels() {
    Instant now = clock.instant();
    List<LevelProgress> candidates =
        levelProgressRepository.findByStatusAndAutoTransitionAtLessThanEqual(
            LevelProgressStatus.ACTIVE, now);

    for (LevelProgress levelProgress : candidates) {
      // Capture ids before atomic update (entity may be detached / associations lazy).
      Long levelProgressId = levelProgress.getId();
      Long questProgressId = levelProgress.getQuestProgress().getId();
      Long questId = levelProgress.getQuestProgress().getQuest().getId();
      Long levelId = levelProgress.getLevel().getId();

      int updatedRows = levelProgressRepository.tryAutoTransition(levelProgressId, now);

      if (updatedRows == 1) {
        LevelProgress transitioned =
            levelProgressRepository
                .findById(levelProgressId)
                .orElseThrow(
                    () -> new IllegalStateException("LevelProgress исчез во время обработки"));
        QuestProgressResponse advanceResponse =
            questProgressService.advanceAfterLevelCompleted(transitioned);

        gameplayEventPublisher.publish(
            GameplayEventType.LEVEL_AUTO_TRANSITIONED,
            questId,
            questProgressId,
            levelProgressId,
            Map.of("levelId", levelId));

        if (advanceResponse.getStatus() == QuestProgressStatus.FINISHED) {
          gameplayEventPublisher.publish(
              GameplayEventType.QUEST_FINISHED,
              questId,
              questProgressId,
              levelProgressId,
              Map.of());
        }
      }
      // updatedRows == 0: уровень уже завершён параллельно (CodeSubmission выиграл гонку,
      // Сценарий 5) — идемпотентно пропускаем.
    }
  }
}
