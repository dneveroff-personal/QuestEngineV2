package dn.questenginev2.team.repository;

import dn.questenginev2.team.entity.JoinRequestType;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamJoinRequest;
import dn.questenginev2.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, Long> {

    List<TeamJoinRequest> findByTeamAndType(Team team, JoinRequestType type);
    List<TeamJoinRequest> findByUserAndType(User user, JoinRequestType type);
    boolean existsByTeamAndUserAndType(Team team, User user, JoinRequestType type);

}