package dn.questenginev2.team.dto;

import dn.questenginev2.team.entity.TeamRole;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class TeamMemberDto {

    private Long id;
    private String name;
    private TeamRole role;
    private Instant joinedAt;

}
