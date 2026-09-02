package dn.questenginev2.level.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.level.dto.LevelProgressResponse;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.QuestProgress;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class LevelProgressServiceImpl implements LevelProgressService {

  private final LevelProgressRepository levelProgressRepository;
  private final LevelRepository levelRepository;
  private final Clock clock;

  @Autowired
  public LevelProgressServiceImpl(
      LevelProgressRepository levelProgressRepository, LevelRepository levelRepository) {
    this(levelProgressRepository, levelRepository, Clock.systemUTC());
  }

  public LevelProgressServiceImpl(
      LevelProgressRepository levelProgressRepository,
      LevelRepository levelRepository,
      Clock clock) {
    this.levelProgressRepository = levelProgressRepository;
    this.levelRepository = levelRepository;
    this.clock = clock;
  }

  // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
  @Override
  public LevelProgressResponse createFirstLevelProgress(QuestProgress questProgress) {
    if (questProgress == null || questProgress.getQuest() == null) {
      throw new IllegalArgumentException("QuestProgress не найден");
    }

    Long questId = questProgress.getQuest().getId();

    Level level =
        levelRepository
            .findByQuestIdAndOrderIndex(questId, 1)
            .orElseThrow(() -> new IllegalArgumentException("Уровень не найден"));

    validateLevelBelongsToQuest(level, questId);

    Instant openedAt = clock.instant();
    Instant autoTransitionAt = null;
    Integer timeOutTime = level.getTimeoutSeconds();

    if (timeOutTime != null) {
      autoTransitionAt = questProgress.getQuestStartedAt().plusSeconds(timeOutTime);
    }

    LevelProgressStatus status;
    Instant completedAt = null;

    if (autoTransitionAt != null && !openedAt.isBefore(autoTransitionAt)) {
      status = LevelProgressStatus.AUTO_TRANSITIONED;
      completedAt = openedAt;
    } else {
      status = LevelProgressStatus.ACTIVE;
    }

    LevelProgress levelProgress =
        LevelProgress.builder()
            .questProgress(questProgress)
            .level(level)
            .status(status)
            .openedAt(openedAt)
            .completedAt(completedAt)
            .autoTransitionAt(autoTransitionAt)
            .build();

    // ADR-0010, Сценарий 2 (concurrency-scenarios.md): вместо read-then-write проверки "уже
    // создан" перед вставкой — полагаемся на уникальный индекс (quest_progress_id, level_id) как
    // источник истины и обрабатываем его нарушение идемпотентно (двойной клик, повтор запроса
    // сетью — ожидаемый случай, не ошибка).
    try {
      LevelProgress saved = levelProgressRepository.saveAndFlush(levelProgress);
      return buildLevelProgressResponse(saved);
    } catch (DataIntegrityViolationException alreadyCreatedConcurrently) {
      LevelProgress existing =
          levelProgressRepository
              .findByQuestProgressIdAndLevelId(questProgress.getId(), level.getId())
              .orElseThrow(() -> alreadyCreatedConcurrently);
      return buildLevelProgressResponse(existing);
    }
  }

  @Override
  public LevelProgressResponse createNextLevelProgress(
      QuestProgress questProgress, Integer nextLevelOrderIndex) {
    Optional<Level> level =
        levelRepository.findByQuestIdAndOrderIndex(
            questProgress.getQuest().getId(), nextLevelOrderIndex);

    // Возвращаем null если в квесте не осталось больше уровней.
    if (level.isEmpty()) {
      return null;
    }

    Level nextLevel = level.get();

    Instant openedAt = clock.instant();
    Instant autoTransitionAt = null;
    Integer timeOutTime = nextLevel.getTimeoutSeconds();
    LevelProgressStatus status = LevelProgressStatus.ACTIVE;

    if (timeOutTime != null) {
      autoTransitionAt = openedAt.plusSeconds(timeOutTime);
    }

    LevelProgress levelProgress =
        LevelProgress.builder()
            .questProgress(questProgress)
            .level(nextLevel)
            .status(status)
            .openedAt(openedAt)
            .completedAt(null)
            .autoTransitionAt(autoTransitionAt)
            .build();

    // См. createFirstLevelProgress() — тот же идемпотентный паттерн (ADR-0010, Сценарий 2):
    // уникальный индекс (quest_progress_id, level_id) как источник истины вместо
    // read-then-write проверки перед вставкой.
    try {
      LevelProgress saved = levelProgressRepository.saveAndFlush(levelProgress);
      return buildLevelProgressResponse(saved);
    } catch (DataIntegrityViolationException alreadyCreatedConcurrently) {
      LevelProgress existing =
          levelProgressRepository
              .findByQuestProgressIdAndLevelId(questProgress.getId(), nextLevel.getId())
              .orElseThrow(() -> alreadyCreatedConcurrently);
      return buildLevelProgressResponse(existing);
    }
  }

  @Override
  public LevelProgressResponse completeLevel(Long levelProgressId) {
    LevelProgress levelProgress = validateLevelProgressExist(levelProgressId);
    validateLevelActive(levelProgress);

    levelProgress.setStatus(LevelProgressStatus.COMPLETED);
    levelProgress.setCompletedAt(clock.instant());

    LevelProgress saved = levelProgressRepository.save(levelProgress);

    return buildLevelProgressResponse(saved);
  }

  @Override
  public LevelProgressResponse autoTransitionLevel(Long levelProgressId) {
    LevelProgress levelProgress = validateLevelProgressExist(levelProgressId);
    validateLevelActive(levelProgress);

    Instant now = clock.instant();
    Instant autoTransitionAt = levelProgress.getAutoTransitionAt();

    if (autoTransitionAt == null) {
      return buildLevelProgressResponse(levelProgress);
    }

    if (!now.isBefore(autoTransitionAt)) {
      levelProgress.setStatus(LevelProgressStatus.AUTO_TRANSITIONED);
      levelProgress.setCompletedAt(now);

      LevelProgress saved = levelProgressRepository.save(levelProgress);
      return buildLevelProgressResponse(saved);
    }

    return buildLevelProgressResponse(levelProgress);
  }

  // ────── VALIDATIONS ───────────────────────────────────────────────────────────
  private Level validateLevelExist(Long levelId) {
    return levelRepository
        .findById(levelId)
        .orElseThrow(() -> new IllegalArgumentException("Уровень не найден: " + levelId));
  }

  private LevelProgress validateLevelProgressExist(Long levelProgressId) {
    // TODO тут тоже убрать проблему N+1
    return levelProgressRepository
        .findById(levelProgressId)
        .orElseThrow(
            () -> new IllegalArgumentException("LevelProgress не найден: " + levelProgressId));
  }

  private void validateLevelActive(LevelProgress levelProgress) {
    if (levelProgress.getStatus() != LevelProgressStatus.ACTIVE) {
      throw new ForbiddenOperationException(
          "Завершить можно только ACTIVE уровень. Текущий статус: " + levelProgress.getStatus());
    }
  }

  private static void validateLevelBelongsToQuest(Level level, Long questId) {
    if (!level.getQuest().getId().equals(questId)) {
      throw new ForbiddenOperationException("Уровень не принадлежит QuestProgress");
    }
  }

  // ────── BUILDERS ───────────────────────────────────────────────────────────
  private LevelProgressResponse buildLevelProgressResponse(LevelProgress levelProgress) {
    return LevelProgressResponse.builder()
        .id(levelProgress.getId())
        .levelId(levelProgress.getLevel().getId())
        .levelTitle(levelProgress.getLevel().getTitle())
        .status(levelProgress.getStatus())
        .openedAt(levelProgress.getOpenedAt())
        .completedAt(levelProgress.getCompletedAt())
        .autoTransitionAt(levelProgress.getAutoTransitionAt())
        .build();
  }
}
