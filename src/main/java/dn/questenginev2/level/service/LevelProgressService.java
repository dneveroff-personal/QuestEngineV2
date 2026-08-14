package dn.questenginev2.level.service;

import dn.questenginev2.level.dto.LevelProgressResponse;
import java.time.Instant;

public interface LevelProgressService {

  // Создаёт первый LevelProgress для команды, вошедшей в Quest
  LevelProgressResponse createFirstLevelProgress(
      Long questProgressId, Long levelId, Instant questStartTime);

  LevelProgressResponse completeLevel(Long levelProgressId);

  // Проверяет и применяет автопереход, если время вышло
  LevelProgressResponse autoTransitionLevel(Long levelProgressId);
}
