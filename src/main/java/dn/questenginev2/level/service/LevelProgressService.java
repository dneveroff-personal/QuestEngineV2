package dn.questenginev2.level.service;

import dn.questenginev2.level.dto.LevelProgressResponse;
import dn.questenginev2.quest.entity.QuestProgress;

public interface LevelProgressService {

  // Создаёт первый LevelProgress для команды, вошедшей в Quest
  LevelProgressResponse createFirstLevelProgress(QuestProgress questProgress);

  LevelProgressResponse createNextLevelProgress(
      QuestProgress questProgress, Integer nextLevelOrderIndex);

  LevelProgressResponse completeLevel(Long levelProgressId);

  // Проверяет и применяет автопереход, если время вышло
  LevelProgressResponse autoTransitionLevel(Long levelProgressId);
}
