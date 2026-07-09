package dn.questenginev2.team.service;

import dn.questenginev2.team.dto.*;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface TeamService {

    TeamResponse createTeam(CreateTeamRequest request, Authentication auth);

    Boolean createJoinRequest(Authentication auth, Long teamId, String username);

    List<TeamJoinResponse> getJoinRequests(Authentication auth);

    Boolean approveRequest(Long requestId, Authentication auth);

    Boolean rejectRequest(Long requestId, Authentication auth);

    TeamResponse getMyTeam(Authentication auth);

    List<TeamMemberDto> getTeamMembers(Long teamId);

    Boolean leaveTeam(Authentication auth);

    Boolean transferCaptain(Long userId, Authentication auth);

    TeamResponse getTeamById(Long teamId);
}
