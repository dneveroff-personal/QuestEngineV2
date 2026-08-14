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
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@AllArgsConstructor
public class LevelProgressServiceImpl implements LevelProgressService {

  private final LevelProgressRepository levelProgressRepository;
  private final LevelRepository levelRepository;
  private final QuestProgressRepository questProgressRepository;

  // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
  @Override
  public LevelProgressResponse createFirstLevelProgress(
      Long questProgressId, Long levelId, Instant questStartTime) {

    QuestProgress questProgress =
        questProgressRepository
            .findById(questProgressId)
            .orElseThrow(
                () -> new IllegalArgumentException("QuestProgress не найден: " + questProgressId));

    if (levelProgressRepository.existsByQuestProgressIdAndLevelId(questProgressId, levelId)) {
      throw new ForbiddenOperationException("Уровень уже был сыгран этой командой");
    }

    Level level = validateLevelExist(levelId);

    Instant openedAt = Instant.now();
    Instant autoTransitionAt = questStartTime.plusSeconds(level.getTimeoutSeconds());

    LevelProgressStatus status;
    Instant completedAt = null;

    if (openedAt.isAfter(autoTransitionAt)) {
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
    levelProgress.setCompletedAt(Instant.now());
    LevelProgress saved = levelProgressRepository.save(levelProgress);

    return buildLevelProgressResponse(saved);
  }

  @Override
  public LevelProgressResponse autoTransitionLevel(Long levelProgressId) {
    LevelProgress levelProgress = validateLevelProgressExist(levelProgressId);
    validateLevelActive(levelProgress);

    Instant now = Instant.now();

    if (now.isAfter(levelProgress.getAutoTransitionAt())) {
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
