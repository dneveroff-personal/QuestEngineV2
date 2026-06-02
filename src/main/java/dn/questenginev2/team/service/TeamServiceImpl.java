package dn.questenginev2.team.service;

import dn.questenginev2.common.exceptions.*;
import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamJoinResponse;
import dn.questenginev2.team.dto.TeamResponse;
import dn.questenginev2.team.entity.*;
import dn.questenginev2.team.repository.TeamJoinRequestRepository;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamJoinRequestRepository joinRequestRepository;
    private final UserService userService;

    @Override
    public TeamResponse createTeam(CreateTeamRequest request, Authentication auth) {
        User currentUser = getCurrentUser(auth);

        String teamName = request.getName();
        validateTeamNameUnique(teamName);
        validateUserNotInTeam(currentUser);

        Team team = buildTeam(teamName, currentUser);
        Team savedTeam = teamRepository.save(team);

        TeamMember teamMember = buildTeamMember(savedTeam, currentUser);
        teamMemberRepository.save(teamMember);

        return buildTeamResponse(savedTeam);
    }

    @Override
    public Boolean createJoinRequest(Authentication auth, Long teamId, String username) {
        User currentUser = getCurrentUser(auth);
        Team team = getTeam(teamId);

        JoinRequestType requestType = JoinRequestType.of(username);

        if (requestType == JoinRequestType.CAPTAIN_INVITE) {
            validateCaptainForInvite(team, currentUser);
        }

        User targetUser = requestType.resolveUser(userService, username, currentUser);

        validateNoDuplicateRequest(team, targetUser, requestType);
        validateRequest(requestType, team, currentUser, targetUser);

        TeamJoinRequest request = new TeamJoinRequest(team, targetUser, requestType);
        joinRequestRepository.save(request);
        return true;
    }

    @Override
    public List<TeamJoinResponse> getJoinRequests(Authentication auth) {
        User currentUser = getCurrentUser(auth);

        return teamRepository.findByCaptain(currentUser)
                .map(this::getCaptainJoinRequests)
                .orElseGet(() -> getUserInvites(currentUser));
    }

    @Override
    public Boolean approveRequest(Long requestId, Authentication auth) {
        User currentUser = getCurrentUser(auth);
        TeamJoinRequest request = getJoinRequest(requestId);

        validateApprovalPermission(request, currentUser);

        TeamMember member = buildTeamMember(request.getTeam(), request.getUser());
        teamMemberRepository.save(member);
        joinRequestRepository.delete(request);

        return true;
    }

    @Override
    public Boolean rejectRequest(Long requestId, Authentication auth) {
        User currentUser = getCurrentUser(auth);
        TeamJoinRequest request = getJoinRequest(requestId);

        validateRejectionPermission(request, currentUser);

        joinRequestRepository.delete(request);
        return true;
    }

    private User getCurrentUser(Authentication auth) {
        String userName = auth.getName();
        return userService.findByUsername(userName)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userName));
    }

    private Team getTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Команда не найдена"));
    }

    private TeamJoinRequest getJoinRequest(Long requestId) {
        return joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found"));
    }

    private void validateTeamNameUnique(String teamName) {
        if (teamRepository.existsByName(teamName)) {
            throw new TeamAlreadyExistsException("Team with name " + teamName + " already exists");
        }
    }

    private void validateUserNotInTeam(User user) {
        if (teamMemberRepository.existsByUser(user)) {
            throw new UserAlreadyInTeamException("User already member of a team");
        }
    }

    private void validateNoDuplicateRequest(Team team, User user, JoinRequestType requestType) {
        if (joinRequestRepository.existsByTeamAndUserAndType(team, user, requestType)) {
            throw new RequestAlreadyExistsException("Request already exists");
        }
    }

    private void validateRequest(JoinRequestType requestType, Team team, User currentUser, User targetUser) {
        if (requestType == JoinRequestType.JOIN_REQUEST) {
            validateJoinRequest(targetUser);
        } else {
            validateInvite(team, currentUser, targetUser);
        }
    }

    private void validateJoinRequest(User user) {
        if (teamMemberRepository.existsByUser(user)) {
            throw new UserAlreadyInTeamException("User already member of a team");
        }
    }

    private void validateInvite(Team team, User captain, User invitedUser) {
        validateCaptainForInvite(team, captain);

        if (teamMemberRepository.existsByUser(invitedUser)) {
            throw new UserAlreadyInTeamException("Пользователь уже состоит в команде");
        }
    }

    private void validateCaptainForInvite(Team team, User currentUser) {
        if (!team.getCaptain().equals(currentUser)) {
            throw new AccessDeniedException("Only captain can invite users");
        }
    }

    private void validateCaptainForApproval(Team team, User currentUser) {
        if (!team.getCaptain().equals(currentUser)) {
            throw new AccessDeniedException("Only captain can approve");
        }
    }

    private void validateCaptainForRejection(Team team, User currentUser) {
        if (!team.getCaptain().equals(currentUser)) {
            throw new AccessDeniedException("Only captain can reject");
        }
    }

    private void validateApprovalPermission(TeamJoinRequest request, User currentUser) {
        if (request.getType() == JoinRequestType.JOIN_REQUEST) {
            validateCaptainForApproval(request.getTeam(), currentUser);
        } else if (!request.getUser().equals(currentUser)) {
            throw new AccessDeniedException("Only invited user can accept");
        }
    }

    private void validateRejectionPermission(TeamJoinRequest request, User currentUser) {
        if (request.getType() == JoinRequestType.JOIN_REQUEST) {
            validateCaptainForRejection(request.getTeam(), currentUser);
        } else if (!request.getUser().equals(currentUser)) {
            throw new AccessDeniedException("Only invited user can reject");
        }
    }

    private Team buildTeam(String name, User captain) {
        return Team.builder()
                .name(name)
                .captain(captain)
                .createdAt(Instant.now())
                .build();
    }

    private TeamMember buildTeamMember(Team team, User user) {
        return TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamRole.CAPTAIN)
                .joinedAt(Instant.now())
                .build();
    }

    private TeamResponse buildTeamResponse(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getCaptain().getPublicName(),
                team.getCreatedAt()
        );
    }

    private List<TeamJoinResponse> getCaptainJoinRequests(Team team) {
        return joinRequestRepository.findByTeamAndType(team, JoinRequestType.JOIN_REQUEST).stream()
                .map(this::buildTeamJoinResponse)
                .collect(Collectors.toList());
    }

    private List<TeamJoinResponse> getUserInvites(User user) {
        return joinRequestRepository.findByUserAndType(user, JoinRequestType.CAPTAIN_INVITE).stream()
                .map(this::buildTeamJoinResponse)
                .collect(Collectors.toList());
    }

    private TeamJoinResponse buildTeamJoinResponse(TeamJoinRequest request) {
        return new TeamJoinResponse(
                request.getId(),
                request.getUser().getPublicName(),
                request.getType(),
                request.getCreatedAt()
        );
    }
}
