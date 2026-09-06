package dn.questenginev2.bonuspenalty.dto;

import dn.questenginev2.bonuspenalty.entity.TimeAdjustmentType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ManualTimeAdjustmentResponse {

  private Long id;
  private Long questProgressId;
  private TimeAdjustmentType type;
  private Integer seconds;
  private String reason;
  private Long createdByUserId;
  private Instant createdAt;
  private Instant revokedAt;
  private Long revokedByUserId;
}
