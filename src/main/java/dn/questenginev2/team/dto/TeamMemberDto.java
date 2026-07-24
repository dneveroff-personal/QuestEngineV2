package dn.questenginev2.team.dto;

import dn.questenginev2.team.entity.TeamRole;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamMemberDto {

  private Long id;
  private String name;
  private TeamRole role;
  private Instant joinedAt;
}
