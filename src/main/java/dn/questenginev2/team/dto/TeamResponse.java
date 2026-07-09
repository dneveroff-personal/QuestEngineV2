package dn.questenginev2.team.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class TeamResponse {

    private Long id;
    private String name;
    private String captainName;
    private Instant createdAt;
    private List<TeamMemberDto> members;

}
