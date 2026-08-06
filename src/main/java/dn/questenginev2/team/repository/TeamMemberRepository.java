package dn.questenginev2.team.repository;

import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeamMemberRepository
    extends JpaRepository<TeamMember, Long>, JpaSpecificationExecutor<TeamMember> {

  boolean existsByUser(User user);

  List<TeamMember> findAllByTeam(Team team);

  Optional<TeamMember> findByUser(User user);

  Optional<TeamMember> findByUserAndTeam(User user, Team team);
}
