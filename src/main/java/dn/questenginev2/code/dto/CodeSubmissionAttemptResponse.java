package dn.questenginev2.code.dto;

import dn.questenginev2.code.entity.CodeSubmissionResult;
import dn.questenginev2.code.entity.CodeType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Одна попытка ввода кода (аудит). Используется для:
 * <ul>
 *   <li>команды — свои попытки на активном уровне;</li>
 *   <li>автора — полная статистика попыток всех команд (statistics-ranking.md).</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CodeSubmissionAttemptResponse {

  private Long id;
  private String rawValue;
  private CodeSubmissionResult result;
  private Instant submittedAt;

  private Long submittedById;
  private String submittedByUsername;

  private Long teamId;
  private String teamName;

  private Long levelId;
  private Integer levelOrderIndex;
  private Long levelProgressId;

  /** null при INCORRECT. */
  private Long matchedCodeId;
  private Integer matchedCodeIndex;
  private CodeType matchedCodeType;
}
