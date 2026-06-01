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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
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
        String userName = auth.getName();
        User currentUser = userService.findByUsername(userName)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userName));

        String teamName = request.getName();
        if (teamRepository.existsByName(teamName)) {
            throw new TeamAlreadyExistsException("Team with name " + teamName + " already exists");
        }

        if (teamMemberRepository.existsByUser(currentUser)) {
            throw new UserAlreadyInTeamException("User with name " + userName + " already member of team");
        }

        Team team = Team.builder()
                .name(teamName)
                .captain(currentUser)
                .createdAt(Instant.now())
                .build();

        Team savedTeam = teamRepository.save(team);

        TeamMember teamMember = TeamMember.builder()
                .team(savedTeam)
                .user(currentUser)
                .role(TeamRole.CAPTAIN)
                .joinedAt(Instant.now())
                .build();
        teamMemberRepository.save(teamMember);

        return buildTeamResponse(savedTeam);
    }

    private TeamResponse buildTeamResponse(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getCaptain().getPublicName(),
                team.getCreatedAt()
        );
    }

    @Override
    public Boolean createJoinRequest(Authentication auth, Long teamId) {
        String userName = auth.getName();
        User currentUser = userService.findByUsername(userName)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userName));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Команда не найдена"));

        if (teamMemberRepository.existsByUser(currentUser)) {
            throw new UserAlreadyInTeamException("User already member of a team");
        }

        if (joinRequestRepository.existsByTeamAndUser(team, currentUser)) {
            throw new RequestAlreadyExistsException("Request already exists");
        }

        TeamJoinRequest request = TeamJoinRequest.builder()
                .user(currentUser)
                .team(team)
                .status(RequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        joinRequestRepository.save(request);

        return true;
    }

    @Override
    public List<TeamJoinResponse> getJoinRequests(Authentication auth) {
        String userName = auth.getName();
        User currentUser = userService.findByUsername(userName)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userName));

        Team team = teamRepository.findByCaptain(currentUser)
                .orElseThrow(() -> new TeamNotFoundException("Пользователь не является капитаном команды"));

        List<TeamJoinRequest> joinRequests = joinRequestRepository.findByTeam(team);

        return joinRequests.stream()
                .map(this::buildTeamJoinResponse)
                .collect(Collectors.toList());
    }

    private TeamJoinResponse buildTeamJoinResponse(TeamJoinRequest request) {
        return new TeamJoinResponse(
                request.getId(),
                request.getUser().getPublicName(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }

    @Override
    public Boolean approveRequest(Long requestId, Authentication auth) {
        String userName = auth.getName();
        User currentUser = userService.findByUsername(userName)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userName));

        TeamJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found"));

        if (!request.getTeam().getCaptain().equals(currentUser)) {
            throw new RuntimeException("Only captain can approve");
        }

        request.setStatus(RequestStatus.APPROVED);
        joinRequestRepository.save(request);

        // Создаём TeamMember для пользователя
        TeamMember member = TeamMember.builder()
                .team(request.getTeam())
                .user(request.getUser())
                .role(TeamRole.MEMBER)
                .joinedAt(Instant.now())
                .build();

        teamMemberRepository.save(member);
        joinRequestRepository.deleteById(requestId);
        return true;
    }

    @Override
    public Boolean rejectRequest(Long requestId, Authentication auth) {
        String userName = auth.getName();
        User currentUser = userService.findByUsername(userName)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userName));

        TeamJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found"));

        if (!request.getTeam().getCaptain().equals(currentUser)) {
            throw new RuntimeException("Only captain can reject");
        }

        request.setStatus(RequestStatus.REJECTED);
        joinRequestRepository.save(request);
        joinRequestRepository.deleteById(requestId);
        return true;
    }

}
