package dn.questenginev2.team.dto;

import dn.questenginev2.team.entity.JoinRequestType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamJoinResponse {

  private Long requestId;
  private String userName;
  private JoinRequestType type;
  private Instant createdAt;
}
