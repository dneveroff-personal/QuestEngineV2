package dn.questenginev2.team.repository;

import dn.questenginev2.team.entity.JoinRequestType;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamJoinRequest;
import dn.questenginev2.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, Long> {

  @EntityGraph(attributePaths = {"user"})
  List<TeamJoinRequest> findByTeamAndType(Team team, JoinRequestType type);

  @EntityGraph(attributePaths = {"user"})
  List<TeamJoinRequest> findByUserAndType(User user, JoinRequestType type);

  boolean existsByTeamAndUserAndType(Team team, User user, JoinRequestType type);
}
