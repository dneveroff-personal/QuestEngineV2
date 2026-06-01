package dn.questenginev2.team.repository;

import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamJoinRequest;
import dn.questenginev2.team.entity.RequestStatus;
import dn.questenginev2.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, Long> {

    boolean existsByTeamAndUser(Team team, User user);
    List<TeamJoinRequest> findByTeamAndStatus(Team team, RequestStatus status);
    List<TeamJoinRequest> findByTeam(Team team);

}