package dn.questenginev2.team.service;

import dn.questenginev2.common.exceptions.TeamAlreadyExistsException;
import dn.questenginev2.common.exceptions.UserAlreadyExistsException;
import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamResponse;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.entity.TeamRole;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserService userService;

    @Override
public TeamResponse createTeam(CreateTeamRequest request, Authentication auth) {
        String userName = auth.getName();
User currentUser = userService.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + userName));

        String teamName = request.getName();
        if (teamRepository.existsByName(teamName)) {
            throw new TeamAlreadyExistsException("Team with name " + teamName + " already exists");
}

        if (teamMemberRepository.existsByUser(currentUser)) {
            throw new UserAlreadyExistsException("User with name " + userName + " already member of team");
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
}
