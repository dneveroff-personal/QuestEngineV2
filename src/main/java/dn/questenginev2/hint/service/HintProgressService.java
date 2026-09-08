package dn.questenginev2.hint.service;

import dn.questenginev2.hint.dto.HintProgressResponse;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface HintProgressService {

  /**
   * Подсказки, видимые команде на её текущем активном уровне (ADR-0020, ADR-0021). Три состояния
   * на подсказку: не наступило время — не включается в ответ; REGULAR показана (auto-reveal) или
   * BONUS/PENALTY взята — полный набор полей с content; BONUS/PENALTY доступна, но не взята —
   * только hintId/orderIndex/type, без content и bonusPenaltySeconds.
   *
   * <p>Если у команды нет активного уровня — пустой список, а не ошибка (это валидное состояние,
   * в отличие от попытки ввести код, см. CodeSubmissionServiceImpl).
   */
  List<HintProgressResponse> getVisibleHints(Long questId, Long teamId, Authentication auth);

  /**
   * Явное взятие BONUS/PENALTY-подсказки командой (ADR-0021). Идемпотентно: повторный вызов для
   * уже взятой подсказки не создаёт дубликата и не повторяет эффект.
   */
  HintProgressResponse takeHint(Long questId, Long teamId, Long hintId, Authentication auth);
}
