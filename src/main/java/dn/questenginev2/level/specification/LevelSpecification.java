package dn.questenginev2.level.specification;

import dn.questenginev2.level.entity.Level;
import dn.questenginev2.quest.entity.Quest;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class LevelSpecification {

  private LevelSpecification() {}

  public static Specification<Level> hasQuest(Quest quest) {
    return (root, query, cb) -> quest == null ? null : cb.equal(root.get("quest"), quest);
  }

  public static Specification<Level> hasQuestId(Long questId) {
    return (root, query, cb) ->
        questId == null ? null : cb.equal(root.get("quest").get("id"), questId);
  }

  public static Specification<Level> titleContains(String title) {
    return (root, query, cb) -> {
      if (!StringUtils.hasText(title)) {
        return null;
      }
      return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    };
  }

  public static Specification<Level> orderIndexEquals(Integer orderIndex) {
    return (root, query, cb) ->
        orderIndex == null ? null : cb.equal(root.get("orderIndex"), orderIndex);
  }

  public static Specification<Level> timeoutSecondsGreaterThanOrEqualTo(Integer timeoutSeconds) {
    return (root, query, cb) ->
        timeoutSeconds == null
            ? null
            : cb.greaterThanOrEqualTo(root.get("timeoutSeconds"), timeoutSeconds);
  }

  public static Specification<Level> timeoutSecondsLessThanOrEqualTo(Integer timeoutSeconds) {
    return (root, query, cb) ->
        timeoutSeconds == null
            ? null
            : cb.lessThanOrEqualTo(root.get("timeoutSeconds"), timeoutSeconds);
  }

  public static Specification<Level> createdAtAfter(Instant createdAt) {
    return (root, query, cb) ->
        createdAt == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), createdAt);
  }

  public static Specification<Level> createdAtBefore(Instant createdAt) {
    return (root, query, cb) ->
        createdAt == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), createdAt);
  }
}
