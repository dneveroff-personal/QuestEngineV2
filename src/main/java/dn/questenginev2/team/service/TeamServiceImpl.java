package dn.questenginev2.team.service;

import dn.questenginev2.common.dto.PageResponse;
import dn.questenginev2.common.exceptions.*;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestRegistration;
import dn.questenginev2.quest.repository.QuestRegistrationRepository;
import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamFilterRequest;
import dn.questenginev2.team.dto.TeamJoinResponse;
import dn.questenginev2.team.dto.TeamMemberDto;
import dn.questenginev2.team.dto.TeamQuestItemResponse;
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
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import java.time.Instant;
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
  private final QuestRegistrationRepository questRegistrationRepository;

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
  public Boolean createJoinRequest(Authentication auth, Long teamId, String username) {
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
    return buildTeamResponse(membership.getTeam());
  }

  @Override
  public List<TeamMemberDto> getTeamMembers(Long teamId) {
    Team team = getTeam(teamId);
    return teamMemberstoDto(teamMemberRepository.findAllByTeam(team));
  }

  @Override
  @Transactional
  public Boolean leaveTeam(Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    TeamMember membership =
        teamMemberRepository
            .findByUser(currentUser)
            .orElseThrow(() -> new TeamNotFoundException("Команда не найдена"));
    if (membership.getRole() == TeamRole.CAPTAIN) {
      throw new ForbiddenOperationException("Капитан не может покинуть команду без передачи роли");
    }
    teamMemberRepository.delete(membership);
    return true;
  }

  @Override
  @Transactional
  public Boolean transferCaptain(Long userId, Authentication auth) {
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
            .findByUserAndTeam(newCaptain, team)
            .orElseThrow(() -> new RequestNotFoundException("User is not a team member"));

    currentMembership.setRole(TeamRole.MEMBER);
    target.setRole(TeamRole.CAPTAIN);
    team.setCaptain(newCaptain);
    teamMemberRepository.save(currentMembership);
    teamMemberRepository.save(target);
    teamRepository.save(team);
    return true;
  }

  @Override
  @Transactional
  public TeamResponse renameTeam(Long teamId, CreateTeamRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Team team = getTeam(teamId);
    validateCaptain(team, currentUser);

    String newName = request.name();
    if (!team.getName().equals(newName)) {
      validateTeamNameUnique(newName);
      team.setName(newName);
      team = teamRepository.save(team);
    }
    return buildTeamResponse(team);
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

  @Override
  public List<TeamQuestItemResponse> getTeamQuests(Long teamId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Team team = getTeam(teamId);
    validateTeamMemberOrAdmin(team, currentUser);

    return questRegistrationRepository.findByTeamIdOrderByCreatedAtDesc(teamId).stream()
        .map(this::toTeamQuestItem)
        .collect(Collectors.toList());
  }

  @Override
  public List<TeamQuestItemResponse> getMyTeamQuests(Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    TeamMember membership =
        teamMemberRepository
            .findByUser(currentUser)
            .orElseThrow(() -> new TeamNotFoundException("Команда не найдена"));
    return getTeamQuests(membership.getTeam().getId(), auth);
  }

  private void validateTeamMemberOrAdmin(Team team, User user) {
    if (user.getRole() == UserRole.ADMIN) {
      return;
    }
    boolean member = teamMemberRepository.findByUserAndTeam(user, team).isPresent();
    if (!member) {
      throw new ForbiddenOperationException("Только участник команды или ADMIN");
    }
  }

  private TeamQuestItemResponse toTeamQuestItem(QuestRegistration reg) {
    Quest q = reg.getQuest();
    return new TeamQuestItemResponse(
        reg.getId(),
        reg.getStatus(),
        reg.getCreatedAt(),
        q.getId(),
        q.getTitle(),
        q.getDescription(),
        q.getType(),
        q.getStatus(),
        q.getStartTime(),
        q.getFinishTime());
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
