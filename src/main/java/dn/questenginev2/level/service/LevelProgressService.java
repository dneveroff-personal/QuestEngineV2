package dn.questenginev2.level.service;

import dn.questenginev2.level.dto.LevelProgressResponse;
import dn.questenginev2.quest.entity.QuestProgress;

public interface LevelProgressService {

  // Создаёт первый LevelProgress для команды, вошедшей в Quest
  LevelProgressResponse createFirstLevelProgress(QuestProgress questProgress);

  LevelProgressResponse createNextLevelProgress(
      QuestProgress questProgress, Integer nextLevelOrderIndex);

  LevelProgressResponse completeLevel(Long levelProgressId);

  /**
   * Проверяет и применяет автопереход, если время вышло.
   *
   * @deprecated Небезопасен для планировщика: делает read-then-write и может гоняться с
   *     завершением уровня через {@code CodeSubmission} (см.
   *     docs/02-processes/concurrency-scenarios.md, Сценарий 5). Планировщик (Job 2, {@code
   *     dn.questenginev2.scheduling}) использует атомарный {@link
   *     dn.questenginev2.level.repository.LevelProgressRepository#tryAutoTransition} напрямую,
   *     не этот метод. Оставлен для обратной совместимости/ручного использования.
   */
  @Deprecated
  LevelProgressResponse autoTransitionLevel(Long levelProgressId);
}
