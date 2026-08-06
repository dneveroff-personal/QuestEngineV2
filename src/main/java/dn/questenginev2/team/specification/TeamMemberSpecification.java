package dn.questenginev2.team.specification;

import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.entity.TeamRole;
import dn.questenginev2.user.entity.User;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public class TeamMemberSpecification {

  private TeamMemberSpecification() {}

  public static Specification<TeamMember> hasTeam(Team team) {
    return (root, query, cb) -> team == null ? null : cb.equal(root.get("team"), team);
  }

  public static Specification<TeamMember> hasUser(User user) {
    return (root, query, cb) -> user == null ? null : cb.equal(root.get("user"), user);
  }

  public static Specification<TeamMember> hasRole(TeamRole role) {
    return (root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role);
  }

  public static Specification<TeamMember> joinedAtAfter(Instant joinedAt) {
    return (root, query, cb) ->
        joinedAt == null ? null : cb.greaterThanOrEqualTo(root.get("joinedAt"), joinedAt);
  }

  public static Specification<TeamMember> joinedAtBefore(Instant joinedAt) {
    return (root, query, cb) ->
        joinedAt == null ? null : cb.lessThanOrEqualTo(root.get("joinedAt"), joinedAt);
  }
}
