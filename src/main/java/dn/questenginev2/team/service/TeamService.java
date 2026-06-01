package dn.questenginev2.team.service;

import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamResponse;
import org.springframework.security.core.Authentication;

public interface TeamService {

    TeamResponse createTeam(CreateTeamRequest request, Authentication authentication);

}
