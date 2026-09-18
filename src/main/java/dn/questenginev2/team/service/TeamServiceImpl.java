package dn.questenginev2.team.service;

import dn.questenginev2.common.dto.PageResponse;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.RequestAlreadyExistsException;
import dn.questenginev2.common.exceptions.RequestNotFoundException;
import dn.questenginev2.common.exceptions.TeamAlreadyExistsException;
import dn.questenginev2.common.exceptions.TeamNotFoundException;
import dn.questenginev2.common.exceptions.UserAlreadyInTeamException;
import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamFilterRequest;
import dn.questenginev2.team.dto.TeamJoinResponse;
import dn.questenginev2.team.dto.TeamMemberDto;
import dn.questenginev2.team.dto.TeamResponse;
import dn.questenginev2.team.entity.JoinRequestType;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamJoinRequest;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.entity.TeamRole;
import dn.questenginev2.team.repository.TeamJoinRequestRepository;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.team.specification.TeamSpecification;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TeamServiceImpl implements TeamService {

  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TeamJoinRequestRepository joinRequestRepository;
  private final UserService userService;

  @Override
  @Transactional
  public TeamResponse createTeam(CreateTeamRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    String teamName = request.name();

    validateTeamNameUnique(teamName);
    validateUserNotInTeam(currentUser);

    Team team = buildTeam(teamName, currentUser);
    Team savedTeam = teamRepository.save(team);

    TeamMember teamMember = buildTeamMember(savedTeam, currentUser, TeamRole.CAPTAIN);
    teamMemberRepository.save(teamMember);

    return buildTeamResponse(savedTeam);
  }

  @Override
  @Transactional
  public Boolean createJoinRequest(Long teamId, String username, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Team team = getTeam(teamId);

    JoinRequestType requestType;
    User targetUser;

    if (username == null || username.isBlank()) {
      requestType = JoinRequestType.JOIN_REQUEST;
      targetUser = currentUser;
    } else {
      requestType = JoinRequestType.CAPTAIN_INVITE;
      targetUser =
          userService
              .findByUsername(username)
              .orElseThrow(() -> new RequestNotFoundException("User not found: " + username));
    }

    validateRequest(requestType, team, currentUser, targetUser);
    validateNoDuplicateRequest(team, targetUser, requestType);

    TeamJoinRequest request = new TeamJoinRequest(team, targetUser, requestType);
    joinRequestRepository.save(request);
    return true;
  }

  @Override
  public List<TeamJoinResponse> getJoinRequests(Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);

    return teamRepository
        .findByCaptain(currentUser)
        .map(this::getCaptainJoinRequests)
        .orElseGet(() -> getUserInvites(currentUser));
  }

  @Override
  @Transactional
  public Boolean approveRequest(Long requestId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    TeamJoinRequest request = getJoinRequest(requestId);

    validateCaptain(request.getTeam(), currentUser);
    validateUserNotInTeam(request.getUser());

    TeamMember member = buildTeamMember(request.getTeam(), request.getUser(), TeamRole.MEMBER);
    teamMemberRepository.save(member);
    joinRequestRepository.delete(request);
    return true;
  }

  @Override
  @Transactional
  public Boolean rejectRequest(Long requestId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    TeamJoinRequest request = getJoinRequest(requestId);
    validateCaptain(request.getTeam(), currentUser);
    joinRequestRepository.delete(request);
    return true;
  }

  @Override
  public TeamResponse getMyTeam(Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    TeamMember membership =
        teamMemberRepository
            .findByUser(currentUser)
            .orElseThrow(() -> new TeamNotFoundException("Команда не найдена"));
    Team team = membership.getTeam();
    return buildTeamResponse(team);
  }

  @Override
  public List<TeamMemberDto> getTeamMembers(Long teamId) {
    Team team = getTeam(teamId);
    return teamMemberstoDto(teamMemberRepository.findAllByTeam(team));
  }

  @Override
  @Transactional
  public void leaveTeam(Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    TeamMember membership =
        teamMemberRepository
            .findByUser(currentUser)
            .orElseThrow(() -> new TeamNotFoundException("Команда не найдена"));
    if (membership.getRole() == TeamRole.CAPTAIN) {
      throw new ForbiddenOperationException("Капитан не может покинуть команду без передачи роли");
    }
    teamMemberRepository.delete(membership);
  }

  @Override
  @Transactional
  public void transferCaptain(Long userId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    TeamMember currentMembership =
        teamMemberRepository
            .findByUser(currentUser)
            .orElseThrow(() -> new TeamNotFoundException("Команда не найдена"));
    validateCaptain(currentMembership, "Только капитан может передать капитанство");

    Team team = currentMembership.getTeam();
    User newCaptain = userService.getUser(userId);
    TeamMember target =
        teamMemberRepository
            .findByTeamAndUser(team, newCaptain)
            .orElseThrow(() -> new RequestNotFoundException("User is not a team member"));

    currentMembership.setRole(TeamRole.MEMBER);
    target.setRole(TeamRole.CAPTAIN);
    team.setCaptain(newCaptain);
    teamMemberRepository.save(currentMembership);
    teamMemberRepository.save(target);
    teamRepository.save(team);
  }

  @Override
  public TeamResponse getTeamById(Long teamId) {
    return buildTeamResponse(getTeam(teamId));
  }

  @Override
  public PageResponse<TeamResponse> searchTeams(TeamFilterRequest filter, Pageable pageable) {
    var spec =
        TeamSpecification.hasName(filter.name())
            .and(TeamSpecification.createdAtAfter(filter.createdAtAfter()))
            .and(TeamSpecification.createdAtBefore(filter.createdAtBefore()));
    return PageResponse.from(teamRepository.findAll(spec, pageable).map(this::buildTeamResponse));
  }

  private List<TeamMemberDto> teamMemberstoDto(List<TeamMember> teamMembers) {
    return teamMembers.stream()
        .map(
            m ->
                new TeamMemberDto(
                    m.getId(),
                    m.getUser().getId(),
                    m.getUser().getUsername(),
                    displayName(m.getUser()),
                    m.getRole(),
                    m.getJoinedAt()))
        .collect(Collectors.toList());
  }

  private static String displayName(User user) {
    String publicName = user.getPublicName();
    if (publicName != null && !publicName.isBlank()) {
      return publicName;
    }
    return user.getUsername();
  }

  private Team getTeam(Long teamId) {
    return teamRepository
        .findById(teamId)
        .orElseThrow(() -> new TeamNotFoundException("Команда не найдена"));
  }

  private TeamJoinRequest getJoinRequest(Long requestId) {
    return joinRequestRepository
        .findById(requestId)
        .orElseThrow(() -> new RequestNotFoundException("Request not found"));
  }

  private void validateTeamNameUnique(String teamName) {
    if (teamRepository.existsByName(teamName)) {
      throw new TeamAlreadyExistsException("Team with name " + teamName + " already exists");
    }
  }

  private void validateUserNotInTeam(User user) {
    if (teamMemberRepository.existsByUser(user)) {
      throw new UserAlreadyInTeamException("Пользователь уже состоит в команде");
    }
  }

  private void validateNoDuplicateRequest(Team team, User user, JoinRequestType requestType) {
    if (joinRequestRepository.existsByTeamAndUserAndType(team, user, requestType)) {
      throw new RequestAlreadyExistsException("Request already exists");
    }
  }

  private void validateRequest(
      JoinRequestType requestType, Team team, User currentUser, User targetUser) {
    if (requestType == JoinRequestType.JOIN_REQUEST) {
      validateJoinRequest(targetUser);
    } else {
      validateInvite(team, currentUser, targetUser);
    }
  }

  private void validateJoinRequest(User user) {
    if (teamMemberRepository.existsByUser(user)) {
      throw new UserAlreadyInTeamException("Пользователь уже состоит в команде");
    }
  }

  private void validateInvite(Team team, User captain, User invitedUser) {
    validateCaptain(team, captain);
    if (teamMemberRepository.existsByUser(invitedUser)) {
      throw new UserAlreadyInTeamException("Пользователь уже состоит в команде");
    }
  }

  private void validateCaptain(TeamMember teamMember, String message) {
    if (!teamMember.getRole().equals(TeamRole.CAPTAIN)) {
      throw new ForbiddenOperationException(message);
    }
  }

  private void validateCaptain(Team team, User user) {
    if (!team.getCaptain().getId().equals(user.getId())) {
      throw new ForbiddenOperationException("Только капитан может выполнить это действие");
    }
  }

  private Team buildTeam(String name, User captain) {
    return Team.builder().name(name).captain(captain).createdAt(Instant.now()).build();
  }

  private TeamMember buildTeamMember(Team team, User user, TeamRole role) {
    return TeamMember.builder().team(team).user(user).role(role).joinedAt(Instant.now()).build();
  }

  private TeamResponse buildTeamResponse(Team team) {
    List<TeamMember> teamMembers = teamMemberRepository.findAllByTeam(team);
    var captain = team.getCaptain();

    return new TeamResponse(
        team.getId(),
        team.getName(),
        captain.getUsername(),
        displayName(captain),
        team.getCreatedAt(),
        teamMemberstoDto(teamMembers));
  }

  private TeamJoinResponse buildTeamJoinResponse(TeamJoinRequest request) {
    return new TeamJoinResponse(
        request.getId(),
        request.getUser().getPublicName(),
        request.getType(),
        request.getCreatedAt());
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
}
