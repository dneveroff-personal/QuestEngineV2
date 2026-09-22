package dn.questenginev2.level.service;

import dn.questenginev2.common.exceptions.ConflictException;
import dn.questenginev2.common.exceptions.ResourceNotFoundException;
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

  @Override
  public LevelProgressResponse createFirstLevelProgress(QuestProgress questProgress) {
    if (questProgress == null || questProgress.getQuest() == null) {
      throw new ResourceNotFoundException("QuestProgress не найден");
    }

    Long questId = questProgress.getQuest().getId();

    Level level =
        levelRepository
            .findByQuestIdAndOrderIndex(questId, 1)
            .orElseThrow(() -> new ResourceNotFoundException("Уровень не найден"));

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

  private LevelProgress validateLevelProgressExist(Long levelProgressId) {
    return levelProgressRepository
        .findById(levelProgressId)
        .orElseThrow(
            () -> new ResourceNotFoundException("LevelProgress не найден: " + levelProgressId));
  }

  private void validateLevelActive(LevelProgress levelProgress) {
    if (levelProgress.getStatus() != LevelProgressStatus.ACTIVE) {
      throw new ConflictException(
          "Завершить можно только ACTIVE уровень. Текущий статус: " + levelProgress.getStatus());
    }
  }

  private static void validateLevelBelongsToQuest(Level level, Long questId) {
    if (!level.getQuest().getId().equals(questId)) {
      throw new ConflictException("Уровень не принадлежит QuestProgress");
    }
  }

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
