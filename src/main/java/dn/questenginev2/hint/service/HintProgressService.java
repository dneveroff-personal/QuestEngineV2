package dn.questenginev2.hint.service;

import dn.questenginev2.hint.dto.HintProgressResponse;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface HintProgressService {

  /**
   * Подсказки, уже показанные команде на её текущем активном уровне (auto-reveal, ADR-0020).
   * Если у команды нет активного уровня — пустой список, а не ошибка (это валидное состояние,
   * в отличие от попытки ввести код, см. CodeSubmissionServiceImpl).
   */
  List<HintProgressResponse> getShownHints(Long questId, Long teamId, Authentication auth);
}
