package dn.questenginev2.team.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MyTeamResponse {

    private Long id;
    private String name;
    private String captainName;
    private List<TeamMemberDto> members;

}
