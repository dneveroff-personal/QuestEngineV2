package dn.questenginev2.team.specification;

import dn.questenginev2.team.entity.Team;
import dn.questenginev2.user.entity.User;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class TeamSpecification {

  private TeamSpecification() {}

  public static Specification<Team> hasName(String name) {
    return (root, query, cb) -> {
      if (!StringUtils.hasText(name)) {
        return null;
      }
      return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    };
  }

  public static Specification<Team> hasCaptain(User captain) {
    return (root, query, cb) -> captain == null ? null : cb.equal(root.get("captain"), captain);
  }

  public static Specification<Team> createdAtAfter(Instant createdAt) {
    return (root, query, cb) ->
        createdAt == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), createdAt);
  }

  public static Specification<Team> createdAtBefore(Instant createdAt) {
    return (root, query, cb) ->
        createdAt == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), createdAt);
  }
}
