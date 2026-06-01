package dn.questenginev2.team.service;

import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamJoinResponse;
import dn.questenginev2.team.dto.TeamResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface TeamService {

    TeamResponse createTeam(CreateTeamRequest request, Authentication auth);

    Boolean createJoinRequest(Authentication auth, Long teamId);

    List<TeamJoinResponse> getJoinRequests(Authentication auth);

    Boolean approveRequest(Long requestId, Authentication auth);
}
