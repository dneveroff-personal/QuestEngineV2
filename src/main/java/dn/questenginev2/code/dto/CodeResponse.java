package dn.questenginev2.code.dto;

import dn.questenginev2.code.entity.CodeType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CodeResponse {

  private Long id;
  private Long levelId;
  private String value;
  private CodeType type;
  private Integer points;
  private Integer groupIndex;
  private Instant createdAt;
}
