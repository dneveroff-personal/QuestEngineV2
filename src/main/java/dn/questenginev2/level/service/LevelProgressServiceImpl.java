package dn.questenginev2.level.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.level.dto.LevelProgressResponse;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class LevelProgressServiceImpl implements LevelProgressService {

  private final LevelProgressRepository levelProgressRepository;
  private final LevelRepository levelRepository;
  private final QuestProgressRepository questProgressRepository;
  private final Clock clock;

  @Autowired
  public LevelProgressServiceImpl(
      LevelProgressRepository levelProgressRepository,
      LevelRepository levelRepository,
      QuestProgressRepository questProgressRepository) {
    this(levelProgressRepository, levelRepository, questProgressRepository, Clock.systemUTC());
  }

  public LevelProgressServiceImpl(
      LevelProgressRepository levelProgressRepository,
      LevelRepository levelRepository,
      QuestProgressRepository questProgressRepository,
      Clock clock) {
    this.levelProgressRepository = levelProgressRepository;
    this.levelRepository = levelRepository;
    this.questProgressRepository = questProgressRepository;
    this.clock = clock;
  }

  // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
  @Override
  public LevelProgressResponse createFirstLevelProgress(Long questProgressId) {

    // TODO исправить проблемы N+1 для подобных выборок findById
    QuestProgress questProgress =
        questProgressRepository
            .findById(questProgressId)
            .orElseThrow(
                () -> new IllegalArgumentException("QuestProgress не найден: " + questProgressId));

    Level level =
        levelRepository
            .findByQuestIdAndOrderIndex(questProgress.getQuest().getId(), 1)
            .orElseThrow(() -> new IllegalArgumentException("Уровень не найден"));

    validateLevelBelongsToQuest(level, questProgress);

    if (levelProgressRepository.existsByQuestProgressIdAndLevelId(questProgressId, level.getId())) {
      throw new ForbiddenOperationException("Уровень уже был сыгран этой командой");
    }

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

    LevelProgress saved = levelProgressRepository.save(levelProgress);
    return buildLevelProgressResponse(saved);
  }

  @Override
  public LevelProgressResponse createLevelProgress(Long questProgressId) {
    // TODO исправить проблемы N+1 для подобных выборок findById
    QuestProgress questProgress =
        questProgressRepository
            .findById(questProgressId)
            .orElseThrow(
                () -> new IllegalArgumentException("QuestProgress не найден: " + questProgressId));

    Level level =
        levelRepository
            .findByQuestIdAndOrderIndex(questProgress.getQuest().getId(), 1)
            .orElseThrow(() -> new IllegalArgumentException("Уровень не найден"));

    validateLevelBelongsToQuest(level, questProgress);

    if (levelProgressRepository.existsByQuestProgressIdAndLevelId(questProgressId, level.getId())) {
      throw new ForbiddenOperationException("Уровень уже был сыгран этой командой");
    }

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

    LevelProgress saved = levelProgressRepository.save(levelProgress);
    return buildLevelProgressResponse(saved);
  }

  @Override
  public LevelProgressResponse completeLevel(Long levelProgressId) {
    LevelProgress levelProgress = validateLevelProgressExist(levelProgressId);
    validateLevelActive(levelProgress);

    levelProgress.setStatus(LevelProgressStatus.COMPLETED);
    levelProgress.setCompletedAt(clock.instant());
    LevelProgress saved = levelProgressRepository.save(levelProgress);

    //находил следующий Level;
    int nextLevelOrderIdx = saved.getLevel().getOrderIndex() + 1;
    QuestProgress questProgress = levelProgress.getQuestProgress();

    Level nextLevel =
        levelRepository
            .findByQuestIdAndOrderIndex(questProgress.getQuest().getId(), nextLevelOrderIdx)
            .orElseThrow(() -> new IllegalArgumentException("Следующий уровень не найден"));

    //если следующий существует — создавал следующий LevelProgress;
    if (nextLevel != null) {
      return createLevelProgress(questProgress.getId());
    }

    //если следующего нет — завершал QuestProgress.
  // TODO
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

  private static void validateLevelBelongsToQuest(Level level, QuestProgress questProgress) {
    if (!level.getQuest().getId().equals(questProgress.getQuest().getId())) {
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
