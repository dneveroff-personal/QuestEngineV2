package dn.questenginev2.team.repository;

import dn.questenginev2.team.entity.JoinRequestType;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamJoinRequest;
import dn.questenginev2.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, Long> {

    boolean existsByTeamAndUser(Team team, User user);
    List<TeamJoinRequest> findByTeam(Team team);
    List<TeamJoinRequest> findByTeamAndType(Team team, JoinRequestType type);
    Optional<TeamJoinRequest> findByTeamAndUserAndType(Team team, User user, JoinRequestType type);
    List<TeamJoinRequest> findByUserAndType(User user, JoinRequestType type);
    boolean existsByTeamAndUserAndType(Team team, User user, JoinRequestType type);

}