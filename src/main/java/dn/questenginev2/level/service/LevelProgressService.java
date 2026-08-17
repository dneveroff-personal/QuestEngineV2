package dn.questenginev2.level.service;

import dn.questenginev2.level.dto.LevelProgressResponse;

public interface LevelProgressService {

  // Создаёт первый LevelProgress для команды, вошедшей в Quest
  LevelProgressResponse createFirstLevelProgress(Long questProgressId);

  LevelProgressResponse completeLevel(Long levelProgressId);

  // Проверяет и применяет автопереход, если время вышло
  LevelProgressResponse autoTransitionLevel(Long levelProgressId);
}
