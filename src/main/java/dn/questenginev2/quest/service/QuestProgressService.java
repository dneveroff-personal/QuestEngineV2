package dn.questenginev2.quest.service;

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

  QuestProgressResponse completeLevel(Long levelProgressId, Authentication auth);
}
