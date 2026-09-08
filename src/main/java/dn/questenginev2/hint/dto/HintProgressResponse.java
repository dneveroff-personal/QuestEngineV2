package dn.questenginev2.hint.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dn.questenginev2.hint.entity.HintType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HintProgressResponse {

  private Long hintId;
  private Integer orderIndex;
  private String content;
  private HintType type;
  private Integer bonusPenaltySeconds;
  private Instant shownAt;
}
