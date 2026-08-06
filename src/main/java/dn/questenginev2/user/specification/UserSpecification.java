package dn.questenginev2.user.specification;

import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class UserSpecification {

  private UserSpecification() {}

  public static Specification<User> hasUsername(String username) {
    return (root, query, cb) -> {
      if (!StringUtils.hasText(username)) {
        return null;
      }
      return cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
    };
  }

  public static Specification<User> hasEmail(String email) {
    return (root, query, cb) -> {
      if (!StringUtils.hasText(email)) {
        return null;
      }
      return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    };
  }

  public static Specification<User> hasRole(UserRole role) {
    return (root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role);
  }

  public static Specification<User> createdAtAfter(Instant createdAt) {
    return (root, query, cb) ->
        createdAt == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), createdAt);
  }

  public static Specification<User> createdAtBefore(Instant createdAt) {
    return (root, query, cb) ->
        createdAt == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), createdAt);
  }
}
