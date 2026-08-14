package dn.questenginev2.level.dto;

import dn.questenginev2.level.entity.LevelProgressStatus;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LevelProgressResponse {

  Long id;
  Long levelId;
  String levelTitle;
  LevelProgressStatus status;
  Instant openedAt;
  Instant completedAt;
  Instant autoTransitionAt;
}
