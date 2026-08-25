package dn.questenginev2.quest.service;

import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.quest.dto.QuestProgressResponse;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface QuestProgressService {

  // Создаёт QuestProgress для APPROVED команды в RUNNING квесте
  QuestProgressResponse createProgress(Long questId, Long teamId);

  // Команда входит в квест (WAITING -> RUNNING)
  QuestProgressResponse enterQuest(Long questId, Authentication auth);

  // Получить прогресс конкретной команды
  QuestProgressResponse getProgress(Long questId, Long teamId);

  // Получить весь прогресс по квесту
  List<QuestProgressResponse> getAllByQuest(Long questId);

  // Завершить прогресс команды
  QuestProgressResponse finishProgress(Long questId, Long teamId, Authentication auth);

  // Установить DNF для команды
  QuestProgressResponse setDnf(Long questId, Long teamId, Authentication auth);

  // Ручной путь завершения уровня (author/team-triggered override), см. ADR-0009
  QuestProgressResponse completeLevel(Long levelProgressId, Authentication auth);

  /**
   * Открывает следующий уровень или переводит QuestProgress в FINISHED, если уровень был
   * последним (ADR-0009 — без разницы между способом завершения предыдущего уровня). Вызывается
   * ПОСЛЕ того, как completedLevelProgress уже атомарно переведён в COMPLETED другим путём
   * (CodeSubmission, в будущем — Job 2 планировщика) — сам не изменяет статус LevelProgress.
   */
  QuestProgressResponse advanceAfterLevelCompleted(LevelProgress completedLevelProgress);
}
