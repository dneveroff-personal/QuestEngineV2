package dn.questenginev2.hint.specification;

import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.level.entity.Level;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class HintSpecification {

  private HintSpecification() {}

  public static Specification<Hint> hasLevel(Level level) {
    return (root, query, cb) -> level == null ? null : cb.equal(root.get("level"), level);
  }

  public static Specification<Hint> hasLevelId(Long levelId) {
    return (root, query, cb) ->
        levelId == null ? null : cb.equal(root.get("level").get("id"), levelId);
  }

  public static Specification<Hint> orderIndexEquals(Integer orderIndex) {
    return (root, query, cb) ->
        orderIndex == null ? null : cb.equal(root.get("orderIndex"), orderIndex);
  }

  public static Specification<Hint> delaySecondsGreaterThanOrEqualTo(Integer delaySeconds) {
    return (root, query, cb) ->
        delaySeconds == null
            ? null
            : cb.greaterThanOrEqualTo(root.get("delaySeconds"), delaySeconds);
  }

  public static Specification<Hint> delaySecondsLessThanOrEqualTo(Integer delaySeconds) {
    return (root, query, cb) ->
        delaySeconds == null ? null : cb.lessThanOrEqualTo(root.get("delaySeconds"), delaySeconds);
  }

  public static Specification<Hint> contentContains(String content) {
    return (root, query, cb) -> {
      if (!StringUtils.hasText(content)) {
        return null;
      }
      return cb.like(cb.lower(root.get("content")), "%" + content.toLowerCase() + "%");
    };
  }

  public static Specification<Hint> createdAtAfter(Instant createdAt) {
    return (root, query, cb) ->
        createdAt == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), createdAt);
  }

  public static Specification<Hint> createdAtBefore(Instant createdAt) {
    return (root, query, cb) ->
        createdAt == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), createdAt);
  }
}
