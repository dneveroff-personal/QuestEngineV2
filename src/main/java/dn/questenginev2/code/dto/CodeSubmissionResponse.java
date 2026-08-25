package dn.questenginev2.code.dto;

import dn.questenginev2.code.entity.CodeSubmissionResult;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CodeSubmissionResponse {

  private CodeSubmissionResult result;

  // Осталось решить кодов до завершения уровня (null, если на уровне нет обязательных кодов).
  private Integer remainingMainCodes;

  // true, если этой попыткой уровень был завершён (ровно один конкурентный запрос получает true
  // при одновременном достижении порога — см. ADR-0010, Сценарий 6).
  private boolean levelCompleted;

  // true, если завершение уровня было последним и QuestProgress перешёл в FINISHED (ADR-0009).
  private boolean questFinished;

  private Instant submittedAt;
}
