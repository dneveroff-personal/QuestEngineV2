package dn.questenginev2.code.service;

import dn.questenginev2.code.dto.CodeSubmissionAttemptResponse;
import dn.questenginev2.code.dto.CodeSubmissionResponse;
import dn.questenginev2.code.dto.SubmitCodeRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface CodeSubmissionService {

  // Ввод кода любым участником команды на активном уровне (01-domain/code-submission.md)
  CodeSubmissionResponse submitCode(
      Long questId, Long teamId, @Valid SubmitCodeRequest request, Authentication auth);

  /**
   * Попытки команды на её текущем ACTIVE LevelProgress.
   * Видимость: только участник этой команды (или ADMIN). Пустой список, если активного уровня нет.
   */
  List<CodeSubmissionAttemptResponse> listAttemptsForTeamActiveLevel(
      Long questId, Long teamId, Authentication auth);

  /**
   * Полная статистика попыток всех команд по квесту.
   * Видимость: автор квеста или ADMIN (statistics-ranking.md).
   */
  List<CodeSubmissionAttemptResponse> listAttemptsForQuestAuthor(
      Long questId, Authentication auth);
}
