package dn.questenginev2.team.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamResponse {

  private Long id;
  private String name;
  private String captainName;
  private Instant createdAt;
  private List<TeamMemberDto> members;
}
